package com.fleetiq.auth.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    OffsetDateTime expiresAt,
    UUID tenantId,
    String role,
    List<String> permissions
) {}
