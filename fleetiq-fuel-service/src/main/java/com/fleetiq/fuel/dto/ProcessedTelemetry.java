package com.fleetiq.fuel.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProcessedTelemetry(
    UUID tenantId,
    UUID vehicleId,
    UUID deviceId,
    OffsetDateTime timestamp,
    Double latitude,
    Double longitude,
    Double speedKmh,
    Double fuelLevelLitres,
    Integer engineRpm,
    String correlationId
) {}
