package com.fleetiq.alerts.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record UnifiedAlertEvent(
        UUID tenantId,
        UUID vehicleId,
        String sourceService,
        String alertType,
        String priority,
        String message,
        OffsetDateTime detectedAt,
        Map<String, Object> metadata,
        String correlationId
) {
}
