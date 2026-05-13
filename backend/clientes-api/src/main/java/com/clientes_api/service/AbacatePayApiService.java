package com.clientes_api.service;

import com.clientes_api.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Cliente HTTP para a API AbacatePay ({@code https://api.abacatepay.com/v1}).
 *
 * @see <a href="https://docs.abacatepay.com/api-reference/criar-uma-nova-cobran%C3%A7a">Criar cobrança</a>
 */
@Service
public class AbacatePayApiService {

    private static final Logger log = LoggerFactory.getLogger(AbacatePayApiService.class);

    private static final int MAX_ERROR_BODY = 1200;

    private final RestClient abacatePayRestClient;
    private final ObjectMapper objectMapper;

    @Value("${abacatepay.api-key:}")
    private String apiKey;

    public AbacatePayApiService(@Qualifier("abacatePayRestClient") RestClient abacatePayRestClient,
                                ObjectMapper objectMapper) {
        this.abacatePayRestClient = abacatePayRestClient;
        this.objectMapper = objectMapper;
    }

    /**
     * POST {@code /v1/billing/create} — retorna o JSON completo (usa {@code data.url} e {@code data.id}).
     */
    public JsonNode criarCobranca(ObjectNode payload) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(
                    "Abacate Pay não configurado: defina abacatepay.api-key (variável ABACATEPAY_API_KEY).");
        }
        final String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Falha ao serializar JSON da cobrança Abacate Pay.");
        }
        try {
            String responseBody = abacatePayRestClient.post()
                    .uri("/v1/billing/create")
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.hasNonNull("error")) {
                String err = root.get("error").asText("Erro retornado pela Abacate Pay.");
                throw new BusinessException("Abacate Pay: " + err);
            }
            return root;
        } catch (JsonProcessingException e) {
            throw new BusinessException("Resposta inválida da Abacate Pay ao criar cobrança.");
        } catch (RestClientResponseException e) {
            String body = safeBody(e);
            log.warn("Abacate Pay billing/create falhou: http={} body={}", e.getStatusCode().value(), trunc(body));
            throw new BusinessException("Erro ao criar cobrança na Abacate Pay: HTTP "
                    + e.getStatusCode().value()
                    + bodyParaMensagem(body));
        }
    }

    private static String safeBody(RestClientResponseException e) {
        try {
            String raw = e.getResponseBodyAsString();
            return raw == null ? "" : raw.trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String bodyParaMensagem(String body) {
        if (body.isEmpty()) {
            return "";
        }
        try {
            ObjectMapper om = new ObjectMapper();
            JsonNode root = om.readTree(body);
            if (root.hasNonNull("error")) {
                return " — " + trunc(root.get("error").asText());
            }
        } catch (Exception ignored) {
            // ignore
        }
        return " — " + trunc(body);
    }

    private static String trunc(String s) {
        if (s.length() <= MAX_ERROR_BODY) {
            return s;
        }
        return s.substring(0, MAX_ERROR_BODY) + "...";
    }
}
