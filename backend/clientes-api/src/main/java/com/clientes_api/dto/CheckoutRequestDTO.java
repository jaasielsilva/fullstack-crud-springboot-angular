package com.clientes_api.dto;

import jakarta.validation.constraints.NotNull;

public record CheckoutRequestDTO(
        @NotNull Long empresaId,
        @NotNull Long planoId
) {}
