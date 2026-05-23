package com.clientes_api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Filter;

@Entity
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Data
@Table(name = "dashboard_meta_configs")
@EqualsAndHashCode(callSuper = false)
public class DashboardMetaConfig extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String login;

    private String role;

    private Double metaReceita;
}
