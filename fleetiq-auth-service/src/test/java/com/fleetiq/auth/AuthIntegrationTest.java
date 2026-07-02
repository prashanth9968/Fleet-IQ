package com.fleetiq.auth;

import com.fleetiq.auth.dto.request.CreateApiKeyRequest;
import com.fleetiq.auth.dto.request.LoginRequest;
import com.fleetiq.auth.dto.request.RefreshTokenRequest;
import com.fleetiq.auth.dto.response.ApiKeyResponse;
import com.fleetiq.auth.dto.response.ApiKeyVerificationResponse;
import com.fleetiq.auth.dto.response.LoginResponse;
import com.fleetiq.auth.entity.Role;
import com.fleetiq.auth.entity.SubscriptionPlan;
import com.fleetiq.auth.entity.Tenant;
import com.fleetiq.auth.entity.User;
import com.fleetiq.auth.exception.ProblemDetails;
import com.fleetiq.auth.repository.RoleRepository;
import com.fleetiq.auth.repository.SubscriptionPlanRepository;
import com.fleetiq.auth.repository.TenantRepository;
import com.fleetiq.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Tenant testTenant;
    private User testUser;

    @BeforeEach
    public void setUp() {
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        // 1. Fetch or create Subscription Plan
        SubscriptionPlan plan = subscriptionPlanRepository.findByCode("ENTERPRISE")
                .orElseGet(() -> {
                    SubscriptionPlan p = new SubscriptionPlan();
                    p.setName("Enterprise");
                    p.setCode("ENTERPRISE");
                    p.setTier("ENTERPRISE");
                    p.setPricePerVehicleMonthly(349.00);
                    p.setMaxVehicles(null);
                    p.setMaxUsers(null);
                    p.setActive(true);
                    return subscriptionPlanRepository.save(p);
                });

        // 2. Fetch or create TENANT_ADMIN Role
        Role adminRole = roleRepository.findByName("TENANT_ADMIN")
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setName("TENANT_ADMIN");
                    r.setDisplayName("Tenant Admin");
                    r.setDescription("Tenant administrator role");
                    r.setSystemRole(true);
                    r.setHierarchyLevel(1);
                    return roleRepository.save(r);
                });

        // 3. Create Tenant
        testTenant = new Tenant();
        testTenant.setName("Test Tenant Corp");
        testTenant.setSlug("test-tenant");
        testTenant.setSubscriptionPlan(plan);
        testTenant.setStatus("ACTIVE");
        testTenant = tenantRepository.save(testTenant);

        // 4. Create User
        testUser = new User();
        testUser.setTenant(testTenant);
        testUser.setEmail("admin@testtenant.com");
        testUser.setPasswordHash(passwordEncoder.encode("SecurePassword123"));
        testUser.setFirstName("Jane");
        testUser.setLastName("Admin");
        testUser.setStatus("ACTIVE");
        testUser.setEmailVerified(true);
        testUser.setRoles(Set.of(adminRole));
        testUser = userRepository.save(testUser);
    }

    @Test
    public void testLoginSuccess() {
        LoginRequest request = new LoginRequest("admin@testtenant.com", "SecurePassword123", "test-tenant");

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login",
                request,
                LoginResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isNotEmpty();
        assertThat(response.getBody().refreshToken()).isNotEmpty();
        assertThat(response.getBody().role()).isEqualTo("TENANT_ADMIN");
        assertThat(response.getBody().tenantId()).isEqualTo(testTenant.getId());
    }

    @Test
    public void testLoginInvalidCredentials() {
        LoginRequest request = new LoginRequest("admin@testtenant.com", "WrongPassword", "test-tenant");

        ResponseEntity<ProblemDetails> response = restTemplate.postForEntity(
                "/api/v1/auth/login",
                request,
                ProblemDetails.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(401);
        assertThat(response.getBody().title()).isEqualTo("Unauthorized Access");
        assertThat(response.getBody().detail()).contains("Invalid tenant or credentials");
        assertThat(response.getBody().correlationId()).isNotEmpty();
    }

    @Test
    public void testRefreshTokenRotation() {
        // Step 1: Login
        LoginRequest request = new LoginRequest("admin@testtenant.com", "SecurePassword123", "test-tenant");
        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login",
                request,
                LoginResponse.class
        );
        String refreshToken = loginResponse.getBody().refreshToken();

        // Step 2: Refresh token
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);
        ResponseEntity<LoginResponse> refreshResponse = restTemplate.postForEntity(
                "/api/v1/auth/refresh",
                refreshRequest,
                LoginResponse.class
        );

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResponse.getBody()).isNotNull();
        assertThat(refreshResponse.getBody().accessToken()).isNotEmpty();
        assertThat(refreshResponse.getBody().refreshToken()).isNotEqualTo(refreshToken);

        // Step 3: Reuse old refresh token (should fail)
        ResponseEntity<ProblemDetails> reuseResponse = restTemplate.postForEntity(
                "/api/v1/auth/refresh",
                refreshRequest,
                ProblemDetails.class
        );
        assertThat(reuseResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void testCreateApiKeyAndVerify() {
        // Step 1: Login to get access token
        LoginRequest request = new LoginRequest("admin@testtenant.com", "SecurePassword123", "test-tenant");
        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login",
                request,
                LoginResponse.class
        );
        String accessToken = loginResponse.getBody().accessToken();

        // Step 2: Create API key
        CreateApiKeyRequest createKeyRequest = new CreateApiKeyRequest(
                "Gateway Key",
                List.of("telemetry:write", "vehicles:view"),
                100,
                OffsetDateTime.now().plusMonths(6)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<CreateApiKeyRequest> entity = new HttpEntity<>(createKeyRequest, headers);

        ResponseEntity<ApiKeyResponse> keyResponse = restTemplate.postForEntity(
                "/api/v1/auth/api-keys",
                entity,
                ApiKeyResponse.class
        );

        assertThat(keyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(keyResponse.getBody()).isNotNull();
        String rawKey = keyResponse.getBody().key();
        assertThat(rawKey).startsWith("flq_");

        // Step 3: Verify API key via query parameter
        ResponseEntity<ApiKeyVerificationResponse> verifyQueryResponse = restTemplate.getForEntity(
                "/api/v1/auth/verify-key?key=" + rawKey,
                ApiKeyVerificationResponse.class
        );
        assertThat(verifyQueryResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(verifyQueryResponse.getBody()).isNotNull();
        assertThat(verifyQueryResponse.getBody().valid()).isTrue();
        assertThat(verifyQueryResponse.getBody().tenantId()).isEqualTo(testTenant.getId());
        assertThat(verifyQueryResponse.getBody().scopes()).containsExactlyInAnyOrder("telemetry:write", "vehicles:view");
        assertThat(verifyQueryResponse.getBody().rateLimitPerMinute()).isEqualTo(100);

        // Step 4: Verify API key via header
        HttpHeaders verifyHeaders = new HttpHeaders();
        verifyHeaders.set("X-API-Key", rawKey);
        HttpEntity<Void> verifyEntity = new HttpEntity<>(verifyHeaders);

        ResponseEntity<ApiKeyVerificationResponse> verifyHeaderResponse = restTemplate.exchange(
                "/api/v1/auth/verify-key",
                HttpMethod.GET,
                verifyEntity,
                ApiKeyVerificationResponse.class
        );
        assertThat(verifyHeaderResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(verifyHeaderResponse.getBody().valid()).isTrue();

        // Step 5: Verify invalid key
        ResponseEntity<ApiKeyVerificationResponse> verifyInvalidResponse = restTemplate.getForEntity(
                "/api/v1/auth/verify-key?key=invalid_key",
                ApiKeyVerificationResponse.class
        );
        assertThat(verifyInvalidResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(verifyInvalidResponse.getBody().valid()).isFalse();
    }
}
