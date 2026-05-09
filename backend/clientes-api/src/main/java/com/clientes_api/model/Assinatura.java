package com.clientes_api.model;

import com.clientes_api.model.enums.StatusAssinatura;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "assinaturas")
@Getter
@Setter
@NoArgsConstructor
public class Assinatura extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Opcional durante {@link StatusAssinatura#TRIAL} antes da escolha do plano pago. */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "plano_id")
    private Plano plano;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StatusAssinatura status = StatusAssinatura.TRIAL;

    private LocalDateTime dataInicio;

    private LocalDateTime dataFim;

    @Column(name = "mercado_pago_payment_id")
    private String mercadoPagoPaymentId;

    @Column(name = "mercado_pago_preference_id")
    private String mercadoPagoPreferenceId;

    @Column(name = "external_reference", length = 512)
    private String externalReference;
}
