package com.fleetiq.fuel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetiq.fuel.dto.FuelAnomalyDto;
import com.fleetiq.fuel.entity.FuelAnomalyHistory;
import com.fleetiq.fuel.repository.FuelAnomalyHistoryRepository;
import com.fleetiq.fuel.state.VehicleFuelState;
import com.fleetiq.fuel.state.VehicleFuelState.FuelSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fuel sensor fault detection. Detects two failure modes:
 * <ul>
 *   <li><b>Stuck sensor</b>: fuel level unchanged (within 0.01 tolerance) for
 *       over 2 hours while vehicle was moving (&gt; 5 km/h).</li>
 *   <li><b>Erratic sensor</b>: fuel level spread (max - min) exceeds 50% of
 *       baseline tank capacity (default 50 L) within a 60-second window.</li>
 * </ul>
 * When a sensor fault is detected, {@code state.sensorFaultActive} is set to
 * {@code true} to suppress downstream theft and leak alerts.
 */
@Service
@Slf4j
public class FuelSensorFaultService {

    private static final String ANOMALY_TOPIC = "fuel.anomalies";
    private static final String ANOMALY_TYPE = "SENSOR_FAULT";
    private static final double STUCK_TOLERANCE = 0.01;
    private static final long STUCK_DURATION_HOURS = 2;
    private static final double STUCK_SPEED_THRESHOLD = 5.0;
    private static final double DEFAULT_TANK_CAPACITY = 100.0; // litres
    private static final double ERRATIC_SPREAD_FACTOR = 0.50; // 50% of tank capacity
    private static final long ERRATIC_WINDOW_SECONDS = 60;

    private final FuelAnomalyHistoryRepository fuelAnomalyHistoryRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public FuelSensorFaultService(FuelAnomalyHistoryRepository fuelAnomalyHistoryRepository,
                                  KafkaTemplate<String, String> kafkaTemplate,
                                  ObjectMapper objectMapper) {
        this.fuelAnomalyHistoryRepository = fuelAnomalyHistoryRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void evaluate(UUID vehicleId, UUID tenantId, VehicleFuelState state,
                         Double currentFuelLevel, OffsetDateTime timestamp) {
        if (currentFuelLevel == null) {
            return;
        }

        try {
            MDC.put("tenantId", tenantId.toString());
            MDC.put("vehicleId", vehicleId.toString());

            evaluateStuckSensor(vehicleId, tenantId, state, currentFuelLevel, timestamp);
            evaluateErraticSensor(vehicleId, tenantId, state, timestamp);
        } catch (Exception e) {
            log.error("Error evaluating sensor fault for vehicle={}: {}", vehicleId, e.getMessage(), e);
        } finally {
            MDC.remove("tenantId");
            MDC.remove("vehicleId");
        }
    }

    private void evaluateStuckSensor(UUID vehicleId, UUID tenantId, VehicleFuelState state,
                                      double currentFuelLevel, OffsetDateTime timestamp) {
        if (state.getStuckSensorValue() != null
                && Math.abs(currentFuelLevel - state.getStuckSensorValue()) <= STUCK_TOLERANCE) {
            // Value hasn't changed — continue tracking
            if (state.getStuckSensorSince() == null) {
                state.setStuckSensorSince(timestamp);
            }

            Duration stuckDuration = Duration.between(state.getStuckSensorSince(), timestamp);
            boolean vehicleWasMoving = state.getLastSpeedKmh() != null
                    && state.getLastSpeedKmh() > STUCK_SPEED_THRESHOLD;

            if (stuckDuration.toHours() >= STUCK_DURATION_HOURS && vehicleWasMoving) {
                log.info("SENSOR_FAULT (stuck) detected for vehicle={}, stuckValue={}, duration={}h, speed={} km/h",
                        vehicleId, currentFuelLevel, stuckDuration.toHours(), state.getLastSpeedKmh());

                state.setSensorFaultActive(true);

                FuelAnomalyHistory anomaly = buildAnomaly(vehicleId, tenantId, state,
                        "Stuck fuel sensor: value " + currentFuelLevel + " L unchanged for "
                                + stuckDuration.toHours() + " hours while vehicle was moving",
                        stuckDuration, timestamp);
                fuelAnomalyHistoryRepository.save(anomaly);

                publishAnomaly(vehicleId, tenantId, state, "stuck",
                        String.format("Stuck fuel sensor: value %.2f L unchanged for %d hours",
                                currentFuelLevel, stuckDuration.toHours()),
                        stuckDuration, timestamp);

                // Reset tracking to avoid repeated alerts
                state.setStuckSensorSince(timestamp);
            }
        } else {
            // Value changed — reset stuck tracking
            state.setStuckSensorValue(currentFuelLevel);
            state.setStuckSensorSince(null);
        }
    }

    private void evaluateErraticSensor(UUID vehicleId, UUID tenantId, VehicleFuelState state,
                                        OffsetDateTime timestamp) {
        List<FuelSnapshot> readings = new ArrayList<>(state.getRecentReadings());
        if (readings.size() < 10) {
            return;
        }

        // Get the last 10 readings
        List<FuelSnapshot> recent = new ArrayList<>(
                readings.subList(Math.max(0, readings.size() - 10), readings.size()));

        // Narrow to readings within 60 seconds of the latest reading
        FuelSnapshot last = recent.getLast();
        OffsetDateTime windowStart = last.timestamp().minusSeconds(ERRATIC_WINDOW_SECONDS);
        recent = recent.stream()
                .filter(r -> !r.timestamp().isBefore(windowStart))
                .toList();

        if (recent.size() < 3) {
            return;
        }

        double min = recent.stream().mapToDouble(FuelSnapshot::fuelLevel).min().orElse(0);
        double max = recent.stream().mapToDouble(FuelSnapshot::fuelLevel).max().orElse(0);
        double spread = max - min;

        double erraticThreshold = DEFAULT_TANK_CAPACITY * ERRATIC_SPREAD_FACTOR;

        if (spread > erraticThreshold) {
            log.info("SENSOR_FAULT (erratic) detected for vehicle={}, spread={} L, threshold={} L, " +
                            "windowSize={} readings",
                    vehicleId, String.format("%.2f", spread), erraticThreshold, recent.size());

            state.setSensorFaultActive(true);

            Duration faultDuration = Duration.between(recent.getFirst().timestamp(),
                    recent.getLast().timestamp());

            FuelAnomalyHistory anomaly = buildAnomaly(vehicleId, tenantId, state,
                    String.format("Erratic fuel sensor: %.2f L spread within %d seconds",
                            spread, faultDuration.getSeconds()),
                    faultDuration, timestamp);
            fuelAnomalyHistoryRepository.save(anomaly);

            publishAnomaly(vehicleId, tenantId, state, "erratic",
                    String.format("Erratic fuel sensor: %.2f L spread within %d seconds",
                            spread, faultDuration.getSeconds()),
                    faultDuration, timestamp);
        }
    }

    private FuelAnomalyHistory buildAnomaly(UUID vehicleId, UUID tenantId, VehicleFuelState state,
                                             String description, Duration duration,
                                             OffsetDateTime timestamp) {
        FuelAnomalyHistory anomaly = new FuelAnomalyHistory();
        anomaly.setVehicleId(vehicleId);
        anomaly.setTenantId(tenantId);
        anomaly.setAnomalyType(ANOMALY_TYPE);
        anomaly.setDetectedAt(timestamp);
        anomaly.setConfidenceScore(BigDecimal.valueOf(0.85));
        anomaly.setStatus("OPEN");
        anomaly.setDurationSeconds((int) duration.getSeconds());
        anomaly.setSpeedKmhAtEvent(state.getLastSpeedKmh() != null
                ? BigDecimal.valueOf(state.getLastSpeedKmh()) : null);
        anomaly.setIgnitionState(state.getLastIgnition());
        anomaly.setNotes(description);
        anomaly.setCreatedAt(OffsetDateTime.now());
        return anomaly;
    }

    private void publishAnomaly(UUID vehicleId, UUID tenantId, VehicleFuelState state,
                                 String subType, String message, Duration duration,
                                 OffsetDateTime timestamp) {
        try {
            String correlationId = UUID.randomUUID().toString();
            FuelAnomalyDto anomalyDto = new FuelAnomalyDto(
                    tenantId,
                    vehicleId,
                    ANOMALY_TYPE,
                    timestamp,
                    0.0, // fuelDropLitres not applicable for sensor faults
                    (int) duration.getSeconds(),
                    state.getLastSpeedKmh(),
                    state.getLastIgnition(),
                    0.85,
                    message,
                    correlationId
            );

            String json = objectMapper.writeValueAsString(anomalyDto);
            ProducerRecord<String, String> record = new ProducerRecord<>(ANOMALY_TOPIC, vehicleId.toString(), json);
            record.headers().add("tenant-id", tenantId.toString().getBytes(StandardCharsets.UTF_8));
            record.headers().add("anomaly-type", ANOMALY_TYPE.getBytes(StandardCharsets.UTF_8));
            record.headers().add("sensor-fault-type", subType.getBytes(StandardCharsets.UTF_8));
            record.headers().add("correlation-id", correlationId.getBytes(StandardCharsets.UTF_8));
            record.headers().add("producer-id", "fleetiq-fuel-service".getBytes(StandardCharsets.UTF_8));

            kafkaTemplate.send(record);
            log.debug("Published SENSOR_FAULT ({}) anomaly to {} for vehicle={}",
                    subType, ANOMALY_TOPIC, vehicleId);
        } catch (Exception e) {
            log.error("Failed to publish SENSOR_FAULT anomaly for vehicle={}: {}", vehicleId, e.getMessage(), e);
        }
    }
}
