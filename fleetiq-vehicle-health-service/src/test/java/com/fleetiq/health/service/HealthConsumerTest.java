package com.fleetiq.health.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetiq.health.dto.VehicleHealthEventDto;
import com.fleetiq.health.entity.*;
import com.fleetiq.health.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HealthConsumerTest {

    @Mock private DtcLibraryRepository dtcLibraryRepository;
    @Mock private VehicleDtcEventRepository dtcEventRepository;
    @Mock private VehicleHealthScoreRepository healthScoreRepository;
    @Mock private VehicleHealthHistoryRepository healthHistoryRepository;
    @Mock private VehicleHealthRuleRepository healthRuleRepository;
    @Mock private MaintenanceScheduleRepository maintenanceScheduleRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private HealthConsumer healthConsumer;

    private UUID tenantId;
    private UUID vehicleId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();
    }

    @Test
    void testEvaluateHealth_WithOverheatAndLowBattery() throws Exception {
        // Arrange default rules
        VehicleHealthRule coolantRule = VehicleHealthRule.builder()
                .ruleType("COOLANT_TEMP")
                .thresholdValue(BigDecimal.valueOf(105.00))
                .deduction(BigDecimal.valueOf(15.00))
                .enabled(true)
                .build();
        VehicleHealthRule batteryRule = VehicleHealthRule.builder()
                .ruleType("BATTERY_VOLTAGE")
                .thresholdValue(BigDecimal.valueOf(11.80))
                .deduction(BigDecimal.valueOf(10.00))
                .enabled(true)
                .build();

        when(healthRuleRepository.findByTenantIdAndRuleType(tenantId, "COOLANT_TEMP"))
                .thenReturn(Optional.of(coolantRule));
        when(healthRuleRepository.findByTenantIdAndRuleType(tenantId, "BATTERY_VOLTAGE"))
                .thenReturn(Optional.of(batteryRule));

        // Mock empty repository responses
        when(dtcEventRepository.findByVehicleIdAndIsActiveTrue(vehicleId))
                .thenReturn(Collections.emptyList());
        when(healthScoreRepository.findByVehicleId(vehicleId))
                .thenReturn(Optional.empty());
        when(healthHistoryRepository.findByVehicleIdAndRecordedAtBetweenOrderByRecordedAtAsc(eq(vehicleId), any(), any()))
                .thenReturn(Collections.emptyList());

        // Event containing 108C Coolant Temp (overheat limit 105C) and 11.2V Battery (low battery limit 11.8V)
        VehicleHealthEventDto event = new VehicleHealthEventDto(
                vehicleId, tenantId, UUID.randomUUID(), Instant.now(),
                2000.0, 108.0, 300.0, 11.2, 25.0, 30.0, 40.0, 0.0, 0.0, 10.0, 300.0, 25.0,
                15000.0, 400.0, 10.0, false, true, Collections.emptyList(), "{}"
        );

        when(objectMapper.readValue(any(String.class), eq(VehicleHealthEventDto.class))).thenReturn(event);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        healthConsumer.onHealthEvent("{}");

        // Assert - Verify updated scores are saved (cooling drop 15%, battery drop 10%)
        ArgumentCaptor<VehicleHealthScore> scoreCaptor = ArgumentCaptor.forClass(VehicleHealthScore.class);
        verify(healthScoreRepository).save(scoreCaptor.capture());
        VehicleHealthScore score = scoreCaptor.getValue();

        assertEquals(0, BigDecimal.valueOf(85).compareTo(score.getCoolingScore()));
        assertEquals(0, BigDecimal.valueOf(90).compareTo(score.getBatteryScore()));
        assertEquals(0, BigDecimal.valueOf(100).compareTo(score.getEngineScore())); // No load/oil issue
        
        // Overall: (85 + 90 + 100 + 100 + 100 + 100 + 100 + 100) / 8 = 96.88%
        assertEquals(0, BigDecimal.valueOf(96.88).compareTo(score.getOverallScore()));
    }
}
