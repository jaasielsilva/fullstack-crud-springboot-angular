package com.clientes_api.dto.admin;

import com.clientes_api.model.enums.StatusAssinatura;
import com.clientes_api.model.enums.StatusEmpresa;
import com.clientes_api.model.enums.StatusPagamento;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AssinanteAdminDTO {
    private Long empresaId;
    private String nomeEmpresa;
    private String documento;
    private String email;
    private StatusEmpresa statusEmpresa;
    
    private String planoNome;
    private StatusAssinatura statusAssinatura;
    private LocalDateTime dataInicio;
    private LocalDateTime dataVencimento;
    
    private StatusPagamento ultimoPagamentoStatus;
    private BigDecimal valorUltimoPagamento;
    private LocalDateTime ultimoPagamentoData;
}
