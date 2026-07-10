package com.fleetiq.driver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fleetiq.driver.dto.DriverScoreDto;
import com.fleetiq.driver.dto.DrivingEventDto;
import com.fleetiq.driver.entity.*;
import com.fleetiq.driver.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DriverSafetyServiceTest {

    private DrivingEventRepository drivingEventRepository;
    private DriverAssignmentRepository assignmentRepository;
    private DriverScoringRuleRepository scoringRuleRepository;
    private DriverSafetyScoreRepository safetyScoreRepository;
    private StringRedisTemplate redisTemplate;
    private ObjectMapper objectMapper;
    private DriverSafetyService service;

    private UUID tenantId = UUID.randomUUID();
    private UUID vehicleId = UUID.randomUUID();
    private UUID driverId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        drivingEventRepository = mock(DrivingEventRepository.class);
        assignmentRepository = mock(DriverAssignmentRepository.class);
        scoringRuleRepository = mock(DriverScoringRuleRepository.class);
        safetyScoreRepository = mock(DriverSafetyScoreRepository.class);
        redisTemplate = mock(StringRedisTemplate.class);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        service = new DriverSafetyService(
                drivingEventRepository,
                assignmentRepository,
                scoringRuleRepository,
                safetyScoreRepository,
                redisTemplate,
                objectMapper
        );
    }

    @Test
    void testDrivingEventAppliesConfigurableScoring() throws Exception {
        Instant eventAt = Instant.parse("2026-07-01T12:00:00Z");
        DrivingEventDto dto = new DrivingEventDto(
                vehicleId, tenantId, driverId, "SPEEDING", eventAt,
                12.34, 56.78, 95.0, 80.0, 0.0, 10, "High Road", UUID.randomUUID(), "{}"
        );

        String jsonPayload = objectMapper.writeValueAsString(dto);

        // Mock Scoring Rule (Tenant Specific: Speeding deducts 15 points instead of 5)
        DriverScoringRule rule = DriverScoringRule.builder()
                .tenantId(tenantId)
                .eventType("SPEEDING")
                .points(15)
                .enabled(true)
                .build();
        when(scoringRuleRepository.findByTenantIdAndEventType(tenantId, "SPEEDING"))
                .thenReturn(Optional.of(rule));

        // Mock Safety Score retrieval (No existing score)
        when(safetyScoreRepository.findByDriverIdAndPeriodTypeOrderByPeriodStartDesc(driverId, "MONTHLY"))
                .thenReturn(List.of());

        // Run
        service.onDrivingEvent(jsonPayload);

        // Verify Driving Event saved
        verify(drivingEventRepository, times(1)).save(any(DrivingEvent.class));

        // Verify Safety Score saved with deduction
        ArgumentCaptor<DriverSafetyScore> scoreCaptor = ArgumentCaptor.forClass(DriverSafetyScore.class);
        verify(safetyScoreRepository, times(1)).save(scoreCaptor.capture());
        
        DriverSafetyScore score = scoreCaptor.getValue();
        assertThat(score.getOverallScore()).isEqualByComparingTo(BigDecimal.valueOf(85.00)); // 100 - 15
        assertThat(score.getSpeedingScore()).isEqualByComparingTo(BigDecimal.valueOf(85.00));
        assertThat(score.getTotalEvents()).isEqualTo(1);

        // Verify published score update
        verify(redisTemplate, times(1)).convertAndSend(eq("driver.scores"), any(DriverScoreDto.class));
        verify(redisTemplate, times(1)).convertAndSend(eq("driver.events"), any(DrivingEventDto.class));
    }

    @Test
    void testDrivingEventAutoResolvesDriverId() throws Exception {
        Instant eventAt = Instant.parse("2026-07-01T12:00:00Z");
        // Driver ID is null in telemetry payload
        DrivingEventDto dto = new DrivingEventDto(
                vehicleId, tenantId, null, "HARSH_BRAKING", eventAt,
                12.34, 56.78, 50.0, 80.0, -1.2, 2, "Main St", UUID.randomUUID(), "{}"
        );

        String jsonPayload = objectMapper.writeValueAsString(dto);

        // Mock Active Shift Assignment
        DriverAssignment assignment = DriverAssignment.builder()
                .driverId(driverId)
                .vehicleId(vehicleId)
                .tenantId(tenantId)
                .status("ACTIVE")
                .build();
        when(assignmentRepository.findActiveAssignmentByVehicleId(vehicleId))
                .thenReturn(Optional.of(assignment));

        // Mock Default Scoring Rule (Harsh braking is default -3)
        when(scoringRuleRepository.findByTenantIdAndEventType(tenantId, "HARSH_BRAKING"))
                .thenReturn(Optional.empty());

        when(safetyScoreRepository.findByDriverIdAndPeriodTypeOrderByPeriodStartDesc(driverId, "MONTHLY"))
                .thenReturn(List.of());

        // Run
        service.onDrivingEvent(jsonPayload);

        // Verify Event saved with resolved driver ID
        ArgumentCaptor<DrivingEvent> eventCaptor = ArgumentCaptor.forClass(DrivingEvent.class);
        verify(drivingEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getDriverId()).isEqualTo(driverId);

        // Verify Safety Score saved with default penalty
        ArgumentCaptor<DriverSafetyScore> scoreCaptor = ArgumentCaptor.forClass(DriverSafetyScore.class);
        verify(safetyScoreRepository).save(scoreCaptor.capture());
        DriverSafetyScore score = scoreCaptor.getValue();
        assertThat(score.getOverallScore()).isEqualByComparingTo(BigDecimal.valueOf(97.00)); // 100 - 3 (default harsh braking)
        assertThat(score.getHarshBrakeScore()).isEqualByComparingTo(BigDecimal.valueOf(97.00));
    }
}
