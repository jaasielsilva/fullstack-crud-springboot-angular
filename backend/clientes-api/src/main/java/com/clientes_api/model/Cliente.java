package com.clientes_api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Filter;

@Entity
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Data
@Table(name = "clientes")
public class Cliente extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private String email;

    private String telefone;

    private Boolean ativo;
}