package com.fleetiq.fuel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetiq.fuel.dto.FuelAnomalyDto;
import com.fleetiq.fuel.entity.FuelAnomalyHistory;
import com.fleetiq.fuel.entity.FuelThreshold;
import com.fleetiq.fuel.repository.FuelAnomalyHistoryRepository;
import com.fleetiq.fuel.repository.FuelThresholdRepository;
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
 * Ignition-aware fuel theft detection. Identifies rapid fuel level drops while
 * the vehicle is stationary (speed &lt; 2 km/h) and ignition is off, within a
 * sliding window of the last 5 minutes. Sensor fault awareness prevents false
 * positives from malfunctioning sensors.
 */
@Service
@Slf4j
public class FuelTheftDetectionService {

    private static final String ANOMALY_TOPIC = "fuel.anomalies";
    private static final String ANOMALY_TYPE = "THEFT";
    private static final double DEFAULT_THEFT_THRESHOLD_LITRES = 5.0;
    private static final double STATIONARY_SPEED_LIMIT = 2.0;
    private static final long THEFT_WINDOW_MINUTES = 5;

    private final FuelThresholdRepository fuelThresholdRepository;
    private final FuelAnomalyHistoryRepository fuelAnomalyHistoryRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public FuelTheftDetectionService(FuelThresholdRepository fuelThresholdRepository,
                                     FuelAnomalyHistoryRepository fuelAnomalyHistoryRepository,
                                     KafkaTemplate<String, String> kafkaTemplate,
                                     ObjectMapper objectMapper) {
        this.fuelThresholdRepository = fuelThresholdRepository;
        this.fuelAnomalyHistoryRepository = fuelAnomalyHistoryRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void evaluate(UUID vehicleId, UUID tenantId, VehicleFuelState state) {
        try {
            MDC.put("tenantId", tenantId.toString());
            MDC.put("vehicleId", vehicleId.toString());

            List<FuelSnapshot> readings = new ArrayList<>(state.getRecentReadings());
            if (readings.size() < 2) {
                return;
            }

            // Determine the 5-minute window boundary from the latest reading
            OffsetDateTime now = readings.getLast().timestamp();
            OffsetDateTime windowStart = now.minusMinutes(THEFT_WINDOW_MINUTES);

            // Filter readings within the window
            List<FuelSnapshot> windowReadings = readings.stream()
                    .filter(r -> !r.timestamp().isBefore(windowStart))
                    .toList();

            if (windowReadings.size() < 2) {
                return;
            }

            FuelSnapshot first = windowReadings.getFirst();
            FuelSnapshot last = windowReadings.getLast();
            double fuelDrop = first.fuelLevel() - last.fuelLevel();
            Duration windowDuration = Duration.between(first.timestamp(), last.timestamp());

            // Get theft threshold
            double threshold = resolveTheftThreshold(vehicleId);

            // Check theft conditions: significant drop + vehicle stationary + ignition off + within window
            boolean significantDrop = fuelDrop > threshold;
            boolean vehicleStationary = state.getLastSpeedKmh() != null && state.getLastSpeedKmh() < STATIONARY_SPEED_LIMIT;
            boolean ignitionOff = state.getLastIgnition() != null && !state.getLastIgnition();
            boolean withinWindow = windowDuration.toMinutes() <= THEFT_WINDOW_MINUTES;

            if (significantDrop && vehicleStationary && ignitionOff && withinWindow) {
                // Suppress if sensor fault is active
                if (state.isSensorFaultActive()) {
                    log.warn("Potential fuel theft suppressed for vehicle={} due to active sensor fault. " +
                            "fuelDrop={} L, threshold={} L", vehicleId, fuelDrop, threshold);
                    return;
                }

                log.info("THEFT alert triggered for vehicle={}, fuelDrop={} L, threshold={} L, " +
                                "speed={} km/h, ignition={}, windowDuration={}s",
                        vehicleId, fuelDrop, threshold,
                        state.getLastSpeedKmh(), state.getLastIgnition(), windowDuration.getSeconds());

                FuelAnomalyHistory anomaly = buildAnomaly(vehicleId, tenantId, state,
                        fuelDrop, windowDuration, last.timestamp());
                fuelAnomalyHistoryRepository.save(anomaly);

                publishAnomaly(vehicleId, tenantId, state, fuelDrop, windowDuration, last.timestamp());
            }
        } catch (Exception e) {
            log.error("Error evaluating fuel theft for vehicle={}: {}", vehicleId, e.getMessage(), e);
        } finally {
            MDC.remove("tenantId");
            MDC.remove("vehicleId");
        }
    }

    private double resolveTheftThreshold(UUID vehicleId) {
        return fuelThresholdRepository.findByVehicleIdAndAlertType(vehicleId, ANOMALY_TYPE)
                .map(t -> t.getThresholdValue() != null
                        ? t.getThresholdValue().doubleValue()
                        : DEFAULT_THEFT_THRESHOLD_LITRES)
                .orElse(DEFAULT_THEFT_THRESHOLD_LITRES);
    }

    private FuelAnomalyHistory buildAnomaly(UUID vehicleId, UUID tenantId, VehicleFuelState state,
                                             double fuelDrop, Duration duration,
                                             OffsetDateTime timestamp) {
        FuelAnomalyHistory anomaly = new FuelAnomalyHistory();
        anomaly.setVehicleId(vehicleId);
        anomaly.setTenantId(tenantId);
        anomaly.setAnomalyType(ANOMALY_TYPE);
        anomaly.setDetectedAt(timestamp);
        anomaly.setConfidenceScore(BigDecimal.valueOf(0.90));
        anomaly.setStatus("OPEN");
        anomaly.setFuelDropLitres(BigDecimal.valueOf(fuelDrop));
        anomaly.setDurationSeconds((int) duration.getSeconds());
        anomaly.setSpeedKmhAtEvent(state.getLastSpeedKmh() != null
                ? BigDecimal.valueOf(state.getLastSpeedKmh()) : null);
        anomaly.setIgnitionState(state.getLastIgnition());
        anomaly.setNotes(String.format(
                "Suspected fuel theft: %.2f L drop in %d seconds while vehicle stationary with ignition off",
                fuelDrop, duration.getSeconds()));
        anomaly.setCreatedAt(OffsetDateTime.now());
        return anomaly;
    }

    private void publishAnomaly(UUID vehicleId, UUID tenantId, VehicleFuelState state,
                                 double fuelDrop, Duration duration, OffsetDateTime timestamp) {
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
                    0.90,
                    String.format("Suspected fuel theft: %.2f L drop while stationary", fuelDrop),
                    correlationId
            );

            String json = objectMapper.writeValueAsString(anomalyDto);
            ProducerRecord<String, String> record = new ProducerRecord<>(ANOMALY_TOPIC, vehicleId.toString(), json);
            record.headers().add("tenant-id", tenantId.toString().getBytes(StandardCharsets.UTF_8));
            record.headers().add("anomaly-type", ANOMALY_TYPE.getBytes(StandardCharsets.UTF_8));
            record.headers().add("correlation-id", correlationId.getBytes(StandardCharsets.UTF_8));
            record.headers().add("producer-id", "fleetiq-fuel-service".getBytes(StandardCharsets.UTF_8));

            kafkaTemplate.send(record);
            log.debug("Published THEFT anomaly to {} for vehicle={}", ANOMALY_TOPIC, vehicleId);
        } catch (Exception e) {
            log.error("Failed to publish THEFT anomaly for vehicle={}: {}", vehicleId, e.getMessage(), e);
        }
    }
}
