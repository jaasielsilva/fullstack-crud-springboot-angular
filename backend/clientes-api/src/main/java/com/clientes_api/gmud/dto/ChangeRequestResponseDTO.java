package com.clientes_api.gmud.dto;

import com.clientes_api.gmud.enums.ChangeStatus;
import com.clientes_api.gmud.enums.ChangeType;
import com.clientes_api.gmud.enums.DeployEnvironment;
import com.clientes_api.gmud.enums.RiskLevel;

import java.time.LocalDateTime;
import java.util.List;

public record ChangeRequestResponseDTO(
        Long id,
        String title,
        String description,
        ChangeType type,
        ChangeStatus status,
        DeployEnvironment environment,
        RiskLevel riskLevel,
        String impactDescription,
        String rollbackPlan,
        LocalDateTime deploymentWindowStart,
        LocalDateTime deploymentWindowEnd,
        LocalDateTime createdAt,
        LocalDateTime approvedAt,
        LocalDateTime deployedAt,
        LocalDateTime rolledBackAt,
        String createdBy,
        String version,
        String artifact,
        String pipelineRunId,
        String commitSha,
        Long taskId,
        List<ChangeLogResponseDTO> logs
) {
}
