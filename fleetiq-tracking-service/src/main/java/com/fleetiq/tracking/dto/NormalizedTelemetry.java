package com.fleetiq.tracking.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NormalizedTelemetry(
    UUID tenantId,
    String deviceId,
    OffsetDateTime timestamp,
    Double latitude,
    Double longitude,
    Double speedKmh,
    Double fuelLevelLitres,
    Integer engineRpm,
    String correlationId
) {}
