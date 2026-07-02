package com.fleetiq.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.OffsetDateTime;
import java.util.List;

public record CreateApiKeyRequest(
    @NotBlank(message = "API Key name is required")
    String name,

    List<String> scopes,

    @Positive(message = "Rate limit must be positive")
    Integer rateLimitPerMinute,

    OffsetDateTime expiresAt
) {}
