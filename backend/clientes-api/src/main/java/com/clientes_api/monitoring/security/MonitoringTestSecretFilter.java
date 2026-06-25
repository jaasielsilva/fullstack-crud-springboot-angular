package com.clientes_api.monitoring.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Profile("hml")
@ConditionalOnProperty(name = "monitoring.test-endpoints.enabled", havingValue = "true")
public class MonitoringTestSecretFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Monitoring-Test-Secret";
    private static final String PATH_PREFIX = "/api/internal/monitoring-test/";

    @Value("${monitoring.test-endpoints.secret:}")
    private String secret;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith(PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (secret == null || secret.isBlank()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getOutputStream().write(
                    "{\"erro\":\"Monitoring test secret não configurado\"}".getBytes(StandardCharsets.UTF_8));
            return;
        }
        String provided = request.getHeader(HEADER);
        if (provided == null || !secret.equals(provided)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getOutputStream().write(
                    "{\"erro\":\"Secret de monitoring test inválido\"}".getBytes(StandardCharsets.UTF_8));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
