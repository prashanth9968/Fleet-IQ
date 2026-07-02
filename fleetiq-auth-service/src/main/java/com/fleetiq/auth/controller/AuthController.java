package com.fleetiq.auth.controller;

import com.fleetiq.auth.dto.request.CreateApiKeyRequest;
import com.fleetiq.auth.dto.request.LoginRequest;
import com.fleetiq.auth.dto.request.RefreshTokenRequest;
import com.fleetiq.auth.dto.response.ApiKeyResponse;
import com.fleetiq.auth.dto.response.ApiKeyVerificationResponse;
import com.fleetiq.auth.dto.response.LoginResponse;
import com.fleetiq.auth.entity.User;
import com.fleetiq.auth.exception.ResourceNotFoundException;
import com.fleetiq.auth.exception.UnauthorizedException;
import com.fleetiq.auth.repository.UserRepository;
import com.fleetiq.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication & Sessions", description = "Endpoints for user login, token refresh, and tenant API Key management")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and issue JWT and Refresh Tokens")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request
    ) {
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        LoginResponse response = authService.login(loginRequest, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue new access JWT")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest refreshRequest,
            HttpServletRequest request
    ) {
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        LoginResponse response = authService.refresh(refreshRequest, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api-keys")
    @PreAuthorize("hasAuthority('write:api-keys')")
    @Operation(summary = "Create an API Key for device/system access (Tenant Admins only)")
    public ResponseEntity<ApiKeyResponse> createApiKey(
            @Valid @RequestBody CreateApiKeyRequest apiKeyRequest
    ) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current authenticated user not found"));

        ApiKeyResponse response = authService.createApiKey(apiKeyRequest, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify-key")
    @Operation(summary = "Validate an API key and return tenant metadata (Used by Device Gateway)")
    public ResponseEntity<ApiKeyVerificationResponse> verifyKey(
            @RequestParam(value = "key", required = false) String queryKey,
            @RequestHeader(value = "X-API-Key", required = false) String headerKey
    ) {
        String apiKeyToVerify = queryKey != null ? queryKey : headerKey;
        if (apiKeyToVerify == null || apiKeyToVerify.trim().isEmpty()) {
            throw new UnauthorizedException("API Key is missing from query param or X-API-Key header");
        }

        ApiKeyVerificationResponse response = authService.verifyApiKey(apiKeyToVerify);
        if (!response.valid()) {
            return ResponseEntity.status(401).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
