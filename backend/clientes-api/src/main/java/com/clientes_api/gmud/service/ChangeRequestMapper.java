package com.clientes_api.gmud.service;

import com.clientes_api.gmud.dto.ChangeLogResponseDTO;
import com.clientes_api.gmud.dto.ChangeRequestResponseDTO;
import com.clientes_api.gmud.model.ChangeLog;
import com.clientes_api.gmud.model.ChangeRequest;

import java.util.Collections;
import java.util.List;

final class ChangeRequestMapper {

    private ChangeRequestMapper() {
    }

    static ChangeRequestResponseDTO toResponse(ChangeRequest entity, List<ChangeLog> logs) {
        List<ChangeLogResponseDTO> logDtos = logs == null ? Collections.emptyList() : logs.stream()
                .map(ChangeRequestMapper::toLogResponse)
                .toList();
        return new ChangeRequestResponseDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getType(),
                entity.getStatus(),
                entity.getEnvironment(),
                entity.getRiskLevel(),
                entity.getImpactDescription(),
                entity.getRollbackPlan(),
                entity.getDeploymentWindowStart(),
                entity.getDeploymentWindowEnd(),
                entity.getCreatedAt(),
                entity.getApprovedAt(),
                entity.getDeployedAt(),
                entity.getRolledBackAt(),
                entity.getCreatedBy(),
                entity.getVersion(),
                entity.getArtifact(),
                entity.getPipelineRunId(),
                entity.getCommitSha(),
                logDtos
        );
    }

    static ChangeLogResponseDTO toLogResponse(ChangeLog log) {
        return new ChangeLogResponseDTO(
                log.getId(),
                log.getFromStatus(),
                log.getToStatus(),
                log.getChangedBy(),
                log.getChangedAt(),
                log.getComment()
        );
    }
}
