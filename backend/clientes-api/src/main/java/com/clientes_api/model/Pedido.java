package com.clientes_api.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "pedidos")
public class Pedido extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Muitos pedidos para 1 cliente
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    private LocalDateTime dataPedido;

    @Enumerated(EnumType.STRING)
    private StatusPedido status;

    private Double valorTotal;

    /** Preenchido quando o pagamento é confirmado (webhook ou simulação manual). */
    private LocalDateTime dataPagamento;

    /** Última preferência Mercado Pago gerada para este pedido (checkout). */
    private String mercadoPagoPreferenceId;

    @OneToMany(
        mappedBy = "pedido",
        cascade = CascadeType.ALL,
        fetch = FetchType.LAZY,
        orphanRemoval = true
    )
    private List<ItemPedido> itens;
}