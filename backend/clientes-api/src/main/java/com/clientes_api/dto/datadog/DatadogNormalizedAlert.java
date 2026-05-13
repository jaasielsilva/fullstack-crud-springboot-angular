package com.clientes_api.dto.datadog;

import java.time.Instant;

/**
 * Campos normalizados após interpretar payloads heterogêneos do Datadog (monitor, Event API, exemplos genéricos).
 */
public record DatadogNormalizedAlert(
        String title,
        String message,
        AlertSeverity severity,
        Instant occurredAt,
        String service,
        String endpoint
) {
    public enum AlertSeverity {
        ALERT,
        WARNING,
        OK
    }
}
