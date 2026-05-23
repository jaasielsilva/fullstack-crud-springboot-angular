package com.clientes_api.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.StreamSupport;

import com.clientes_api.dto.datadog.DatadogNormalizedAlert;
import com.clientes_api.dto.datadog.DatadogNormalizedAlert.AlertSeverity;
import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Interpreta payloads do Datadog, formata mensagem legível e envia ao Telegram (chat do super admin).
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private static final ZoneId BRAZIL = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter PT_BR = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT)
            .withZone(BRAZIL);

    private final TelegramService telegramService;

    public AlertService(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    /**
     * Normaliza o JSON, envia ao Telegram e engole falhas externas (só loga).
     */
    public void handleDatadogPayload(JsonNode root) {
        DatadogNormalizedAlert alert = normalize(root);
        String text = formatTelegramMessage(alert);
        boolean sent = telegramService.sendMessage(text);
        if (!sent) {
            log.warn("Datadog alert | Telegram não confirmou envio | título={}", alert.title());
        }
    }

    DatadogNormalizedAlert normalize(JsonNode root) {
        String title = firstNonBlank(
                textAt(root, "title"),
                textAt(root, "alert_title"),
                textAt(root, "event", "title"),
                "Alerta Datadog"
        );

        String message = firstNonBlank(
                textAt(root, "text"),
                textAt(root, "body"),
                textAt(root, "message"),
                textAt(root, "event", "text"),
                textAt(root, "msg"),
                "(sem detalhes)"
        );

        AlertSeverity severity = resolveSeverity(root, message, title);

        Instant occurredAt = resolveInstant(root);

        String service = firstNonBlank(
                textAt(root, "service"),
                textAt(root, "service_name"),
                extractTag(root, "service"),
                extractTag(root, "kube_deployment"),
                "erp-api"
        );

        String endpoint = firstNonBlank(
                textAt(root, "endpoint"),
                extractTag(root, "endpoint"),
                extractTag(root, "http.url"),
                ""
        );

        return new DatadogNormalizedAlert(title, message, severity, occurredAt, service, endpoint);
    }

    String formatTelegramMessage(DatadogNormalizedAlert a) {
        String headerEmoji = switch (a.severity()) {
            case ALERT -> "🚨";
            case WARNING -> "⚠️";
            case OK -> "🟢";
        };
        String headerLine = a.severity() == AlertSeverity.OK
                ? headerEmoji + " RECUPERADO"
                : headerEmoji + " ALERTA DATADOG";

        String statusLabel = switch (a.severity()) {
            case ALERT -> "ALERT";
            case WARNING -> "WARNING";
            case OK -> "OK";
        };

        String hora = PT_BR.format(a.occurredAt());

        StringBuilder sb = new StringBuilder();
        sb.append(headerLine).append("\n\n");
        if (a.title() != null && !a.title().isBlank()) {
            sb.append("Título: ").append(trimOneLine(a.title())).append('\n');
        }
        if (a.severity() != AlertSeverity.OK) {
            sb.append("Status: ").append(statusLabel).append('\n');
        }
        sb.append("Serviço: ").append(a.service()).append('\n');
        sb.append("Mensagem: ").append(trimOneLine(a.message())).append('\n');
        if (a.endpoint() != null && !a.endpoint().isBlank()) {
            sb.append("Endpoint: ").append(a.endpoint()).append('\n');
        }
        sb.append("Hora: ").append(hora);
        return sb.toString();
    }

    private static String trimOneLine(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\r\n", " ").replace('\n', ' ').trim();
    }

    private static AlertSeverity resolveSeverity(JsonNode root, String message, String title) {
        String haystack = (title + " " + message).toLowerCase(Locale.ROOT);
        String alertType = textAt(root, "alert_type").toLowerCase(Locale.ROOT);
        String transition = textAt(root, "alert_transition").toLowerCase(Locale.ROOT);
        String status = textAt(root, "status").toLowerCase(Locale.ROOT);
        String eventType = textAt(root, "event_type").toLowerCase(Locale.ROOT);

        if (containsRecovery(haystack, transition, status, eventType, alertType)) {
            return AlertSeverity.OK;
        }
        if ("warning".equals(alertType) || "warn".equals(alertType) || haystack.contains("warning")) {
            return AlertSeverity.WARNING;
        }
        if ("error".equals(alertType) || "error".equals(status) || haystack.contains("triggered")) {
            return AlertSeverity.ALERT;
        }
        if ("success".equals(alertType) || "info".equals(alertType)) {
            return AlertSeverity.OK;
        }
        return AlertSeverity.ALERT;
    }

    private static boolean containsRecovery(String hay, String transition, String status, String eventType, String alertType) {
        if (transition.contains("recovered") || transition.contains("resolve")) {
            return true;
        }
        if ("ok".equals(status) || "success".equals(status)) {
            return true;
        }
        if (eventType.contains("recovery") || eventType.contains("success")) {
            return true;
        }
        if (alertType.contains("success")) {
            return true;
        }
        return hay.contains("recovered")
                || hay.contains("resolved")
                || hay.contains("back to normal")
                || hay.contains("voltou ao normal");
    }

    private static Instant resolveInstant(JsonNode root) {
        if (root.has("date") && root.get("date").canConvertToLong()) {
            long epoch = root.get("date").asLong();
            if (epoch > 1_000_000_000_000L) {
                return Instant.ofEpochMilli(epoch);
            }
            return Instant.ofEpochSecond(epoch);
        }
        if (root.has("last_updated") && root.get("last_updated").canConvertToLong()) {
            long v = root.get("last_updated").asLong();
            return v > 1_000_000_000_000L ? Instant.ofEpochMilli(v) : Instant.ofEpochSecond(v);
        }
        if (root.has("timestamp") && root.get("timestamp").canConvertToLong()) {
            long v = root.get("timestamp").asLong();
            return v > 1_000_000_000_000L ? Instant.ofEpochMilli(v) : Instant.ofEpochSecond(v);
        }
        return Instant.now();
    }

    private static String textAt(JsonNode root, String... path) {
        JsonNode n = root;
        for (String p : path) {
            if (n == null || !n.has(p)) {
                return "";
            }
            n = n.get(p);
        }
        if (n == null || n.isNull()) {
            return "";
        }
        if (n.isTextual()) {
            return n.asText("");
        }
        if (n.isNumber()) {
            return n.asText();
        }
        return n.toString();
    }

    private static String extractTag(JsonNode root, String tagKey) {
        JsonNode tags = root.get("tags");
        if (tags == null || !tags.isArray()) {
            return "";
        }
        String prefix = tagKey + ":";
        return StreamSupport.stream(tags.spliterator(), false)
                .filter(JsonNode::isTextual)
                .map(JsonNode::asText)
                .filter(s -> s.startsWith(prefix))
                .map(s -> s.substring(prefix.length()))
                .findFirst()
                .orElse("");
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }

}
