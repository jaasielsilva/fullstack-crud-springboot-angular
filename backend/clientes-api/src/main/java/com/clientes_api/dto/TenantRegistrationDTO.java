package com.clientes_api.dto;

public record TenantRegistrationDTO(
    String nomeEmpresa,
    String cnpj,
    String emailAdmin,
    String usernameAdmin,
    String senhaAdmin
) {}
