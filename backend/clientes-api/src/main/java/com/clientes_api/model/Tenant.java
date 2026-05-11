package com.clientes_api.model;

import com.clientes_api.model.enums.StatusEmpresa;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Representa a empresa (tenant) no SaaS multi-tenant.
 * Tabela física: {@code tenants} (mantida por compatibilidade com FKs existentes).
 */
@Entity
@Table(name = "tenants")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nome;

    /** Documento da empresa (CNPJ ou equivalente); coluna legada {@code cnpj}. */
    @Column(name = "cnpj")
    @JsonProperty("cnpj")
    private String documento;

    private String email;

    private String telefone;

    /**
     * Legado: mantido para compatibilidade; preferir {@link #status}.
     * TRUE quando TRIAL ou ATIVA; FALSE quando BLOQUEADA ou CANCELADA.
     */
    private Boolean ativo = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StatusEmpresa status = StatusEmpresa.TRIAL;

    private LocalDateTime trialInicio;

    private LocalDateTime trialFim;

    @Column(name = "mercado_pago_customer_id")
    private String mercadoPagoCustomerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "updated_at")
    private LocalDateTime atualizadoEm;

    public Tenant(String nome) {
        this.nome = nome;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (criadoEm == null) {
            criadoEm = now;
        }
        atualizadoEm = now;
        syncAtivoFlag();
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = LocalDateTime.now();
        syncAtivoFlag();
    }

    private void syncAtivoFlag() {
        if (status == null) {
            return;
        }
        ativo = status == StatusEmpresa.TRIAL || status == StatusEmpresa.ATIVA;
    }
}
