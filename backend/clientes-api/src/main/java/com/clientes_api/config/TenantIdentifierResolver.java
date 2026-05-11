package com.clientes_api.config;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Long> {

    /**
     * Chamado pelo Hibernate em cada operação de persistência/consulta multi-tenant.
     * Com {@code tenantId == null} retorna {@code 0L} (rotas públicas, jobs, bootstrap).
     * O rastreio SLF4J do tenant fica em {@code AutenticacaoService}, {@code SecurityFilter},
     * fluxos de trial e {@code SubscriptionSnapshotService} (evita ruído por volume de chamadas).
     */
    @Override
    public Long resolveCurrentTenantIdentifier() {
        Long tenantId = TenantContext.getCurrentTenant();

        if (tenantId == null) {
            return 0L;
        }

        return tenantId;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
