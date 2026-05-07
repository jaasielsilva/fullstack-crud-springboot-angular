package com.clientes_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordDTO(
    @NotBlank(message = "O token é obrigatório")
    String token,

    @NotBlank(message = "A nova senha é obrigatória")
    @Size(min = 3, message = "A senha deve ter pelo menos 3 caracteres")
    String novaSenha
) {
}
