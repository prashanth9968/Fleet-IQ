package com.fleetiq.health.dto;

import java.time.Instant;
import java.util.UUID;

public record MaintenanceEventDto(
        UUID tenantId,
        UUID vehicleId,
        String eventType, // SERVICE_DUE, WORK_ORDER_CREATED, WORK_ORDER_COMPLETED
        UUID workOrderId,
        String description,
        Instant occurredAt
) {}
