package com.fleetiq.driver.dto;

import java.util.UUID;

public record AssignDriverRequest(
        UUID driverId,
        UUID vehicleId,
        UUID assignedBy,
        String notes
) {}
