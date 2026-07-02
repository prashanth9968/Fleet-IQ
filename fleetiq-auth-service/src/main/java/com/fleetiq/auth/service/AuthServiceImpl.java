package com.fleetiq.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetiq.auth.config.JwtService;
import com.fleetiq.auth.dto.request.CreateApiKeyRequest;
import com.fleetiq.auth.dto.request.LoginRequest;
import com.fleetiq.auth.dto.request.RefreshTokenRequest;
import com.fleetiq.auth.dto.response.ApiKeyResponse;
import com.fleetiq.auth.dto.response.ApiKeyVerificationResponse;
import com.fleetiq.auth.dto.response.LoginResponse;
import com.fleetiq.auth.entity.ApiKey;
import com.fleetiq.auth.entity.Permission;
import com.fleetiq.auth.entity.Role;
import com.fleetiq.auth.entity.Tenant;
import com.fleetiq.auth.entity.User;
import com.fleetiq.auth.entity.UserSession;
import com.fleetiq.auth.exception.BadRequestException;
import com.fleetiq.auth.exception.ResourceNotFoundException;
import com.fleetiq.auth.exception.UnauthorizedException;
import com.fleetiq.auth.repository.ApiKeyRepository;
import com.fleetiq.auth.repository.TenantRepository;
import com.fleetiq.auth.repository.UserRepository;
import com.fleetiq.auth.repository.UserSessionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final UserSessionRepository userSessionRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthServiceImpl(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            UserSessionRepository userSessionRepository,
            ApiKeyRepository apiKeyRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.userSessionRepository = userSessionRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        Tenant tenant = null;
        if (request.tenantSlug() != null && !request.tenantSlug().trim().isEmpty()) {
            tenant = tenantRepository.findBySlug(request.tenantSlug())
                    .orElseThrow(() -> new UnauthorizedException("Invalid tenant or credentials"));
            
            if (!"ACTIVE".equals(tenant.getStatus())) {
                throw new UnauthorizedException("Tenant account is suspended");
            }
        }

        User user;
        if (tenant != null) {
            user = userRepository.findByTenantIdAndEmail(tenant.getId(), request.email())
                    .orElseThrow(() -> new UnauthorizedException("Invalid tenant or credentials"));
        } else {
            user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new UnauthorizedException("Invalid tenant or credentials"));
        }

        // Check account lock
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new UnauthorizedException("Account is locked until " + user.getLockedUntil());
        }

        // Verify password
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= 5) {
                user.setLockedUntil(OffsetDateTime.now().plusMinutes(15));
                userRepository.save(user);
                throw new UnauthorizedException("Account is locked for 15 minutes due to 5 failed login attempts");
            }
            userRepository.save(user);
            throw new UnauthorizedException("Invalid tenant or credentials");
        }

        // Check status
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new UnauthorizedException("User account is inactive or suspended");
        }

        // Reset failures
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        // Fetch roles and permissions
        List<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .distinct()
                .toList();

        String highestRole = user.getRoles().stream()
                .max(Comparator.comparingInt(Role::getHierarchyLevel))
                .map(Role::getName)
                .orElse("USER");

        UUID tenantId = tenant != null ? tenant.getId() : (user.getTenant() != null ? user.getTenant().getId() : null);

        String accessToken = jwtService.generateToken(user.getEmail(), tenantId, highestRole, permissions);
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        // Save session
        UserSession session = new UserSession();
        session.setUser(user);
        session.setRefreshTokenHash(hashToken(refreshToken));
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        session.setExpiresAt(OffsetDateTime.now().plusWeeks(1)); // 7 days refresh token validity
        userSessionRepository.save(session);

        return new LoginResponse(
                accessToken,
                refreshToken,
                OffsetDateTime.now().plusHours(1), // 1 hour access token validity
                tenantId,
                highestRole,
                permissions
        );
    }

    @Override
    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request, String ipAddress, String userAgent) {
        String refreshToken = request.refreshToken();
        String hashedToken = hashToken(refreshToken);

        UserSession session = userSessionRepository.findByRefreshTokenHash(hashedToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (session.getRevokedAt() != null) {
            throw new UnauthorizedException("Refresh token is revoked");
        }

        if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UnauthorizedException("Refresh token is expired");
        }

        User user = session.getUser();
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new UnauthorizedException("User account is inactive");
        }

        // Revoke current session
        session.setRevokedAt(OffsetDateTime.now());
        userSessionRepository.save(session);

        // Generate new set of tokens
        List<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .distinct()
                .toList();

        String highestRole = user.getRoles().stream()
                .max(Comparator.comparingInt(Role::getHierarchyLevel))
                .map(Role::getName)
                .orElse("USER");

        UUID tenantId = user.getTenant() != null ? user.getTenant().getId() : null;

        String newAccessToken = jwtService.generateToken(user.getEmail(), tenantId, highestRole, permissions);
        String newRefreshToken = jwtService.generateRefreshToken(user.getEmail());

        // Create new session (token rotation)
        UserSession newSession = new UserSession();
        newSession.setUser(user);
        newSession.setRefreshTokenHash(hashToken(newRefreshToken));
        newSession.setIpAddress(ipAddress);
        newSession.setUserAgent(userAgent);
        newSession.setExpiresAt(OffsetDateTime.now().plusWeeks(1));
        userSessionRepository.save(newSession);

        return new LoginResponse(
                newAccessToken,
                newRefreshToken,
                OffsetDateTime.now().plusHours(1),
                tenantId,
                highestRole,
                permissions
        );
    }

    @Override
    @Transactional
    public ApiKeyResponse createApiKey(CreateApiKeyRequest request, UUID creatorUserId) {
        User creator = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Creator user not found"));

        if (creator.getTenant() == null) {
            throw new BadRequestException("API keys can only be generated for users belonging to a tenant");
        }

        // Generate raw key: flq_ + 32-char secure random string
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        String randomPart = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String rawKey = "flq_" + randomPart;

        // Prefix: flq_ + first 8 characters of random string
        String prefix = rawKey.substring(0, 12);
        String hashedKey = hashToken(rawKey);

        ApiKey apiKey = new ApiKey();
        apiKey.setTenant(creator.getTenant());
        apiKey.setName(request.name());
        apiKey.setKeyHash(hashedKey);
        apiKey.setKeyPrefix(prefix);
        
        try {
            apiKey.setScopes(objectMapper.writeValueAsString(request.scopes()));
        } catch (Exception e) {
            apiKey.setScopes("[]");
        }

        if (request.rateLimitPerMinute() != null) {
            apiKey.setRateLimitPerMinute(request.rateLimitPerMinute());
        }
        
        apiKey.setExpiresAt(request.expiresAt());
        apiKey.setCreatedBy(creator);
        apiKey.setActive(true);

        apiKeyRepository.save(apiKey);

        List<String> scopesList = request.scopes();
        if (scopesList == null) {
            scopesList = List.of();
        }

        return new ApiKeyResponse(
                apiKey.getId(),
                apiKey.getName(),
                rawKey, // Raw key returned only here
                scopesList,
                apiKey.getRateLimitPerMinute(),
                apiKey.getExpiresAt(),
                apiKey.isActive()
        );
    }

    @Override
    @Transactional
    public ApiKeyVerificationResponse verifyApiKey(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.trim().isEmpty()) {
            return new ApiKeyVerificationResponse(false, null, null, null);
        }

        String hashedKey = hashToken(rawApiKey);
        ApiKey apiKey = apiKeyRepository.findByKeyHashAndIsActiveTrue(hashedKey).orElse(null);

        if (apiKey == null) {
            return new ApiKeyVerificationResponse(false, null, null, null);
        }

        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(OffsetDateTime.now())) {
            apiKey.setActive(false);
            apiKeyRepository.save(apiKey);
            return new ApiKeyVerificationResponse(false, null, null, null);
        }

        // Update last used time
        apiKey.setLastUsedAt(OffsetDateTime.now());
        apiKeyRepository.save(apiKey);

        List<String> scopesList;
        try {
            scopesList = objectMapper.readValue(apiKey.getScopes(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception e) {
            scopesList = List.of();
        }

        return new ApiKeyVerificationResponse(
                true,
                apiKey.getTenant().getId(),
                scopesList,
                apiKey.getRateLimitPerMinute()
        );
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 hashing algorithm not found", e);
        }
    }
}
