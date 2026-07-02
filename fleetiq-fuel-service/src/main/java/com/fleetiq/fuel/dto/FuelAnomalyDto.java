package com.fleetiq.fuel.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FuelAnomalyDto(
    UUID tenantId,
    UUID vehicleId,
    String anomalyType,
    OffsetDateTime detectedAt,
    Double fuelDropLitres,
    Integer durationSeconds,
    Double speedKmh,
    Boolean ignitionState,
    Double confidenceScore,
    String message,
    String correlationId
) {}
