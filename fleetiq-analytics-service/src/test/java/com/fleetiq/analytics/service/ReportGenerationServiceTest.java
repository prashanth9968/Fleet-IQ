package com.fleetiq.analytics.service;

import com.fleetiq.analytics.entity.DailyFleetReport;
import com.fleetiq.analytics.entity.FleetReport;
import com.fleetiq.analytics.repository.DailyFleetReportRepository;
import com.fleetiq.analytics.repository.FleetReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportGenerationServiceTest {

    @Mock private FleetReportRepository fleetReportRepository;
    @Mock private DailyFleetReportRepository dailyFleetReportRepository;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private ReportGenerationService reportGenerationService;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
    }

    @Test
    void testGenerateReport_PDF_Success() {
        // Arrange
        Instant end = Instant.now();
        Instant start = end.minus(7, ChronoUnit.DAYS);

        DailyFleetReport mockSummary = DailyFleetReport.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .date(LocalDate.now())
                .distanceKm(BigDecimal.valueOf(1000.0))
                .fuelConsumedLitres(BigDecimal.valueOf(250.0))
                .avgFuelEfficiency(BigDecimal.valueOf(4.0))
                .safetyScore(BigDecimal.valueOf(95.0))
                .utilizationPct(BigDecimal.valueOf(90.0))
                .avgTripDurationMins(BigDecimal.valueOf(120))
                .avgIdleTimeMins(BigDecimal.valueOf(10.0))
                .fuelCost(BigDecimal.valueOf(300.0))
                .maintenanceCost(BigDecimal.valueOf(100.0))
                .criticalAlertsCount(1)
                .co2EstimateKg(BigDecimal.valueOf(600.0))
                .build();

        when(dailyFleetReportRepository.findByTenantIdAndDateBetweenOrderByDateAsc(eq(tenantId), any(), any()))
                .thenReturn(Collections.singletonList(mockSummary));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        FleetReport report = reportGenerationService.generateReport(tenantId, "CUSTOM", start, end, "PDF");

        // Assert
        assertNotNull(report);
        assertEquals("CUSTOM", report.getType());
        assertEquals("PDF", report.getFormat());
        assertEquals("COMPLETED", report.getStatus());

        ArgumentCaptor<FleetReport> captor = ArgumentCaptor.forClass(FleetReport.class);
        verify(fleetReportRepository, times(2)).save(captor.capture()); // Once for GENERATING, once for COMPLETED
        FleetReport savedReport = captor.getValue();
        assertEquals("COMPLETED", savedReport.getStatus());
        assertNotNull(savedReport.getFileUrl());

        // Verify Redis cache gets updated
        verify(redisTemplate).delete(anyList());
        verify(valueOperations, times(5)).set(anyString(), any(), eq(24L), eq(java.util.concurrent.TimeUnit.HOURS));
    }
}
