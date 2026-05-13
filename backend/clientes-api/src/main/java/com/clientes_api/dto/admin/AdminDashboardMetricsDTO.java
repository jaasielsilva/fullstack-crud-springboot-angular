package com.clientes_api.dto.admin;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminDashboardMetricsDTO {
    private long totalAssinantesAtivos;
    private BigDecimal mrrEstimado;
    private long empresasEmTrial;
    private long pagamentosPendentes;
}
