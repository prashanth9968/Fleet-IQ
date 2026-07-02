package com.fleetiq.vehicle.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignDeviceRequest(
    @NotNull(message = "Device ID is required")
    UUID deviceId,

    Boolean isPrimary
) {}
