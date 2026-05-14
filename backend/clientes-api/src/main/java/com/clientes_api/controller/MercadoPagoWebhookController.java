package com.clientes_api.controller;

import com.clientes_api.dto.MercadoPagoWebhookDTO;
import com.clientes_api.model.WebhookEvento;
import com.clientes_api.repository.WebhookEventoRepository;
import com.clientes_api.service.MercadoPagoWebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Webhook Mercado Pago — sempre responde 200 após persistir o evento bruto (evita reenvios infinitos).
 * Erros de processamento ficam na auditoria ({@link WebhookEvento}) e nos logs.
 * <p>
 * O mesmo {@code payment_id} pode receber várias notificações (ex.: {@code pending} e depois {@code approved}).
 * Não se deve ignorar notificações posteriores só porque a primeira já foi persistida.
 */
@Hidden
@RestController
@RequestMapping("/api/webhooks")
public class MercadoPagoWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    private static final int MAX_WEBHOOK_LOG_PREVIEW = 1500;

    private final ObjectMapper objectMapper;
    private final WebhookEventoRepository webhookEventoRepository;
    private final MercadoPagoWebhookService mercadoPagoWebhookService;

    @Value("${mercadopago.log-webhook-inbound:false}")
    private boolean logWebhookInbound;

    public MercadoPagoWebhookController(ObjectMapper objectMapper,
                                        WebhookEventoRepository webhookEventoRepository,
                                        MercadoPagoWebhookService mercadoPagoWebhookService) {
        this.objectMapper = objectMapper;
        this.webhookEventoRepository = webhookEventoRepository;
        this.mercadoPagoWebhookService = mercadoPagoWebhookService;
    }

    @PostMapping("/mercadopago")
    public ResponseEntity<Void> receber(@RequestBody String rawBody) {
        if (logWebhookInbound) {
            int len = rawBody == null ? 0 : rawBody.length();
            log.info("Mercado Pago webhook | POST recebido | bodyLen={} | preview={}", len, previewBody(rawBody));
        }
        try {
            String rawPaymentId = extrairPaymentId(rawBody);
            if (rawPaymentId == null || rawPaymentId.isBlank()) {
                if (logWebhookInbound) {
                    log.info("Mercado Pago webhook | ignorado (sem payment id) | preview={}", previewBody(rawBody));
                }
                return ResponseEntity.ok().build();
            }

            final String paymentId = rawPaymentId.trim();
            if (logWebhookInbound) {
                log.info("Mercado Pago webhook | processando paymentId={}", paymentId);
            }
            String payloadPersistido = payloadParaPersistencia(rawBody);
            WebhookEvento evento = webhookEventoRepository
                    .findByTipoAndMercadoPagoId("payment", paymentId)
                    .orElseGet(() -> {
                        WebhookEvento novo = new WebhookEvento();
                        novo.setTipo("payment");
                        novo.setMercadoPagoId(paymentId);
                        novo.setProcessado(false);
                        novo.setPayloadJson(payloadPersistido);
                        return webhookEventoRepository.save(novo);
                    });
            evento.setPayloadJson(payloadPersistido);

            try {
                mercadoPagoWebhookService.processarPagamentoPorId(paymentId);
                evento.setProcessado(true);
                webhookEventoRepository.save(evento);
                if (logWebhookInbound) {
                    log.info("Mercado Pago webhook | paymentId={} | processado com sucesso", paymentId);
                }
            } catch (Exception ex) {
                log.error("Falha ao processar webhook Mercado Pago paymentId={}: {}", paymentId, ex.getMessage(), ex);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.warn("Payload webhook inválido: {}", e.getMessage());
            if (logWebhookInbound) {
                log.warn("Mercado Pago webhook | parse falhou | preview={}", previewBody(rawBody));
            }
            return ResponseEntity.ok().build();
        }
    }

    /**
     * JSON do webhook ({@code type}/{@code action} + {@code data.id}) ou corpo {@code application/x-www-form-urlencoded}
     * ({@code topic=payment&id=...}) usado em notificações legadas.
     */
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

    /** {@code webhook_eventos.payload_json} é NOT NULL — corpo vazio vira JSON mínimo. */
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
