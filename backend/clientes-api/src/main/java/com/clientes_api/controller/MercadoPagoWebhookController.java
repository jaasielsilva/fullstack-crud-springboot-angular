package com.clientes_api.controller;

import com.clientes_api.service.MercadoPagoWebhookAdmissionService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook Mercado Pago — responde 200 após persistir o evento bruto e agendar o processamento assíncrono.
 * <p>
 * O mesmo {@code payment_id} pode receber várias notificações (ex.: {@code pending} e depois {@code approved}).
 */
@Hidden
@RestController
@RequestMapping("/api/webhooks")
public class MercadoPagoWebhookController {

    private final MercadoPagoWebhookAdmissionService mercadoPagoWebhookAdmissionService;

    public MercadoPagoWebhookController(MercadoPagoWebhookAdmissionService mercadoPagoWebhookAdmissionService) {
        this.mercadoPagoWebhookAdmissionService = mercadoPagoWebhookAdmissionService;
    }

    @PostMapping("/mercadopago")
    public ResponseEntity<Void> receber(@RequestBody String rawBody) {
        mercadoPagoWebhookAdmissionService.handleInboundWebhook(rawBody);
        return ResponseEntity.ok().build();
    }
}
