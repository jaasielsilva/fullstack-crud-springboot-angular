package com.clientes_api.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PedidoRequestDTO {

    @NotNull(message = "Cliente é obrigatório")
    private Long clienteId;

    @NotNull(message = "O pedido deve possuir pelo menos um item")
    private List<ItemPedidoRequestDTO> itens;
}
