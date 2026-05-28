package com.clientes_api.task.service;

import com.clientes_api.task.dto.WorkTaskResponseDTO;
import com.clientes_api.task.model.WorkTask;

final class WorkTaskMapper {

    private WorkTaskMapper() {
    }

    static WorkTaskResponseDTO toResponse(WorkTask entity) {
        return new WorkTaskResponseDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getBranchName(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getCreatedBy(),
                entity.getLinkedChangeId());
    }
}
