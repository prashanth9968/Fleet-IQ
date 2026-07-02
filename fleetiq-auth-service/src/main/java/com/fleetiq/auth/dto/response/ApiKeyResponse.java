package com.fleetiq.auth.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ApiKeyResponse(
    UUID id,
    String name,
    String key, // The raw key value (only populated on creation)
    List<String> scopes,
    Integer rateLimitPerMinute,
    OffsetDateTime expiresAt,
    boolean isActive
) {}
