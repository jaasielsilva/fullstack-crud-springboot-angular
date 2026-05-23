package com.clientes_api.service;

import com.clientes_api.model.WebhookEvento;
import com.clientes_api.repository.WebhookEventoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Persistência do evento bruto do webhook em transação curta e agendamento do processamento após commit.
 */
@Service
public class MercadoPagoWebhookIngressService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookIngressService.class);

    private final WebhookEventoRepository webhookEventoRepository;
    private final MercadoPagoPaymentWebhookProcessor mercadoPagoPaymentWebhookProcessor;

    public MercadoPagoWebhookIngressService(WebhookEventoRepository webhookEventoRepository,
                                            MercadoPagoPaymentWebhookProcessor mercadoPagoPaymentWebhookProcessor) {
        this.webhookEventoRepository = webhookEventoRepository;
        this.mercadoPagoPaymentWebhookProcessor = mercadoPagoPaymentWebhookProcessor;
    }

    @Transactional
    public void persistRawEventAndScheduleAfterCommit(String paymentId, String payloadJson) {
        WebhookEvento evento = webhookEventoRepository
                .findByTipoAndMercadoPagoId("payment", paymentId)
                .orElseGet(() -> {
                    WebhookEvento novo = new WebhookEvento();
                    novo.setTipo("payment");
                    novo.setMercadoPagoId(paymentId);
                    novo.setProcessado(false);
                    novo.setPayloadJson(payloadJson);
                    return webhookEventoRepository.save(novo);
                });
        evento.setPayloadJson(payloadJson);
        WebhookEvento salvo = webhookEventoRepository.save(evento);
        final Long eventoId = salvo.getId();

        log.info("Mercado Pago webhook | evento persistido | paymentId={} | webhookEventoId={}", paymentId, eventoId);

        Runnable agendar = () -> {
            log.info("Mercado Pago webhook | processamento assíncrono enfileirado | paymentId={} | webhookEventoId={}", paymentId, eventoId);
            mercadoPagoPaymentWebhookProcessor.processPaymentAsync(paymentId, eventoId);
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    agendar.run();
                }
            });
        } else {
            log.warn("Mercado Pago webhook | sem sincronização transacional ativa; enfileirando imediatamente | paymentId={}", paymentId);
            agendar.run();
        }
    }
}
