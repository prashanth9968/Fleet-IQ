package com.fleetiq.fuel.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FuelAlertDto(
    UUID tenantId,
    UUID vehicleId,
    String alertType,
    String severity,
    OffsetDateTime detectedAt,
    Double fuelRate,
    Double threshold,
    Integer durationSeconds,
    Double confidence,
    String message,
    String correlationId
) {}
