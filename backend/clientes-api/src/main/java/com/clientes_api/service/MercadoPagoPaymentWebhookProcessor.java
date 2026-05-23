package com.clientes_api.service;

import com.clientes_api.config.TenantContext;
import com.clientes_api.model.WebhookEvento;
import com.clientes_api.repository.WebhookEventoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Processamento assíncrono do pagamento após o POST do webhook ter persistido o evento bruto e respondido 200.
 */
@Component
public class MercadoPagoPaymentWebhookProcessor {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoPaymentWebhookProcessor.class);

    private final MercadoPagoWebhookService mercadoPagoWebhookService;
    private final WebhookEventoRepository webhookEventoRepository;

    public MercadoPagoPaymentWebhookProcessor(MercadoPagoWebhookService mercadoPagoWebhookService,
                                              WebhookEventoRepository webhookEventoRepository) {
        this.mercadoPagoWebhookService = mercadoPagoWebhookService;
        this.webhookEventoRepository = webhookEventoRepository;
    }

    @Async("mercadoPagoWebhookExecutor")
    public void processPaymentAsync(String paymentId, Long webhookEventoId) {
        TenantContext.clear();
        try {
            log.info("Mercado Pago webhook | async iniciado | paymentId={} | webhookEventoId={}", paymentId, webhookEventoId);
            mercadoPagoWebhookService.processarPagamentoPorId(paymentId);
            marcarEventoProcessado(webhookEventoId);
            log.info("Mercado Pago webhook | async concluído | paymentId={} | webhookEventoId={}", paymentId, webhookEventoId);
        } catch (Exception ex) {
            log.error(
                    "Mercado Pago webhook | async falhou | paymentId={} | webhookEventoId={}: {}",
                    paymentId,
                    webhookEventoId,
                    ex.getMessage(),
                    ex
            );
        } finally {
            TenantContext.clear();
        }
    }

    private void marcarEventoProcessado(Long webhookEventoId) {
        if (webhookEventoId == null) {
            return;
        }
        webhookEventoRepository.findById(webhookEventoId).ifPresent(e -> {
            e.setProcessado(true);
            webhookEventoRepository.save(e);
        });
    }
}
