package com.clientes_api.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class PedidoResponseDTO {
    private Long id;
    private Long clienteId;
    private String cliente;
    private String status;
    private Double valorTotal;
    private LocalDateTime dataPedido;
    private List<ItemPedidoResponseDTO> itens;
}
