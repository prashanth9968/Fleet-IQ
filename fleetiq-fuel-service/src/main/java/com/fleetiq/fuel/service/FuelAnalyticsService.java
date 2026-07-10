package com.fleetiq.fuel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetiq.fuel.entity.FuelBaseline;
import com.fleetiq.fuel.entity.FuelReading;
import com.fleetiq.fuel.repository.FuelBaselineRepository;
import com.fleetiq.fuel.repository.FuelReadingRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Computes fuel efficiency metrics by comparing consecutive fuel readings with
 * odometer deltas. Produces L/100km and km/L figures, calculates deviation from
 * baseline performance, and publishes analytics summaries to the
 * {@code fuel.analytics} Kafka topic for downstream aggregation.
 */
@Service
@Slf4j
public class FuelAnalyticsService {

    private static final String ANALYTICS_TOPIC = "fuel.analytics";

    private final FuelReadingRepository fuelReadingRepository;
    private final FuelBaselineRepository fuelBaselineRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public FuelAnalyticsService(FuelReadingRepository fuelReadingRepository,
                                FuelBaselineRepository fuelBaselineRepository,
                                StringRedisTemplate redisTemplate,
                                ObjectMapper objectMapper) {
        this.fuelReadingRepository = fuelReadingRepository;
        this.fuelBaselineRepository = fuelBaselineRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void recordReading(UUID vehicleId, UUID tenantId, Double fuelLevel,
                               Double odometerKm, Double speedKmh, OffsetDateTime timestamp) {
        if (fuelLevel == null || odometerKm == null) {
            return;
        }

        try {
            MDC.put("tenantId", tenantId.toString());
            MDC.put("vehicleId", vehicleId.toString());

            // Get previous reading
            Optional<FuelReading> previousOpt = fuelReadingRepository
                    .findTopByVehicleIdOrderByRecordedAtDesc(vehicleId);

            if (previousOpt.isEmpty()) {
                log.debug("No previous fuel reading for vehicle={}; skipping analytics", vehicleId);
                return;
            }

            FuelReading previous = previousOpt.get();
            BigDecimal prevOdometer = previous.getOdometerKm();
            BigDecimal prevFuelLevel = previous.getFuelLevelLitres();

            if (prevOdometer == null || prevFuelLevel == null) {
                return;
            }

            double distanceKm = odometerKm - prevOdometer.doubleValue();
            double fuelConsumed = prevFuelLevel.doubleValue() - fuelLevel;

            // Only calculate if distance and consumption are positive (no refuel in between)
            if (distanceKm <= 0 || fuelConsumed <= 0) {
                return;
            }

            // Calculate efficiency metrics
            double efficiencyLPer100km = (fuelConsumed / distanceKm) * 100.0;
            double efficiencyKmPerL = distanceKm / fuelConsumed;

            // Get baseline and calculate deviation
            Double baselineKmPerL = null;
            Double deviationPct = null;

            Optional<FuelBaseline> baselineOpt = fuelBaselineRepository
                    .findByVehicleTypeIdAndTenantId(vehicleId, tenantId);
            if (baselineOpt.isPresent()) {
                FuelBaseline baseline = baselineOpt.get();
                if (baseline.getExpectedEfficiencyKmPerLitre() != null) {
                    baselineKmPerL = baseline.getExpectedEfficiencyKmPerLitre().doubleValue();
                    deviationPct = ((efficiencyKmPerL - baselineKmPerL) / baselineKmPerL) * 100.0;
                }
            }

            log.debug("Fuel analytics for vehicle={}: distance={} km, consumed={} L, " +
                            "efficiency={} L/100km ({} km/L), baseline={} km/L, deviation={}%",
                    vehicleId,
                    String.format("%.2f", distanceKm),
                    String.format("%.2f", fuelConsumed),
                    String.format("%.2f", efficiencyLPer100km),
                    String.format("%.2f", efficiencyKmPerL),
                    baselineKmPerL != null ? String.format("%.2f", baselineKmPerL) : "N/A",
                    deviationPct != null ? String.format("%.1f", deviationPct) : "N/A");

            publishAnalytics(vehicleId, tenantId, distanceKm, fuelConsumed,
                    efficiencyLPer100km, efficiencyKmPerL, baselineKmPerL, deviationPct,
                    speedKmh, timestamp);

        } catch (Exception e) {
            log.error("Error computing fuel analytics for vehicle={}: {}", vehicleId, e.getMessage(), e);
        } finally {
            MDC.remove("tenantId");
            MDC.remove("vehicleId");
        }
    }

    private void publishAnalytics(UUID vehicleId, UUID tenantId,
                                   double distanceKm, double fuelConsumed,
                                   double efficiencyLPer100km, double efficiencyKmPerL,
                                   Double baselineKmPerL, Double deviationPct,
                                   Double speedKmh, OffsetDateTime timestamp) {
        try {
            Map<String, Object> summary = new HashMap<>();
            summary.put("eventId", UUID.randomUUID().toString());
            summary.put("tenantId", tenantId.toString());
            summary.put("vehicleId", vehicleId.toString());
            summary.put("distanceKm", BigDecimal.valueOf(distanceKm).setScale(2, RoundingMode.HALF_UP));
            summary.put("fuelConsumedLitres", BigDecimal.valueOf(fuelConsumed).setScale(2, RoundingMode.HALF_UP));
            summary.put("efficiencyLPer100km", BigDecimal.valueOf(efficiencyLPer100km).setScale(2, RoundingMode.HALF_UP));
            summary.put("efficiencyKmPerL", BigDecimal.valueOf(efficiencyKmPerL).setScale(2, RoundingMode.HALF_UP));
            if (baselineKmPerL != null) {
                summary.put("baselineKmPerL", BigDecimal.valueOf(baselineKmPerL).setScale(2, RoundingMode.HALF_UP));
            }
            if (deviationPct != null) {
                summary.put("deviationPct", BigDecimal.valueOf(deviationPct).setScale(1, RoundingMode.HALF_UP));
            }
            if (speedKmh != null) {
                summary.put("speedKmh", BigDecimal.valueOf(speedKmh).setScale(2, RoundingMode.HALF_UP));
            }
            summary.put("timestamp", timestamp.toString());

            String json = objectMapper.writeValueAsString(summary);
            ProducerRecord<String, String> record = new ProducerRecord<>(ANALYTICS_TOPIC, vehicleId.toString(), json);
            record.headers().add("tenant-id", tenantId.toString().getBytes(StandardCharsets.UTF_8));
            record.headers().add("producer-id", "fleetiq-fuel-service".getBytes(StandardCharsets.UTF_8));

            redisTemplate.convertAndSend(record);
            log.debug("Published fuel analytics to {} for vehicle={}", ANALYTICS_TOPIC, vehicleId);
        } catch (Exception e) {
            log.error("Failed to publish fuel analytics for vehicle={}: {}", vehicleId, e.getMessage(), e);
        }
    }
}
