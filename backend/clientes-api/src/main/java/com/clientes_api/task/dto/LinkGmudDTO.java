package com.clientes_api.task.dto;

import jakarta.validation.constraints.NotNull;

public record LinkGmudDTO(
        @NotNull Long changeId
) {
}
