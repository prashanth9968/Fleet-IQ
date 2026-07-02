package com.fleetiq.driver.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DriverScoreDto(
        UUID tenantId,
        UUID driverId,
        String periodType,
        Instant periodStart,
        Instant periodEnd,
        BigDecimal overallScore,
        Integer totalEvents,
        Instant updatedAt
) {}
