package com.clientes_api.service;

import com.clientes_api.config.TenantContext;
import com.clientes_api.model.Assinatura;
import com.clientes_api.model.Pagamento;
import com.clientes_api.model.Tenant;
import com.clientes_api.model.enums.StatusAssinatura;
import com.clientes_api.model.enums.StatusEmpresa;
import com.clientes_api.model.enums.StatusPagamento;
import com.clientes_api.util.MercadoPagoExternalReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Processa webhooks de pagamento do Mercado Pago com idempotência e validação de {@code external_reference}.
 * Libera acesso somente após status {@code approved} confirmado na API (não pelo redirect do frontend).
 */
@Service
public class MercadoPagoWebhookService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookService.class);

    private final MercadoPagoApiService mercadoPagoApiService;
    private final PagamentoService pagamentoService;
    private final AssinaturaService assinaturaService;
    private final EmpresaService empresaService;

    public MercadoPagoWebhookService(MercadoPagoApiService mercadoPagoApiService,
                                     PagamentoService pagamentoService,
                                     AssinaturaService assinaturaService,
                                     EmpresaService empresaService) {
        this.mercadoPagoApiService = mercadoPagoApiService;
        this.pagamentoService = pagamentoService;
        this.assinaturaService = assinaturaService;
        this.empresaService = empresaService;
    }

    @Transactional
    public void processarPagamentoPorId(String mercadoPagoPaymentId) {
        JsonNode payment = mercadoPagoApiService.buscarPagamento(mercadoPagoPaymentId);
        String rawPayment = payment.toString();

        String mpStatus = payment.path("status").asText("unknown");
        StatusPagamento statusPagamento = mapearStatusPagamento(mpStatus);

        if ("approved".equalsIgnoreCase(mpStatus)
                && pagamentoService.jaAprovado(mercadoPagoPaymentId)) {
            log.info("Webhook idempotente: pagamento {} já estava APPROVED.", mercadoPagoPaymentId);
            return;
        }

        String externalRef = payment.path("external_reference").asText(null);
        var parsedOpt = MercadoPagoExternalReference.parse(externalRef);
        if (parsedOpt.isEmpty()) {
            log.warn("external_reference inválido no pagamento MP {}: {}", mercadoPagoPaymentId, externalRef);
            throw new IllegalArgumentException("external_reference inválido");
        }
        MercadoPagoExternalReference parsed = parsedOpt.get();

        TenantContext.setCurrentTenant(parsed.empresaId());
        try {
            Assinatura assinatura = assinaturaService.buscarPorIdETenantOuErro(parsed.assinaturaId(), parsed.empresaId());
            if (assinatura.getPlano() == null || !assinatura.getPlano().getId().equals(parsed.planoId())) {
                throw new IllegalArgumentException("external_reference não confere com assinatura/plano persistidos");
            }

            Tenant empresa = empresaService.buscarPorIdOuErro(parsed.empresaId());

            Pagamento pagamento = pagamentoService.buscarPorMercadoPagoId(mercadoPagoPaymentId)
                    .orElseGet(Pagamento::new);
            pagamento.setTenantId(parsed.empresaId());
            pagamento.setAssinatura(assinatura);
            pagamento.setMercadoPagoPaymentId(mercadoPagoPaymentId);
            pagamento.setMercadoPagoPreferenceId(payment.path("preference_id").asText(null));
            pagamento.setStatus(statusPagamento);
            pagamento.setStatusDetail(payment.path("status_detail").asText(null));
            if (payment.hasNonNull("transaction_amount")) {
                pagamento.setValor(new BigDecimal(payment.path("transaction_amount").asText()));
            }
            pagamento.setMetodoPagamento(payment.path("payment_method_id").asText(null));
            pagamento.setExternalReference(externalRef);
            pagamento.setPayloadJson(rawPayment);
            pagamentoService.salvar(pagamento);

            LocalDateTime agora = LocalDateTime.now();
            aplicarRegraNegocio(mpStatus, assinatura, empresa, agora, mercadoPagoPaymentId);

            assinaturaService.salvar(assinatura);
            empresaService.salvar(empresa);
        } finally {
            TenantContext.clear();
        }
    }

    private void aplicarRegraNegocio(String mpStatus, Assinatura assinatura, Tenant empresa, LocalDateTime agora,
                                     String mercadoPagoPaymentId) {
        switch (mpStatus == null ? "" : mpStatus.toLowerCase()) {
            case "approved" -> {
                assinatura.setStatus(StatusAssinatura.ATIVA);
                assinatura.setDataInicio(agora);
                assinatura.setDataFim(agora.plusDays(30));
                assinatura.setMercadoPagoPaymentId(mercadoPagoPaymentId);
                empresa.setStatus(StatusEmpresa.ATIVA);
            }
            case "pending", "in_process" -> assinatura.setStatus(StatusAssinatura.PENDENTE);
            case "rejected", "cancelled" -> tratarPagamentoTerminal(assinatura, empresa, StatusAssinatura.CANCELADA);
            case "refunded", "charged_back" -> tratarPagamentoTerminal(assinatura, empresa, StatusAssinatura.EXPIRADA);
            default -> log.warn("Status Mercado Pago não mapeado para regra de negócio: {}", mpStatus);
        }
    }

    /**
     * Pagamentos definitivamente negativos não alteram empresas já {@link StatusEmpresa#ATIVA}
     * (ex.: tentativa de nova cobrança recusada).
     */
    private void tratarPagamentoTerminal(Assinatura assinatura, Tenant empresa, StatusAssinatura novoStatusAssinatura) {
        assinatura.setStatus(novoStatusAssinatura);
        if (empresa.getStatus() != StatusEmpresa.ATIVA && empresa.getStatus() != StatusEmpresa.CANCELADA) {
            empresa.setStatus(StatusEmpresa.BLOQUEADA);
        }
    }

    private StatusPagamento mapearStatusPagamento(String mpStatus) {
        if (mpStatus == null) {
            return StatusPagamento.UNKNOWN;
        }
        return switch (mpStatus.toLowerCase()) {
            case "approved" -> StatusPagamento.APPROVED;
            case "pending" -> StatusPagamento.PENDING;
            case "in_process" -> StatusPagamento.IN_PROCESS;
            case "rejected" -> StatusPagamento.REJECTED;
            case "cancelled" -> StatusPagamento.CANCELLED;
            case "refunded" -> StatusPagamento.REFUNDED;
            case "charged_back" -> StatusPagamento.CHARGED_BACK;
            default -> StatusPagamento.UNKNOWN;
        };
    }
}
