package com.clientes_api.gmud.dto;

import com.clientes_api.gmud.enums.ChangeStatus;

import java.time.LocalDateTime;

public record ChangeLogResponseDTO(
        Long id,
        ChangeStatus fromStatus,
        ChangeStatus toStatus,
        String changedBy,
        LocalDateTime changedAt,
        String comment
) {
}
