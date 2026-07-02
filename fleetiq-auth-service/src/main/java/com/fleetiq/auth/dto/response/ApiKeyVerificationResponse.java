package com.fleetiq.auth.dto.response;

import java.util.List;
import java.util.UUID;

public record ApiKeyVerificationResponse(
    boolean valid,
    UUID tenantId,
    List<String> scopes,
    Integer rateLimitPerMinute
) {}
