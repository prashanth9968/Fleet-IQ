package com.fleetiq.analytics.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TenantFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String CORRELATION_HEADER = "X-Correlation-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Handle Correlation ID
        String correlationId = request.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put("traceId", correlationId);
        response.setHeader(CORRELATION_HEADER, correlationId);

        // 2. Handle Tenant ID
        String tenantHeader = request.getHeader(TENANT_HEADER);
        if (tenantHeader != null && !tenantHeader.trim().isEmpty()) {
            try {
                UUID tenantId = UUID.fromString(tenantHeader);
                TenantContext.setCurrentTenant(tenantId);
                MDC.put("tenantId", tenantId.toString());
            } catch (IllegalArgumentException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/problem+json");
                response.getWriter().write(
                        "{\"type\":\"https://api.fleetiq.com/errors/invalid-tenant\"," +
                        "\"title\":\"Invalid Tenant Header\"," +
                        "\"status\":400," +
                        "\"detail\":\"The X-Tenant-ID header must be a valid UUID.\"," +
                        "\"instance\":\"" + request.getRequestURI() + "\"}"
                );
                return;
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            MDC.remove("tenantId");
            MDC.remove("traceId");
        }
    }
}
