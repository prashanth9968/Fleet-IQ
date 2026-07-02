package com.fleetiq.fuel.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FuelEvent(
    UUID tenantId,
    UUID vehicleId,
    UUID deviceId,
    OffsetDateTime timestamp,
    Double fuelLevelLitres,
    Double fuelRateLitresPerMin,
    Double odometerKm,
    Double speedKmh,
    Integer engineRpm,
    Boolean ignition,
    String correlationId
) {}
