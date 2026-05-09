package com.clientes_api.model;

import com.clientes_api.model.enums.TipoPlano;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Catálogo global de planos (sem {@code tenant_id}).
 */
@Entity
@Table(name = "planos")
@Getter
@Setter
@NoArgsConstructor
public class Plano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(length = 1024)
    private String descricao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TipoPlano tipo;

    @Column(nullable = false)
    private Boolean ativo = true;

    /** JSON com lista de recursos liberados (ex: ["CLIENTES","PRODUTOS"]). */
    @Column(name = "recursos_liberados", columnDefinition = "TEXT")
    private String recursosLiberados;
}
