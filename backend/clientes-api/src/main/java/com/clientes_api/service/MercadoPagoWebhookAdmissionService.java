package com.clientes_api.service;

import com.clientes_api.dto.MercadoPagoWebhookDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Entrada do webhook Mercado Pago: validação do corpo, extração do {@code paymentId} e delegação à persistência/agendamento.
 */
@Service
public class MercadoPagoWebhookAdmissionService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookAdmissionService.class);

    private static final int MAX_WEBHOOK_LOG_PREVIEW = 1500;

    private final ObjectMapper objectMapper;
    private final MercadoPagoWebhookIngressService mercadoPagoWebhookIngressService;

    @Value("${mercadopago.log-webhook-inbound:false}")
    private boolean logWebhookInbound;

    public MercadoPagoWebhookAdmissionService(ObjectMapper objectMapper,
                                              MercadoPagoWebhookIngressService mercadoPagoWebhookIngressService) {
        this.objectMapper = objectMapper;
        this.mercadoPagoWebhookIngressService = mercadoPagoWebhookIngressService;
    }

    /**
     * Não propaga exceção: o endpoint HTTP deve responder 200 para o Mercado Pago mesmo em caso de payload inválido.
     */
    public void handleInboundWebhook(String rawBody) {
        try {
            if (logWebhookInbound) {
                int len = rawBody == null ? 0 : rawBody.length();
                log.info("Mercado Pago webhook | POST recebido | bodyLen={} | preview={}", len, previewBody(rawBody));
            }

            String rawPaymentId = extrairPaymentId(rawBody);
            if (rawPaymentId == null || rawPaymentId.isBlank()) {
                if (logWebhookInbound) {
                    log.info("Mercado Pago webhook | ignorado (sem payment id) | preview={}", previewBody(rawBody));
                }
                return;
            }

            final String paymentId = rawPaymentId.trim();
            String payloadPersistido = payloadParaPersistencia(rawBody);

            log.info("Mercado Pago webhook | aceito | paymentId={}", paymentId);
            mercadoPagoWebhookIngressService.persistRawEventAndScheduleAfterCommit(paymentId, payloadPersistido);
        } catch (Exception e) {
            log.warn("Mercado Pago webhook | falha na admissão: {}", e.getMessage(), e);
        }
    }

    private String extrairPaymentId(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return null;
        }
        String trimmed = rawBody.trim();
        if (trimmed.startsWith("{")) {
            try {
                MercadoPagoWebhookDTO dto = objectMapper.readValue(trimmed, MercadoPagoWebhookDTO.class);
                if (!isNotificacaoDePagamento(dto)) {
                    return null;
                }
                if (dto.data() == null || dto.data().id() == null || dto.data().id().isBlank()) {
                    return null;
                }
                return dto.data().id().trim();
            } catch (Exception e) {
                log.debug("Mercado Pago webhook | JSON não parseado como notificação padrão: {}", e.getMessage());
                return null;
            }
        }
        return extrairPaymentIdFormUrlEncoded(trimmed).orElse(null);
    }

    private static boolean isNotificacaoDePagamento(MercadoPagoWebhookDTO dto) {
        if (dto.type() != null && !dto.type().isBlank() && "payment".equalsIgnoreCase(dto.type().trim())) {
            return true;
        }
        if (dto.action() != null && !dto.action().isBlank()) {
            return dto.action().trim().toLowerCase().startsWith("payment.");
        }
        return false;
    }

    private static Optional<String> extrairPaymentIdFormUrlEncoded(String body) {
        try {
            String topic = null;
            String id = null;
            for (String part : body.split("&")) {
                int eq = part.indexOf('=');
                if (eq < 0) {
                    continue;
                }
                String key = URLDecoder.decode(part.substring(0, eq), StandardCharsets.UTF_8).trim();
                String val = URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8).trim();
                if ("topic".equalsIgnoreCase(key)) {
                    topic = val;
                }
                if ("id".equalsIgnoreCase(key)) {
                    id = val;
                }
            }
            if ("payment".equalsIgnoreCase(topic) && id != null && !id.isBlank()) {
                return Optional.of(id.trim());
            }
        } catch (Exception ignored) {
            // vazio
        }
        return Optional.empty();
    }

    private static String payloadParaPersistencia(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return "{}";
        }
        return rawBody;
    }

    private static String previewBody(String rawBody) {
        if (rawBody == null || rawBody.isEmpty()) {
            return "";
        }
        String t = rawBody.trim();
        if (t.length() <= MAX_WEBHOOK_LOG_PREVIEW) {
            return t;
        }
        return t.substring(0, MAX_WEBHOOK_LOG_PREVIEW) + "...";
    }
}
