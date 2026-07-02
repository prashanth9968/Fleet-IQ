package com.fleetiq.gateway.dto;

import java.time.OffsetDateTime;

public record TelemetryPayload(
    String apiKey,
    String deviceId,
    OffsetDateTime timestamp,
    Double latitude,
    Double longitude,
    Double speed,
    Double fuelLevel,
    Integer engineRpm
) {}
