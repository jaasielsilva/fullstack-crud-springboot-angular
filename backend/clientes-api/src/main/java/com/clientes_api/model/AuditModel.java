package com.clientes_api.model;

import com.clientes_api.config.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
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
     * Gerenciado manualmente via @PrePersist + TenantContext.
     * Não usa @TenantId do Hibernate para evitar conflito de validação de sessão
     * ("assigned tenant id differs from current tenant id") e erro de boot
     * ("no tenant identifier specified") quando TenantContext é null.
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
