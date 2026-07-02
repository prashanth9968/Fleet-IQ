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
 * Slow fuel leak detection (premium feature). Detects gradual fuel loss over a
 * 10-minute window while the vehicle is operating normally (moving, ignition on).
 * Distinguished from theft patterns by requiring non-zero speed and active ignition.
 * Sensor fault awareness prevents false positives from malfunctioning sensors.
 */
@Service
@Slf4j
public class FuelLeakDetectionService {

    private static final String ANOMALY_TOPIC = "fuel.anomalies";
    private static final String ANOMALY_TYPE = "LEAK";
    private static final double LEAK_RATE_THRESHOLD_LPM = 0.05; // L/min
    private static final double NORMAL_DRIVING_SPEED_THRESHOLD = 10.0; // km/h
    private static final long LEAK_WINDOW_MINUTES = 10;

    private final FuelAnomalyHistoryRepository fuelAnomalyHistoryRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public FuelLeakDetectionService(FuelAnomalyHistoryRepository fuelAnomalyHistoryRepository,
                                    KafkaTemplate<String, String> kafkaTemplate,
                                    ObjectMapper objectMapper) {
        this.fuelAnomalyHistoryRepository = fuelAnomalyHistoryRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void evaluate(UUID vehicleId, UUID tenantId, VehicleFuelState state) {
        try {
            MDC.put("tenantId", tenantId.toString());
            MDC.put("vehicleId", vehicleId.toString());

            // Suppress if sensor fault is active
            if (state.isSensorFaultActive()) {
                log.debug("Leak detection suppressed for vehicle={} due to active sensor fault", vehicleId);
                return;
            }

            List<FuelSnapshot> readings = new ArrayList<>(state.getRecentReadings());
            if (readings.size() < 5) {
                return;
            }

            // Determine the 10-minute window boundary
            OffsetDateTime now = readings.getLast().timestamp();
            OffsetDateTime windowStart = now.minusMinutes(LEAK_WINDOW_MINUTES);

            // Filter readings within the window
            List<FuelSnapshot> windowReadings = readings.stream()
                    .filter(r -> !r.timestamp().isBefore(windowStart))
                    .toList();

            if (windowReadings.size() < 5) {
                return;
            }

            FuelSnapshot first = windowReadings.getFirst();
            FuelSnapshot last = windowReadings.getLast();
            double fuelDrop = first.fuelLevel() - last.fuelLevel();
            Duration windowDuration = Duration.between(first.timestamp(), last.timestamp());

            if (windowDuration.toMinutes() < LEAK_WINDOW_MINUTES) {
                return; // Not enough data duration
            }

            double durationMinutes = windowDuration.getSeconds() / 60.0;
            double dropRateLpm = fuelDrop / durationMinutes;

            // Check leak conditions:
            // 1. Drop rate exceeds threshold
            // 2. Vehicle is moving normally (not stationary — that's a theft pattern)
            // 3. Not a theft pattern (speed > 2 or ignition on)
            // 4. Duration is at least 10 minutes
            boolean significantDropRate = dropRateLpm > LEAK_RATE_THRESHOLD_LPM;
            boolean vehicleMoving = state.getLastSpeedKmh() != null
                    && state.getLastSpeedKmh() > NORMAL_DRIVING_SPEED_THRESHOLD;
            boolean notTheftPattern = (state.getLastSpeedKmh() != null && state.getLastSpeedKmh() > 2.0)
                    || (state.getLastIgnition() != null && state.getLastIgnition());
            boolean sufficientDuration = windowDuration.toMinutes() >= LEAK_WINDOW_MINUTES;

            if (significantDropRate && vehicleMoving && notTheftPattern && sufficientDuration) {
                log.info("LEAK alert triggered for vehicle={}, dropRate={} L/min, threshold={} L/min, " +
                                "speed={} km/h, windowDuration={}min",
                        vehicleId, String.format("%.4f", dropRateLpm), LEAK_RATE_THRESHOLD_LPM,
                        state.getLastSpeedKmh(), windowDuration.toMinutes());

                FuelAnomalyHistory anomaly = buildAnomaly(vehicleId, tenantId, state,
                        fuelDrop, windowDuration, last.timestamp());
                fuelAnomalyHistoryRepository.save(anomaly);

                publishAnomaly(vehicleId, tenantId, state, fuelDrop, dropRateLpm, windowDuration, last.timestamp());
            }
        } catch (Exception e) {
            log.error("Error evaluating fuel leak for vehicle={}: {}", vehicleId, e.getMessage(), e);
        } finally {
            MDC.remove("tenantId");
            MDC.remove("vehicleId");
        }
    }

    private FuelAnomalyHistory buildAnomaly(UUID vehicleId, UUID tenantId, VehicleFuelState state,
                                             double fuelDrop, Duration duration,
                                             OffsetDateTime timestamp) {
        FuelAnomalyHistory anomaly = new FuelAnomalyHistory();
        anomaly.setVehicleId(vehicleId);
        anomaly.setTenantId(tenantId);
        anomaly.setAnomalyType(ANOMALY_TYPE);
        anomaly.setDetectedAt(timestamp);
        anomaly.setConfidenceScore(BigDecimal.valueOf(0.60));
        anomaly.setStatus("OPEN");
        anomaly.setFuelDropLitres(BigDecimal.valueOf(fuelDrop));
        anomaly.setDurationSeconds((int) duration.getSeconds());
        anomaly.setSpeedKmhAtEvent(state.getLastSpeedKmh() != null
                ? BigDecimal.valueOf(state.getLastSpeedKmh()) : null);
        anomaly.setIgnitionState(state.getLastIgnition());
        anomaly.setNotes(String.format(
                "Suspected fuel leak: %.2f L drop over %d minutes while vehicle operating normally",
                fuelDrop, duration.toMinutes()));
        anomaly.setCreatedAt(OffsetDateTime.now());
        return anomaly;
    }

    private void publishAnomaly(UUID vehicleId, UUID tenantId, VehicleFuelState state,
                                 double fuelDrop, double dropRate, Duration duration,
                                 OffsetDateTime timestamp) {
        try {
            String correlationId = UUID.randomUUID().toString();
            FuelAnomalyDto anomalyDto = new FuelAnomalyDto(
                    tenantId,
                    vehicleId,
                    ANOMALY_TYPE,
                    timestamp,
                    fuelDrop,
                    (int) duration.getSeconds(),
                    state.getLastSpeedKmh(),
                    state.getLastIgnition(),
                    0.60,
                    String.format("Suspected fuel leak: %.4f L/min drop rate over %d minutes",
                            dropRate, duration.toMinutes()),
                    correlationId
            );

            String json = objectMapper.writeValueAsString(anomalyDto);
            ProducerRecord<String, String> record = new ProducerRecord<>(ANOMALY_TOPIC, vehicleId.toString(), json);
            record.headers().add("tenant-id", tenantId.toString().getBytes(StandardCharsets.UTF_8));
            record.headers().add("anomaly-type", ANOMALY_TYPE.getBytes(StandardCharsets.UTF_8));
            record.headers().add("correlation-id", correlationId.getBytes(StandardCharsets.UTF_8));
            record.headers().add("producer-id", "fleetiq-fuel-service".getBytes(StandardCharsets.UTF_8));

            kafkaTemplate.send(record);
            log.debug("Published LEAK anomaly to {} for vehicle={}", ANOMALY_TOPIC, vehicleId);
        } catch (Exception e) {
            log.error("Failed to publish LEAK anomaly for vehicle={}: {}", vehicleId, e.getMessage(), e);
        }
    }
}
