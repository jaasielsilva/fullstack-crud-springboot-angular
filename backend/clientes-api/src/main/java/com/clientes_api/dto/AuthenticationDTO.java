package com.clientes_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthenticationDTO(
        @NotBlank(message = "O email é obrigatório")
        @Email(message = "Formato de email inválido")
        String login,

        @NotBlank(message = "A senha é obrigatória")
        String senha
) {
}
