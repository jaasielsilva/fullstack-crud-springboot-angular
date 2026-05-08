package com.clientes_api.dto;

import lombok.Data;

@Data
public class ItemPedidoResponseDTO {
    private Long produtoId;
    private String produto;
    private Integer quantidade;
    private Double valorUnitario;
    private Double subtotal;
}
