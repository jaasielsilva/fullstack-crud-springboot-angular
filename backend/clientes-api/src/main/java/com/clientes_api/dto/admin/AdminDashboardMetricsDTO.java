package com.clientes_api.dto.admin;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminDashboardMetricsDTO {
    private long totalAssinantesAtivos;
    private BigDecimal mrrEstimado;
    private long empresasEmTrial;
    private long pagamentosPendentes;
    /** Assinaturas ATIVA com vencimento do período pago entre hoje e +7 dias (renovação). */
    private long renovacaoUrgente7Dias;
}
