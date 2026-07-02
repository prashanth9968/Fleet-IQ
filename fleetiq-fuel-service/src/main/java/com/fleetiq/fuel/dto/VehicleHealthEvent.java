package com.fleetiq.fuel.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleHealthEvent(
    UUID tenantId,
    UUID vehicleId,
    OffsetDateTime timestamp,
    Integer engineRpm,
    Double coolantTemp,
    Boolean ignition,
    Double speedKmh,
    String correlationId
) {}
