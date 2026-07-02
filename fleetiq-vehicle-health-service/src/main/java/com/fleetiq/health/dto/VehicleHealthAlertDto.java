package com.fleetiq.health.dto;

import java.time.Instant;
import java.util.UUID;

public record VehicleHealthAlertDto(
        UUID tenantId,
        UUID vehicleId,
        String alertType, // ENGINE_OVERHEAT, LOW_BATTERY, LOW_OIL_PRESSURE, HIGH_ENGINE_LOAD, SERVICE_DUE, CRITICAL_DTC
        String severity, // CRITICAL, HIGH, MEDIUM, LOW
        String message,
        Instant detectedAt,
        Double value,
        Double threshold
) {}
