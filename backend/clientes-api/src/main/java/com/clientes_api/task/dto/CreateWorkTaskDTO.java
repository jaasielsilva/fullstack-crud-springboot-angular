package com.clientes_api.task.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkTaskDTO(
        @NotBlank String title,
        String description
) {
}
