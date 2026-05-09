package com.clientes_api.controller;

import com.clientes_api.dto.MercadoPagoWebhookDTO;
import com.clientes_api.model.WebhookEvento;
import com.clientes_api.repository.WebhookEventoRepository;
import com.clientes_api.service.MercadoPagoWebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook Mercado Pago — sempre responde 200 após persistir o evento bruto (evita reenvios infinitos).
 * Erros de processamento ficam na auditoria ({@link WebhookEvento}) e nos logs.
 */
@Hidden
@RestController
@RequestMapping("/api/webhooks")
public class MercadoPagoWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    private final ObjectMapper objectMapper;
    private final WebhookEventoRepository webhookEventoRepository;
    private final MercadoPagoWebhookService mercadoPagoWebhookService;

    public MercadoPagoWebhookController(ObjectMapper objectMapper,
                                        WebhookEventoRepository webhookEventoRepository,
                                        MercadoPagoWebhookService mercadoPagoWebhookService) {
        this.objectMapper = objectMapper;
        this.webhookEventoRepository = webhookEventoRepository;
        this.mercadoPagoWebhookService = mercadoPagoWebhookService;
    }

    @PostMapping("/mercadopago")
    public ResponseEntity<Void> receber(@RequestBody String rawBody) {
        try {
            MercadoPagoWebhookDTO dto = objectMapper.readValue(rawBody, MercadoPagoWebhookDTO.class);
            if (dto.type() == null || !"payment".equalsIgnoreCase(dto.type())) {
                return ResponseEntity.ok().build();
            }
            if (dto.data() == null || dto.data().id() == null || dto.data().id().isBlank()) {
                return ResponseEntity.ok().build();
            }

            String paymentId = dto.data().id().trim();
            WebhookEvento evento = webhookEventoRepository
                    .findByTipoAndMercadoPagoId("payment", paymentId)
                    .orElseGet(() -> {
                        WebhookEvento novo = new WebhookEvento();
                        novo.setTipo("payment");
                        novo.setMercadoPagoId(paymentId);
                        novo.setPayloadJson(rawBody);
                        novo.setProcessado(false);
                        return webhookEventoRepository.save(novo);
                    });

            if (Boolean.TRUE.equals(evento.getProcessado())) {
                return ResponseEntity.ok().build();
            }

            try {
                mercadoPagoWebhookService.processarPagamentoPorId(paymentId);
                evento.setProcessado(true);
                webhookEventoRepository.save(evento);
            } catch (Exception ex) {
                log.error("Falha ao processar webhook Mercado Pago paymentId={}: {}", paymentId, ex.getMessage(), ex);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.warn("Payload webhook inválido: {}", e.getMessage());
            return ResponseEntity.ok().build();
        }
    }
}
