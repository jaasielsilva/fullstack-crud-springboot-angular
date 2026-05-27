package com.clientes_api.gmud.dto;

import com.clientes_api.gmud.enums.DeployEnvironment;
import jakarta.validation.constraints.NotNull;

public record CicdDeployEventDTO(
        Long changeId,
        @NotNull DeployEnvironment environment,
        String title,
        String version,
        String artifact,
        String pipelineRunId,
        String commitSha,
        String comment
) {
}
