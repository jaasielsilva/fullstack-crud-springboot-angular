package com.clientes_api.task.dto;

import com.clientes_api.task.enums.TaskStatus;

import java.time.LocalDateTime;

public record WorkTaskResponseDTO(
        Long id,
        String title,
        String description,
        TaskStatus status,
        String branchName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String createdBy,
        Long linkedChangeId
) {
}
