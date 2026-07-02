package com.fleetiq.tracking.dto;

import java.util.UUID;

public record AssignmentCacheEntry(
    UUID tenantId,
    UUID vehicleId,
    UUID deviceId
) {}
