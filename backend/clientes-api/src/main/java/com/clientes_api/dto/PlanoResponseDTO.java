package com.clientes_api.dto;

import java.math.BigDecimal;

public record PlanoResponseDTO(Long id, String nome, String descricao, BigDecimal valor, String tipo) {}
