package com.fleetiq.tracking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProcessedTelemetry(
    @JsonProperty("tenantId")        UUID tenantId,
    @JsonProperty("vehicleId")       UUID vehicleId,
    @JsonProperty("deviceId")        UUID deviceId,
    @JsonProperty("timestamp")       OffsetDateTime timestamp,
    @JsonProperty("latitude")        Double latitude,
    @JsonProperty("longitude")       Double longitude,
    @JsonProperty("speedKmh")        Double speedKmh,
    @JsonProperty("fuelLevelLitres") Double fuelLevelLitres,
    @JsonProperty("engineRpm")       Integer engineRpm,
    @JsonProperty("correlationId")   String correlationId
) {}
