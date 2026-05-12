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
    /** Data em que o pagamento foi confirmado (webhook ou simulação). */
    private LocalDateTime dataPagamento;
    /** Última preferência Mercado Pago gerada para checkout do pedido. */
    private String mercadoPagoPreferenceId;
    private List<ItemPedidoResponseDTO> itens;
}
