package com.clientes_api.service;

import com.clientes_api.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class MercadoPagoApiService {

    private final RestClient mercadoPagoRestClient;

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    public MercadoPagoApiService(RestClient mercadoPagoRestClient) {
        this.mercadoPagoRestClient = mercadoPagoRestClient;
    }

    public JsonNode criarPreferencia(JsonNode corpo) {
        validarToken();
        try {
            return mercadoPagoRestClient.post()
                    .uri("/checkout/preferences")
                    .header("Authorization", "Bearer " + accessToken.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            throw new BusinessException("Erro ao criar preferência no Mercado Pago: HTTP " + e.getStatusCode().value());
        }
    }

    public JsonNode buscarPagamento(String paymentId) {
        validarToken();
        try {
            return mercadoPagoRestClient.get()
                    .uri("/v1/payments/{id}", paymentId)
                    .header("Authorization", "Bearer " + accessToken.trim())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            throw new BusinessException("Erro ao consultar pagamento no Mercado Pago: HTTP " + e.getStatusCode().value());
        }
    }

    private void validarToken() {
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException("Integração Mercado Pago não configurada (mercadopago.access-token).");
        }
    }
}
