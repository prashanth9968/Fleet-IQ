package com.fleetiq.driver.dto;

import java.time.Instant;
import java.util.UUID;

public record DrivingEventDto(
        UUID vehicleId,
        UUID tenantId,
        UUID driverId,
        String eventType,
        Instant eventAt,
        Double latitude,
        Double longitude,
        Double speedKmh,
        Double speedLimitKmh,
        Double magnitude,
        Integer durationSeconds,
        String roadName,
        UUID tripId,
        String metadata
) {}
