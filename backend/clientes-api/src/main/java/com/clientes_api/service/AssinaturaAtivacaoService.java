package com.clientes_api.service;

import com.clientes_api.model.Assinatura;
import com.clientes_api.model.Tenant;
import com.clientes_api.model.enums.StatusAssinatura;
import com.clientes_api.model.enums.StatusEmpresa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Ativação de assinatura e empresa após pagamento aprovado (Mercado Pago, Abacate Pay, etc.).
 */
@Service
public class AssinaturaAtivacaoService {

    private static final Logger log = LoggerFactory.getLogger(AssinaturaAtivacaoService.class);

    public void aplicarResultadoPagamentoMercadoPago(String mpStatus, Assinatura assinatura, Tenant empresa,
                                                     LocalDateTime agora, String mercadoPagoPaymentId) {
        switch (mpStatus == null ? "" : mpStatus.toLowerCase()) {
            case "approved" -> ativarAssinaturaEEmpresa(assinatura, empresa, agora, mercadoPagoPaymentId);
            case "pending", "in_process" -> assinatura.setStatus(StatusAssinatura.PENDENTE);
            case "rejected", "cancelled" -> tratarPagamentoTerminal(assinatura, empresa, StatusAssinatura.CANCELADA);
            case "refunded", "charged_back" -> tratarPagamentoTerminal(assinatura, empresa, StatusAssinatura.EXPIRADA);
            default -> log.warn("Status Mercado Pago não mapeado para regra de negócio: {}", mpStatus);
        }
    }

    /**
     * Pagamento concluído com sucesso (ex.: checkout Abacate em {@code PAID}).
     */
    public void ativarAssinaturaEEmpresa(Assinatura assinatura, Tenant empresa, LocalDateTime agora,
                                        String mercadoPagoPaymentIdIfPresent) {
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setDataInicio(agora);
        assinatura.setDataFim(agora.plusDays(30));
        if (mercadoPagoPaymentIdIfPresent != null && !mercadoPagoPaymentIdIfPresent.isBlank()) {
            assinatura.setMercadoPagoPaymentId(mercadoPagoPaymentIdIfPresent);
        }
        empresa.setStatus(StatusEmpresa.ATIVA);
        empresa.setTrialInicio(null);
        empresa.setTrialFim(null);
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
}
