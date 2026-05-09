package com.clientes_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicTrialRegistrationDTO(
        @NotBlank(message = "Nome da empresa é obrigatório")
        @Size(max = 255)
        String nomeEmpresa,

        @NotBlank(message = "Nome do responsável é obrigatório")
        @Size(max = 255)
        String nomeResponsavel,

        @NotBlank @Email String email,

        @NotBlank(message = "Telefone é obrigatório")
        @Size(max = 40)
        String telefone,

        @NotBlank @Size(min = 6, max = 120)
        String senha
) {}
