package com.clientes_api.config;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Long> {

    /**
     * Chamado pelo Hibernate em cada operação de persistência/consulta multi-tenant.
     * Retorna {@code null} quando não há tenant no contexto (rotas públicas, jobs, bootstrap),
     * sinalizando ao Hibernate que não deve aplicar filtro de tenant nessa operação.
     * Retornar {@code 0L} causava o erro "assigned tenant id differs from current tenant id [N != 0]"
     * porque o Hibernate fixava o tenant da sessão como 0 antes da entidade ser persistida com tenant real.
     */
    @Override
    public Long resolveCurrentTenantIdentifier() {
        return TenantContext.getCurrentTenant();
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
