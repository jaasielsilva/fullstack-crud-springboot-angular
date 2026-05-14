package com.clientes_api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TenantSecurityLogger {

    private static final Logger log = LoggerFactory.getLogger("SECURITY_TENANT");

    public void logMissingTenantContext(String contexto) {
        log.warn("SECURITY_TENANT | tenant_context_vazio | contexto={}", contexto);
    }

    public void logCrossTenantAccessDenied(long tenantAutenticado, long tenantRecurso, String operacao) {
        log.warn(
                "SECURITY_TENANT | acesso_cruzado_negado | operacao={} | tenantAutenticado={} | tenantRecurso={}",
                operacao,
                tenantAutenticado,
                tenantRecurso
        );
    }
}
