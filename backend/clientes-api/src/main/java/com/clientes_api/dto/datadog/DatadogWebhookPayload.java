package com.clientes_api.dto.datadog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Exemplo de payload genérico (Custom Webhook / teste manual). Campos desconhecidos são ignorados.
 * O controller aceita qualquer JSON adicional via {@link com.fasterxml.jackson.databind.JsonNode}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatadogWebhookPayload {

    private String title;
    private String text;
    @JsonProperty("alert_type")
    private String alertType;
    /** Epoch em segundos (ex.: 1710000000) ou milissegundos, conforme origem. */
    private Long date;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public Long getDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }
}
