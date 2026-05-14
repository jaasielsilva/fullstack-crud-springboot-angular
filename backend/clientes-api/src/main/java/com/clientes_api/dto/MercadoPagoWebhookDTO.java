package com.clientes_api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MercadoPagoWebhookDTO(String type, String action, MercadoPagoWebhookDataDTO data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MercadoPagoWebhookDataDTO(String id) {}
}
