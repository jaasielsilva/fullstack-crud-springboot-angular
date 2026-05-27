package com.clientes_api.gmud.dto;

import com.clientes_api.gmud.enums.ChangeType;
import com.clientes_api.gmud.enums.DeployEnvironment;
import com.clientes_api.gmud.enums.RiskLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateChangeRequestDTO(
        @NotBlank String title,
        String description,
        @NotNull ChangeType type,
        @NotNull DeployEnvironment environment,
        @NotNull RiskLevel riskLevel,
        String impactDescription,
        String rollbackPlan,
        LocalDateTime deploymentWindowStart,
        LocalDateTime deploymentWindowEnd
) {
}
