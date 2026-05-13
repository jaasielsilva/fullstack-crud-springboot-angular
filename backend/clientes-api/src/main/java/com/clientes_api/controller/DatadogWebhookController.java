package com.clientes_api.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.clientes_api.service.AlertService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook Datadog → formatação → Telegram (chat do super admin).
 * <p>
 * URL pública (com prefixo da API): {@code POST /api/webhooks/datadog}
 * </p>
 */
@Hidden
@RestController
@RequestMapping("/api/webhooks")
public class DatadogWebhookController {

    public static final String HEADER_DATADOG_TOKEN = "X-Datadog-Token";

    private static final Logger log = LoggerFactory.getLogger(DatadogWebhookController.class);

    private final ObjectMapper objectMapper;
    private final AlertService alertService;
    private final String webhookSecret;
    private final String allowedIpsCsv;
    private final boolean logPayloadPreview;

    public DatadogWebhookController(
            ObjectMapper objectMapper,
            AlertService alertService,
            @Value("${datadog.webhook.secret:}") String webhookSecret,
            @Value("${datadog.webhook.allowed-ips:}") String allowedIpsCsv,
            @Value("${datadog.webhook.log-payload-preview:false}") boolean logPayloadPreview) {
        this.objectMapper = objectMapper;
        this.alertService = alertService;
        this.webhookSecret = webhookSecret;
        this.allowedIpsCsv = allowedIpsCsv;
        this.logPayloadPreview = logPayloadPreview;
    }

    @PostMapping(value = "/datadog", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> receber(
            @RequestHeader(value = HEADER_DATADOG_TOKEN, required = false) String token,
            @RequestBody String rawBody,
            HttpServletRequest request) {

        if (!isClientIpAllowed(request)) {
            log.warn("Datadog webhook | IP não autorizado | remote={} | xff={}", request.getRemoteAddr(), request.getHeader("X-Forwarded-For"));
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"error\":\"ip_not_allowed\"}");
        }

        String expected = webhookSecret == null ? "" : webhookSecret.trim();
        if (expected.isEmpty()) {
            log.warn("Datadog webhook | DATADOG_WEBHOOK_SECRET não configurado no servidor");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\":\"webhook_not_configured\"}");
        }

        if (!constantTimeEquals(token == null ? "" : token.trim(), expected)) {
            log.warn("Datadog webhook | token inválido ou ausente");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\":\"unauthorized\"}");
        }

        if (logPayloadPreview && rawBody != null) {
            int len = rawBody.length();
            String preview = len <= 1200 ? rawBody : rawBody.substring(0, 1200) + "...";
            log.info("Datadog webhook | bodyLen={} | preview={}", len, preview);
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody == null || rawBody.isBlank() ? "{}" : rawBody);
            alertService.handleDatadogPayload(root);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body("{\"status\":\"processed\"}");
        } catch (Exception e) {
            log.warn("Datadog webhook | JSON inválido: {}", e.getMessage());
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"invalid_json\"}");
        }
    }

    private boolean isClientIpAllowed(HttpServletRequest request) {
        if (allowedIpsCsv == null || allowedIpsCsv.isBlank()) {
            return true;
        }
        String clientIp = clientIp(request);
        var allowed = Arrays.stream(allowedIpsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        return allowed.contains(clientIp);
    }

    /**
     * Considera o primeiro IP de {@code X-Forwarded-For} quando presente (proxy/Nginx).
     */
    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr() == null ? "" : request.getRemoteAddr().trim();
    }

    private static boolean constantTimeEquals(String provided, String expected) {
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = provided.getBytes(StandardCharsets.UTF_8);
        if (a.length != b.length) {
            return false;
        }
        return MessageDigest.isEqual(a, b);
    }
}
