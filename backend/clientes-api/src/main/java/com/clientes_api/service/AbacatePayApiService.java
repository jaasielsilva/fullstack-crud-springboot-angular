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
 * Cliente HTTP para a API AbacatePay <strong>v2</strong> ({@code https://api.abacatepay.com/v2}).
 * Chaves novas do dashboard exigem v2; v1 retorna 401 "API key version mismatch".
 *
 * @see <a href="https://docs.abacatepay.com/pages/payment/create">Criar checkout</a>
 * @see <a href="https://docs.abacatepay.com/pages/products/create">Criar produto</a>
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
     * POST {@code /products/create} — corpo e resposta conforme OpenAPI v2.
     */
    public JsonNode criarProduto(ObjectNode payload) {
        return postJson("/products/create", payload, "produto");
    }

    /**
     * POST {@code /checkouts/create} — corpo e resposta conforme OpenAPI v2.
     * <p>
     * A API devolve o billing em {@code data}; este método devolve o nó {@code data}
     * quando existir, para que {@code id} e {@code url} fiquem no nível esperado pelos serviços.
     */
    public JsonNode criarCheckout(ObjectNode payload) {
        JsonNode root = postJson("/checkouts/create", payload, "checkout");
        return desembrulharDataCheckout(root);
    }

    /**
     * Respostas v2: {@code { "success": true, "data": { "id", "url", ... } }}.
     */
    static JsonNode desembrulharDataCheckout(JsonNode root) {
        if (root == null || root.isNull() || !root.isObject()) {
            return root;
        }
        JsonNode data = root.get("data");
        if (data != null && data.isObject() && (data.has("url") || data.has("id"))) {
            return data;
        }
        return root;
    }

    private JsonNode postJson(String path, ObjectNode payload, String recurso) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(
                    "Abacate Pay não configurado: defina abacatepay.api-key (variável ABACATEPAY_API_KEY).");
        }
        final String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Falha ao serializar JSON da requisição Abacate Pay (" + recurso + ").");
        }
        try {
            String responseBody = abacatePayRestClient.post()
                    .uri(path)
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
            throw new BusinessException("Resposta inválida da Abacate Pay (" + recurso + ").");
        } catch (RestClientResponseException e) {
            String errBody = safeBody(e);
            log.warn("Abacate Pay {} falhou: http={} body={}", path, e.getStatusCode().value(), trunc(errBody));
            String hint = hintParaErroHttp(path, e.getStatusCode().value(), errBody);
            throw new BusinessException("Erro ao chamar Abacate Pay (" + recurso + "): HTTP "
                    + e.getStatusCode().value()
                    + bodyParaMensagem(errBody)
                    + hint);
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

    private static String hintParaErroHttp(String path, int status, String errBody) {
        if (status == 401) {
            return " Verifique se a chave é da API v2 e se tem permissões (ex.: PRODUCT:CREATE, CHECKOUT:CREATE).";
        }
        if (status == 400 && path.contains("checkouts")) {
            String lower = errBody.toLowerCase();
            if (lower.contains("product")) {
                return " No painel Abacate Pay, crie dois produtos (ex.: \"Plano Básico\" R$ 29,90 e \"Plano Premium\" R$ 49,90/mês)"
                        + " na mesma conta da sua API key, copie o ID público de cada um (ex.: prod_…)"
                        + " e configure no servidor: ABACATEPAY_PRODUCT_ID_BASICO e ABACATEPAY_PRODUCT_ID_PREMIUM.";
            }
        }
        return "";
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
