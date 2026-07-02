package com.fleetiq.fuel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetiq.fuel.dto.FuelAlertDto;
import com.fleetiq.fuel.entity.FuelAnomalyHistory;
import com.fleetiq.fuel.entity.FuelThreshold;
import com.fleetiq.fuel.repository.FuelAnomalyHistoryRepository;
import com.fleetiq.fuel.repository.FuelThresholdRepository;
import com.fleetiq.fuel.state.VehicleFuelState;
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
import java.util.UUID;

/**
 * Detects sustained high fuel consumption by comparing the real-time fuel burn
 * rate against per-vehicle (or per-vehicle-type, or global default) thresholds.
 * A HIGH_CONSUMPTION anomaly is only raised once the consumption has persisted
 * for at least 30 consecutive seconds to avoid false positives from brief
 * acceleration spikes.
 */
@Service
@Slf4j
public class FuelConsumptionService {

    private static final String ALERT_TOPIC = "fuel.alerts";
    private static final String ALERT_TYPE = "HIGH_CONSUMPTION";
    private static final double DEFAULT_THRESHOLD_LPM = 1.0;
    private static final long SUSTAINED_SECONDS = 30;

    private final FuelThresholdRepository fuelThresholdRepository;
    private final FuelAnomalyHistoryRepository fuelAnomalyHistoryRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public FuelConsumptionService(FuelThresholdRepository fuelThresholdRepository,
                                  FuelAnomalyHistoryRepository fuelAnomalyHistoryRepository,
                                  KafkaTemplate<String, String> kafkaTemplate,
                                  ObjectMapper objectMapper) {
        this.fuelThresholdRepository = fuelThresholdRepository;
        this.fuelAnomalyHistoryRepository = fuelAnomalyHistoryRepository;
        this.kafkaTemplate = kafkaTemplate;
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

            double threshold = resolveThreshold(vehicleId);

            if (fuelRate > threshold) {
                if (state.getHighConsumptionStart() == null) {
                    state.setHighConsumptionStart(timestamp);
                    state.setConsecutiveHighReadings(1);
                    log.debug("High consumption tracking started for vehicle={}, fuelRate={} L/min, threshold={} L/min",
                            vehicleId, fuelRate, threshold);
                    return;
                }

                state.setConsecutiveHighReadings(state.getConsecutiveHighReadings() + 1);
                Duration duration = Duration.between(state.getHighConsumptionStart(), timestamp);

                if (duration.getSeconds() > SUSTAINED_SECONDS) {
                    log.info("HIGH_CONSUMPTION alert triggered for vehicle={}, duration={}s, fuelRate={} L/min",
                            vehicleId, duration.getSeconds(), fuelRate);

                    FuelAnomalyHistory anomaly = buildAnomaly(vehicleId, tenantId, state, fuelRate, duration, timestamp);
                    fuelAnomalyHistoryRepository.save(anomaly);

                    publishAlert(vehicleId, tenantId, fuelRate, threshold, duration, timestamp);

                    // Reset after alert to avoid duplicate alerts for the same streak
                    state.setHighConsumptionStart(timestamp);
                    state.setConsecutiveHighReadings(0);
                }
            } else {
                if (state.getHighConsumptionStart() != null) {
                    log.debug("High consumption resolved for vehicle={}, fuelRate={} L/min dropped below threshold={} L/min",
                            vehicleId, fuelRate, threshold);
                }
                state.setHighConsumptionStart(null);
                state.setConsecutiveHighReadings(0);
            }
        } catch (Exception e) {
            log.error("Error evaluating fuel consumption for vehicle={}: {}", vehicleId, e.getMessage(), e);
        } finally {
            MDC.remove("tenantId");
            MDC.remove("vehicleId");
        }
    }

    private double resolveThreshold(UUID vehicleId) {
        // Try vehicle-specific threshold with alertType='HIGH_CONSUMPTION'
        return fuelThresholdRepository.findByVehicleIdAndAlertType(vehicleId, ALERT_TYPE)
                .map(t -> t.getThresholdValue() != null
                        ? t.getThresholdValue().doubleValue()
                        : DEFAULT_THRESHOLD_LPM)
                .orElse(DEFAULT_THRESHOLD_LPM);
    }

    private FuelAnomalyHistory buildAnomaly(UUID vehicleId, UUID tenantId, VehicleFuelState state,
                                             double fuelRate, Duration duration,
                                             OffsetDateTime timestamp) {
        FuelAnomalyHistory anomaly = new FuelAnomalyHistory();
        anomaly.setVehicleId(vehicleId);
        anomaly.setTenantId(tenantId);
        anomaly.setAnomalyType(ALERT_TYPE);
        anomaly.setDetectedAt(timestamp);
        anomaly.setConfidenceScore(BigDecimal.valueOf(0.90));
        anomaly.setStatus("OPEN");
        anomaly.setDurationSeconds((int) duration.getSeconds());
        anomaly.setSpeedKmhAtEvent(state.getLastSpeedKmh() != null
                ? BigDecimal.valueOf(state.getLastSpeedKmh()) : null);
        anomaly.setIgnitionState(state.getLastIgnition());
        anomaly.setNotes(String.format(
                "Sustained high fuel consumption of %.3f L/min for %d seconds",
                fuelRate, duration.getSeconds()));
        anomaly.setCreatedAt(OffsetDateTime.now());
        return anomaly;
    }

    private void publishAlert(UUID vehicleId, UUID tenantId, double fuelRate,
                               double threshold, Duration duration, OffsetDateTime timestamp) {
        try {
            String correlationId = UUID.randomUUID().toString();
            FuelAlertDto alert = new FuelAlertDto(
                    tenantId,
                    vehicleId,
                    ALERT_TYPE,
                    "HIGH",
                    timestamp,
                    fuelRate,
                    threshold,
                    (int) duration.getSeconds(),
                    0.90,
                    String.format("Sustained high fuel consumption of %.3f L/min for %d seconds",
                            fuelRate, duration.getSeconds()),
                    correlationId
            );

            String json = objectMapper.writeValueAsString(alert);
            ProducerRecord<String, String> record = new ProducerRecord<>(ALERT_TOPIC, vehicleId.toString(), json);
            record.headers().add("tenant-id", tenantId.toString().getBytes(StandardCharsets.UTF_8));
            record.headers().add("alert-type", ALERT_TYPE.getBytes(StandardCharsets.UTF_8));
            record.headers().add("correlation-id", correlationId.getBytes(StandardCharsets.UTF_8));
            record.headers().add("producer-id", "fleetiq-fuel-service".getBytes(StandardCharsets.UTF_8));

            kafkaTemplate.send(record);
            log.debug("Published HIGH_CONSUMPTION alert to {} for vehicle={}", ALERT_TOPIC, vehicleId);
        } catch (Exception e) {
            log.error("Failed to publish HIGH_CONSUMPTION alert for vehicle={}: {}", vehicleId, e.getMessage(), e);
        }
    }
}
