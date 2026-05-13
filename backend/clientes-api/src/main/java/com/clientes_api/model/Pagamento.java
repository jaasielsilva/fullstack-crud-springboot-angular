package com.clientes_api.model;

import com.clientes_api.model.enums.StatusPagamento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "pagamentos")
@Getter
@Setter
@NoArgsConstructor
public class Pagamento extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assinatura_id", nullable = true)
    private Assinatura assinatura;

    /** Pagamento vinculado a pedido B2B (checkout de pedido). Mutuamente exclusivo com {@link #assinatura} na aplicação. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = true)
    private Pedido pedido;

    @Column(name = "mercado_pago_payment_id", unique = true, length = 64)
    private String mercadoPagoPaymentId;

    @Column(name = "mercado_pago_preference_id", length = 64)
    private String mercadoPagoPreferenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StatusPagamento status = StatusPagamento.UNKNOWN;

    @Column(name = "status_detail", length = 255)
    private String statusDetail;

    @Column(precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "metodo_pagamento", length = 64)
    private String metodoPagamento;

    @Column(name = "external_reference", length = 512)
    private String externalReference;

    @Column(name = "payload_json", columnDefinition = "LONGTEXT")
    private String payloadJson;
}
