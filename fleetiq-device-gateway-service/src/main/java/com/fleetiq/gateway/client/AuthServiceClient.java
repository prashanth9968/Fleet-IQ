package com.fleetiq.gateway.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

@Component
public class AuthServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceClient.class);

    private final RestTemplate restTemplate;
    private final String authServiceUrl;

    public AuthServiceClient(
            @Value("${app.auth-service.url}") String authServiceUrl
    ) {
        this.restTemplate = new RestTemplate();
        this.authServiceUrl = authServiceUrl;
    }

    public VerificationResult verifyKey(String apiKey) {
        String url = authServiceUrl + "/api/v1/auth/verify-key?key=" + apiKey;
        try {
            ResponseEntity<VerificationResponse> response = restTemplate.getForEntity(url, VerificationResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                VerificationResponse body = response.getBody();
                return new VerificationResult(body.valid(), body.tenantId(), body.scopes(), body.rateLimitPerMinute());
            }
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("API Key verification failed: unauthorized");
        } catch (Exception e) {
            log.error("Failed to connect to Auth Service for API key verification", e);
        }
        return new VerificationResult(false, null, List.of(), null);
    }

    public record VerificationResponse(
            boolean valid,
            UUID tenantId,
            List<String> scopes,
            Integer rateLimitPerMinute
    ) {}

    public record VerificationResult(
            boolean isValid,
            UUID tenantId,
            List<String> scopes,
            Integer rateLimitPerMinute
    ) {}
}
