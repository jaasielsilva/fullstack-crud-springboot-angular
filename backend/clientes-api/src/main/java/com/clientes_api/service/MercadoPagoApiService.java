package com.clientes_api.service;

import com.clientes_api.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class MercadoPagoApiService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoApiService.class);

    /** Limite para não estourar payload de erro HTTP na resposta da API. */
    private static final int MAX_MP_ERROR_BODY = 1200;

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
            throw preferenciaException(e);
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
            String body = safeResponseBody(e);
            log.warn("Mercado Pago buscarPagamento falhou: http={} paymentId={} body={}",
                    e.getStatusCode().value(), paymentId, body);
            throw new BusinessException("Erro ao consultar pagamento no Mercado Pago: HTTP "
                    + e.getStatusCode().value()
                    + bodyParaMensagem(body));
        }
    }

    private void validarToken() {
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException("Integração Mercado Pago não configurada (mercadopago.access-token).");
        }
    }

    private BusinessException preferenciaException(RestClientResponseException e) {
        String body = safeResponseBody(e);
        log.warn("Mercado Pago criarPreferencia falhou: http={} body={}", e.getStatusCode().value(), body);
        return new BusinessException("Erro ao criar preferência no Mercado Pago: HTTP "
                + e.getStatusCode().value()
                + bodyParaMensagem(body));
    }

    private static String safeResponseBody(RestClientResponseException e) {
        try {
            String raw = e.getResponseBodyAsString();
            return raw == null ? "" : raw.trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    /**
     * Anexa trecho do JSON de erro do MP (campo {@code message} / {@code cause}) ou o corpo truncado.
     */
    private static String bodyParaMensagem(String body) {
        if (body.isEmpty()) {
            return "";
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            JsonNode root = om.readTree(body);
            StringBuilder sb = new StringBuilder(" — ");
            if (root.hasNonNull("message")) {
                sb.append(root.get("message").asText());
            }
            if (root.has("cause") && root.get("cause").isArray()) {
                for (JsonNode c : root.get("cause")) {
                    if (c.hasNonNull("description")) {
                        if (sb.length() > 3) {
                            sb.append(" | ");
                        }
                        sb.append(c.get("description").asText());
                    }
                }
            }
            if (sb.length() > 3) {
                return truncar(sb.toString(), MAX_MP_ERROR_BODY);
            }
        } catch (Exception ignored) {
            // cai no corpo bruto abaixo
        }
        return " — " + truncar(body, MAX_MP_ERROR_BODY);
    }

    private static String truncar(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "...";
    }
}
