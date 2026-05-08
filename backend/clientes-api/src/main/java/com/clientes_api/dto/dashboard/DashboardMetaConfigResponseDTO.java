package com.clientes_api.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardMetaConfigResponseDTO {
    private String escopo;
    private String alvo;
    private Double metaReceita;
}
