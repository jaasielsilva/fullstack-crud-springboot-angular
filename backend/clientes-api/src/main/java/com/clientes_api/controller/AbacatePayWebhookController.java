package com.clientes_api.controller;

import com.clientes_api.model.WebhookEvento;
import com.clientes_api.repository.WebhookEventoRepository;
import com.clientes_api.service.AbacatePayWebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Webhook Abacate Pay — responde 200 após persistir o evento (evita reenvios).
 * Idempotência por {@code id} do payload (coluna genérica {@code mercado_pago_id}).
 */
@Hidden
@RestController
@RequestMapping("/api/webhooks")
public class AbacatePayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(AbacatePayWebhookController.class);

    private static final String TIPO_EVENTO = "abacate_event";

    private final ObjectMapper objectMapper;
    private final WebhookEventoRepository webhookEventoRepository;
    private final AbacatePayWebhookService abacatePayWebhookService;

    @Value("${abacatepay.log-webhook-inbound:false}")
    private boolean logWebhookInbound;

    public AbacatePayWebhookController(ObjectMapper objectMapper,
                                       WebhookEventoRepository webhookEventoRepository,
                                       AbacatePayWebhookService abacatePayWebhookService) {
        this.objectMapper = objectMapper;
        this.webhookEventoRepository = webhookEventoRepository;
        this.abacatePayWebhookService = abacatePayWebhookService;
    }

    @PostMapping("/abacatepay")
    public ResponseEntity<Void> receber(
            @RequestBody String rawBody,
            @RequestParam(value = "webhookSecret", required = false) String webhookSecret,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature
    ) {
        try {
            String eventId = extrairIdIdempotente(rawBody);
            if (eventId.isBlank()) {
                if (logWebhookInbound) {
                    log.warn("Abacate Pay webhook | sem id para idempotência; ignorando processamento");
                }
                return ResponseEntity.ok().build();
            }

            String eventIdDb = truncarIdParaColuna(eventId);

            WebhookEvento evento = webhookEventoRepository
                    .findByTipoAndMercadoPagoId(TIPO_EVENTO, eventIdDb)
                    .orElseGet(() -> {
                        WebhookEvento novo = new WebhookEvento();
                        novo.setTipo(TIPO_EVENTO);
                        novo.setMercadoPagoId(eventIdDb);
                        novo.setPayloadJson(rawBody == null ? "{}" : rawBody);
                        novo.setProcessado(false);
                        return webhookEventoRepository.save(novo);
                    });

            if (Boolean.TRUE.equals(evento.getProcessado())) {
                if (logWebhookInbound) {
                    log.info("Abacate Pay webhook | idempotente | eventId={}", eventIdDb);
                }
                return ResponseEntity.ok().build();
            }

            try {
                boolean ok = abacatePayWebhookService.processar(rawBody, webhookSecret, signature);
                if (ok) {
                    evento.setProcessado(true);
                    webhookEventoRepository.save(evento);
                }
            } catch (Exception ex) {
                log.error("Abacate Pay webhook | falha ao processar eventId={}: {}", eventIdDb, ex.getMessage(), ex);
            }
        } catch (Exception e) {
            log.warn("Abacate Pay webhook | parse inicial falhou: {}", e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    private String extrairIdIdempotente(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String id = root.path("id").asText("").trim();
            if (!id.isEmpty()) {
                return id;
            }
            JsonNode checkout = root.path("data").path("checkout");
            String checkoutId = checkout.path("id").asText("").trim();
            String ev = root.path("event").asText("").trim();
            if (!checkoutId.isEmpty()) {
                return checkoutId + "_" + ev;
            }
        } catch (Exception ignored) {
            // retorno vazio abaixo
        }
        return "";
    }

    /** Coluna {@code mercado_pago_id} tem limite 64 caracteres (hash quando o id nativo é longo). */
    private static String truncarIdParaColuna(String id) {
        if (id == null) {
            return "";
        }
        String t = id.trim();
        if (t.length() <= 64) {
            return t;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(t.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return t.substring(0, 64);
        }
    }
}
