package com.clientes_api.config;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Long> {

    /**
     * Chamado pelo Hibernate em cada operação de persistência/consulta.
     * Retorna 0L quando não há tenant no contexto (rotas públicas, jobs, bootstrap).
     * O isolamento real é feito via @PrePersist no AuditModel e queries nativas globais.
     * Não usamos @TenantId do Hibernate para evitar conflitos de validação de sessão.
     */
    @Override
    public Long resolveCurrentTenantIdentifier() {
        Long tenantId = TenantContext.getCurrentTenant();
        return (tenantId != null) ? tenantId : 0L;
    }

    /**
     * Deve retornar {@code false} para permitir que o TenantContext mude durante a requisição
     * (ex: bootstrap de trial, SubscriptionSnapshotService, DataInitializer).
     * Com {@code true}, o Hibernate revalidaria o tenant da sessão existente e lançaria exceção
     * quando o contexto muda de null/0 para o tenant real.
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
