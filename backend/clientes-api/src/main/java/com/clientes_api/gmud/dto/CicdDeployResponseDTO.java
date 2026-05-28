package com.clientes_api.gmud.dto;

import com.clientes_api.gmud.enums.ChangeStatus;

public record CicdDeployResponseDTO(
        Long changeId,
        ChangeStatus status,
        String message
) {
}
