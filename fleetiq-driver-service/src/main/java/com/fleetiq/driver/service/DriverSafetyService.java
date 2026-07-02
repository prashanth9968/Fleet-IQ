package com.fleetiq.driver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetiq.driver.dto.DriverScoreDto;
import com.fleetiq.driver.dto.DrivingEventDto;
import com.fleetiq.driver.entity.*;
import com.fleetiq.driver.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverSafetyService {

    private final DrivingEventRepository drivingEventRepository;
    private final DriverAssignmentRepository assignmentRepository;
    private final DriverScoringRuleRepository scoringRuleRepository;
    private final DriverSafetyScoreRepository safetyScoreRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "driving.events", groupId = "fleetiq-driver-group")
    @Transactional
    public void onDrivingEvent(String payload) {
        try {
            DrivingEventDto dto = objectMapper.readValue(payload, DrivingEventDto.class);
            UUID driverId = dto.driverId();

            if (driverId == null) {
                // Look up active assignment
                Optional<DriverAssignment> assignment = assignmentRepository.findActiveAssignmentByVehicleId(dto.vehicleId());
                if (assignment.isPresent()) {
                    driverId = assignment.get().getDriverId();
                } else {
                    log.warn("Driving event received but no driver is assigned to vehicle ID: {}", dto.vehicleId());
                    return;
                }
            }

            // 1. Save Driving Event
            DrivingEvent event = DrivingEvent.builder()
                    .vehicleId(dto.vehicleId())
                    .eventAt(dto.eventAt() != null ? dto.eventAt() : Instant.now())
                    .tenantId(dto.tenantId())
                    .driverId(driverId)
                    .eventType(dto.eventType())
                    .latitude(dto.latitude())
                    .longitude(dto.longitude())
                    .speedKmh(dto.speedKmh())
                    .speedLimitKmh(dto.speedLimitKmh())
                    .magnitude(dto.magnitude())
                    .durationSeconds(dto.durationSeconds())
                    .roadName(dto.roadName())
                    .tripId(dto.tripId())
                    .metadata(dto.metadata())
                    .build();

            drivingEventRepository.save(event);

            // 2. Query Configurable scoring rules
            DriverScoringRule rule = getScoringRule(dto.tenantId(), dto.eventType());
            if (rule != null && !rule.getEnabled()) {
                log.info("Scoring rule for event type {} is disabled for tenant {}", dto.eventType(), dto.tenantId());
                return;
            }

            int penaltyPoints = rule != null ? rule.getPoints() : getDefaultPenaltyPoints(dto.eventType());

            // 3. Update Monthly Safety Score
            updateMonthlySafetyScore(dto.tenantId(), driverId, dto.eventAt() != null ? dto.eventAt() : Instant.now(), penaltyPoints, dto.eventType());

            // 4. Publish to driver.events topic
            kafkaTemplate.send("driver.events", dto);

        } catch (Exception e) {
            log.error("Error processing driving event: {}", e.getMessage(), e);
        }
    }

    @Cacheable(value = "scoringRules", key = "#tenantId.toString() + ':' + #eventType")
    public DriverScoringRule getScoringRule(UUID tenantId, String eventType) {
        return scoringRuleRepository.findByTenantIdAndEventType(tenantId, eventType).orElse(null);
    }

    private int getDefaultPenaltyPoints(String eventType) {
        return switch (eventType) {
            case "HARSH_ACCELERATION", "HARSH_CORNERING" -> 2;
            case "HARSH_BRAKING" -> 3;
            case "SPEEDING" -> 5;
            case "FATIGUE" -> 10;
            case "PHONE_USE" -> 15;
            case "SEATBELT_VIOLATION" -> 5;
            default -> 2;
        };
    }

    private void updateMonthlySafetyScore(UUID tenantId, UUID driverId, Instant eventAt, int penaltyPoints, String eventType) {
        LocalDate localDate = LocalDate.ofInstant(eventAt, ZoneOffset.UTC);
        Instant periodStart = localDate.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant periodEnd = localDate.with(TemporalAdjusters.lastDayOfMonth()).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        // Try to find monthly safety score
        var scores = safetyScoreRepository.findByDriverIdAndPeriodTypeOrderByPeriodStartDesc(driverId, "MONTHLY");
        DriverSafetyScore monthlyScore = scores.stream()
                .filter(s -> s.getPeriodStart().equals(periodStart))
                .findFirst()
                .orElseGet(() -> DriverSafetyScore.builder()
                        .id(UUID.randomUUID())
                        .tenantId(tenantId)
                        .driverId(driverId)
                        .periodType("MONTHLY")
                        .periodStart(periodStart)
                        .periodEnd(periodEnd)
                        .overallScore(BigDecimal.valueOf(100.00))
                        .harshAccelScore(BigDecimal.valueOf(100.00))
                        .harshBrakeScore(BigDecimal.valueOf(100.00))
                        .harshCornerScore(BigDecimal.valueOf(100.00))
                        .speedingScore(BigDecimal.valueOf(100.00))
                        .fatigueScore(BigDecimal.valueOf(100.00))
                        .seatbeltScore(BigDecimal.valueOf(100.00))
                        .totalTrips(0)
                        .totalDistanceKm(BigDecimal.ZERO)
                        .totalDrivingHours(BigDecimal.ZERO)
                        .totalEvents(0)
                        .build());

        // Update overall and sub-category scores
        BigDecimal newOverall = monthlyScore.getOverallScore().subtract(BigDecimal.valueOf(penaltyPoints));
        if (newOverall.compareTo(BigDecimal.ZERO) < 0) {
            newOverall = BigDecimal.ZERO;
        }
        monthlyScore.setOverallScore(newOverall);
        monthlyScore.setTotalEvents(monthlyScore.getTotalEvents() + 1);

        switch (eventType) {
            case "HARSH_ACCELERATION" -> monthlyScore.setHarshAccelScore(deduct(monthlyScore.getHarshAccelScore(), penaltyPoints));
            case "HARSH_BRAKING" -> monthlyScore.setHarshBrakeScore(deduct(monthlyScore.getHarshBrakeScore(), penaltyPoints));
            case "HARSH_CORNERING" -> monthlyScore.setHarshCornerScore(deduct(monthlyScore.getHarshCornerScore(), penaltyPoints));
            case "SPEEDING" -> monthlyScore.setSpeedingScore(deduct(monthlyScore.getSpeedingScore(), penaltyPoints));
            case "FATIGUE" -> monthlyScore.setFatigueScore(deduct(monthlyScore.getFatigueScore(), penaltyPoints));
            case "SEATBELT_VIOLATION" -> monthlyScore.setSeatbeltScore(deduct(monthlyScore.getSeatbeltScore(), penaltyPoints));
        }

        safetyScoreRepository.save(monthlyScore);

        // Publish updated score to driver.scores topic
        DriverScoreDto scoreDto = new DriverScoreDto(
                tenantId,
                driverId,
                "MONTHLY",
                periodStart,
                periodEnd,
                monthlyScore.getOverallScore(),
                monthlyScore.getTotalEvents(),
                Instant.now()
        );
        kafkaTemplate.send("driver.scores", scoreDto);
    }

    private BigDecimal deduct(BigDecimal score, int penalty) {
        BigDecimal res = score.subtract(BigDecimal.valueOf(penalty));
        return res.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : res;
    }
}
