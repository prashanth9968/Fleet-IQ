package com.fleetiq.fuel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetiq.fuel.dto.FuelAlertDto;
import com.fleetiq.fuel.entity.FuelAnomalyHistory;
import com.fleetiq.fuel.repository.FuelAnomalyHistoryRepository;
import com.fleetiq.fuel.state.VehicleFuelState;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Excessive idle fuel burn detection. Identifies vehicles with engine running
 * (RPM &gt; 0) but effectively stationary (speed &lt; 1 km/h) that are consuming
 * fuel above the idle threshold. An alert is raised only after 10 consecutive
 * minutes of idle burn to avoid false positives from brief traffic stops.
 */
@Service
@Slf4j
public class FuelIdleDetectionService {

    private static final String ALERT_TOPIC = "fuel.alerts";
    private static final String ANOMALY_TYPE = "IDLE_BURN";
    private static final double DEFAULT_IDLE_THRESHOLD_LPM = 0.3;
    private static final double STATIONARY_SPEED_LIMIT = 1.0;
    private static final long IDLE_DURATION_THRESHOLD_MINUTES = 10;

    private final FuelAnomalyHistoryRepository fuelAnomalyHistoryRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public FuelIdleDetectionService(FuelAnomalyHistoryRepository fuelAnomalyHistoryRepository,
                                    StringRedisTemplate redisTemplate,
                                    ObjectMapper objectMapper) {
        this.fuelAnomalyHistoryRepository = fuelAnomalyHistoryRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void evaluate(UUID vehicleId, UUID tenantId, VehicleFuelState state,
                         Double fuelRate, OffsetDateTime timestamp) {
        if (fuelRate == null) {
            return;
        }

        try {
            MDC.put("tenantId", tenantId.toString());
            MDC.put("vehicleId", vehicleId.toString());

            boolean engineRunning = state.getLastEngineRpm() != null && state.getLastEngineRpm() > 0;
            boolean vehicleStationary = state.getLastSpeedKmh() != null && state.getLastSpeedKmh() < STATIONARY_SPEED_LIMIT;
            boolean highIdleConsumption = fuelRate > DEFAULT_IDLE_THRESHOLD_LPM;

            if (engineRunning && vehicleStationary && highIdleConsumption) {
                if (state.getIdleStart() == null) {
                    state.setIdleStart(timestamp);
                    log.debug("Idle burn tracking started for vehicle={}, fuelRate={} L/min, rpm={}",
                            vehicleId, fuelRate, state.getLastEngineRpm());
                    return;
                }

                Duration idleDuration = Duration.between(state.getIdleStart(), timestamp);

                if (idleDuration.toMinutes() >= IDLE_DURATION_THRESHOLD_MINUTES) {
                    log.info("IDLE_BURN alert triggered for vehicle={}, idleDuration={}min, " +
                                    "fuelRate={} L/min, rpm={}",
                            vehicleId, idleDuration.toMinutes(), fuelRate, state.getLastEngineRpm());

                    FuelAnomalyHistory anomaly = buildAnomaly(vehicleId, tenantId, state,
                            fuelRate, idleDuration, timestamp);
                    fuelAnomalyHistoryRepository.save(anomaly);

                    publishAlert(vehicleId, tenantId, fuelRate, idleDuration, timestamp);

                    // Reset idle start to avoid repeated alerts for the same idle session
                    state.setIdleStart(timestamp);
                }
            } else {
                if (state.getIdleStart() != null) {
                    log.debug("Idle burn resolved for vehicle={}", vehicleId);
                }
                state.setIdleStart(null);
            }
        } catch (Exception e) {
            log.error("Error evaluating idle fuel burn for vehicle={}: {}", vehicleId, e.getMessage(), e);
        } finally {
            MDC.remove("tenantId");
            MDC.remove("vehicleId");
        }
    }

    private FuelAnomalyHistory buildAnomaly(UUID vehicleId, UUID tenantId, VehicleFuelState state,
                                             double fuelRate, Duration duration,
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
        anomaly.setNotes(String.format(
                "Excessive idle fuel burn: %.3f L/min for %d minutes with engine at %d RPM",
                fuelRate, duration.toMinutes(),
                state.getLastEngineRpm() != null ? state.getLastEngineRpm() : 0));
        anomaly.setCreatedAt(OffsetDateTime.now());
        return anomaly;
    }

    private void publishAlert(UUID vehicleId, UUID tenantId, double fuelRate,
                               Duration duration, OffsetDateTime timestamp) {
        try {
            String correlationId = UUID.randomUUID().toString();
            FuelAlertDto alert = new FuelAlertDto(
                    tenantId,
                    vehicleId,
                    ANOMALY_TYPE,
                    "MEDIUM",
                    timestamp,
                    fuelRate,
                    DEFAULT_IDLE_THRESHOLD_LPM,
                    (int) duration.getSeconds(),
                    0.85,
                    String.format("Excessive idle fuel burn: %.3f L/min for %d minutes",
                            fuelRate, duration.toMinutes()),
                    correlationId
            );

            String json = objectMapper.writeValueAsString(alert);
            ProducerRecord<String, String> record = new ProducerRecord<>(ALERT_TOPIC, vehicleId.toString(), json);
            record.headers().add("tenant-id", tenantId.toString().getBytes(StandardCharsets.UTF_8));
            record.headers().add("alert-type", ANOMALY_TYPE.getBytes(StandardCharsets.UTF_8));
            record.headers().add("correlation-id", correlationId.getBytes(StandardCharsets.UTF_8));
            record.headers().add("producer-id", "fleetiq-fuel-service".getBytes(StandardCharsets.UTF_8));

            redisTemplate.convertAndSend(record);
            log.debug("Published IDLE_BURN alert to {} for vehicle={}", ALERT_TOPIC, vehicleId);
        } catch (Exception e) {
            log.error("Failed to publish IDLE_BURN alert for vehicle={}: {}", vehicleId, e.getMessage(), e);
        }
    }
}
