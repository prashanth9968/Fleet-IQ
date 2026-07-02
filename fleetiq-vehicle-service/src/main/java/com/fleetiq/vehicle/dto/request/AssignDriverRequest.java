package com.fleetiq.vehicle.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AssignDriverRequest(
    @NotNull(message = "Driver ID is required")
    UUID driverId,

    @NotNull(message = "Shift start time is required")
    OffsetDateTime shiftStart,

    OffsetDateTime shiftEnd,

    String notes
) {}
