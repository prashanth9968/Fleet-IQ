package com.fleetiq.fuel.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RefuelEventDto(
    UUID tenantId,
    UUID vehicleId,
    OffsetDateTime refueledAt,
    Double fuelBeforeLitres,
    Double fuelAfterLitres,
    Double fuelAddedLitres,
    Double locationLat,
    Double locationLng,
    Double odometerKm,
    String correlationId
) {}
