package com.clientes_api.dto.admin;

import com.clientes_api.model.enums.StatusEmpresa;
import lombok.Data;

@Data
public class StatusUpdateRequestDTO {
    private StatusEmpresa status;
}
