package com.clientes_api.model;

import com.clientes_api.config.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Data;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
public abstract class AuditModel {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    /**
     * Coluna de isolamento multi-tenant.
     * Preenchimento em insert: {@link #setTenantIdOnCreate()} + {@link TenantContext}.
     * Filtro global Hibernate {@code tenantFilter} (definido em {@link AuditModel}, anotado nas entidades)
     * restringe leituras quando o tenant está no contexto e a transação JPA é aberta.
     */
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @PrePersist
    public void setTenantIdOnCreate() {
        if (this.tenantId == null) {
            Long ctx = TenantContext.getCurrentTenant();
            this.tenantId = (ctx != null) ? ctx : 0L;
        }
    }
}
