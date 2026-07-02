package com.fleetiq.auth.service;

import com.fleetiq.auth.dto.request.CreateApiKeyRequest;
import com.fleetiq.auth.dto.request.LoginRequest;
import com.fleetiq.auth.dto.request.RefreshTokenRequest;
import com.fleetiq.auth.dto.response.ApiKeyResponse;
import com.fleetiq.auth.dto.response.ApiKeyVerificationResponse;
import com.fleetiq.auth.dto.response.LoginResponse;

import java.util.UUID;

public interface AuthService {
    LoginResponse login(LoginRequest request, String ipAddress, String userAgent);
    LoginResponse refresh(RefreshTokenRequest request, String ipAddress, String userAgent);
    ApiKeyResponse createApiKey(CreateApiKeyRequest request, UUID creatorUserId);
    ApiKeyVerificationResponse verifyApiKey(String rawApiKey);
}
