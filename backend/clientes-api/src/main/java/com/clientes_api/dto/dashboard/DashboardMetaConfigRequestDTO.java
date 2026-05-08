package com.clientes_api.dto.dashboard;

import lombok.Data;

@Data
public class DashboardMetaConfigRequestDTO {
    private String escopo; // USER | ROLE | GLOBAL
    private String alvo;   // role ou login opcional
    private Double metaReceita;
}
