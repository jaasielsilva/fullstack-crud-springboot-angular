package com.clientes_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code init_point} espelha {@link #checkoutUrl} (mesmo valor do Mercado Pago) para clientes que leem o nome da API do MP.
 */
public record CheckoutResponseDTO(
        String checkoutUrl,
        String preferenceId,
        @JsonProperty("init_point") String initPoint
) {
    public CheckoutResponseDTO(String checkoutUrl, String preferenceId) {
        this(checkoutUrl, preferenceId, checkoutUrl);
    }
}
