package com.clientes_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

/**
 * Orquestração do processamento de pagamento Mercado Pago (notificações assíncronas):
 * consulta a API do MP fora da transação JPA e aplica o resultado no domínio em transação isolada.
 */
@Service
public class MercadoPagoWebhookService {

    private final MercadoPagoApiService mercadoPagoApiService;
    private final MercadoPagoWebhookPaymentApplier mercadoPagoWebhookPaymentApplier;

    public MercadoPagoWebhookService(MercadoPagoApiService mercadoPagoApiService,
                                     MercadoPagoWebhookPaymentApplier mercadoPagoWebhookPaymentApplier) {
        this.mercadoPagoApiService = mercadoPagoApiService;
        this.mercadoPagoWebhookPaymentApplier = mercadoPagoWebhookPaymentApplier;
    }

    public void processarPagamentoPorId(String mercadoPagoPaymentId) {
        JsonNode payment = mercadoPagoApiService.buscarPagamento(mercadoPagoPaymentId);
        mercadoPagoWebhookPaymentApplier.aplicarWebhookPagamento(mercadoPagoPaymentId, payment);
    }
}
