package com.clientes_api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_eventos")
@Getter
@Setter
@NoArgsConstructor
public class WebhookEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String tipo;

    /** ID do recurso no Mercado Pago (ex.: payment id). */
    @Column(name = "mercado_pago_id", nullable = false, length = 64)
    private String mercadoPagoId;

    @Column(name = "payload_json", nullable = false, columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Column(nullable = false)
    private Boolean processado = false;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }
}
