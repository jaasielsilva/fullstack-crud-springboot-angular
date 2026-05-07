package com.clientes_api.config;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Long> {

    private static final Logger logger = LoggerFactory.getLogger(TenantIdentifierResolver.class);

    @Override
    public Long resolveCurrentTenantIdentifier() {
        Long tenantId = TenantContext.getCurrentTenant();
        
        if (tenantId == null) {
            // Em caso de tarefas em background ou inicialização sem contexto
            return 0L; 
        }
        
        return tenantId;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
