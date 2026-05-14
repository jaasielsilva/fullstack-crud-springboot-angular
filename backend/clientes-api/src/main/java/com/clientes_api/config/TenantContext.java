package com.clientes_api.config;

import com.clientes_api.exception.BusinessException;

public class TenantContext {
    private static final ThreadLocal<Long> currentTenant = new ThreadLocal<>();

    public static void setCurrentTenant(Long tenantId) {
        currentTenant.set(tenantId);
    }

    public static Long getCurrentTenant() {
        return currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();
    }

    /**
     * Tenant obrigatório para operações de negócio multi-tenant (nunca aceitar tenant vindo do body).
     */
    public static long requireTenantId() {
        Long t = getCurrentTenant();
        if (t == null || t == 0L) {
            throw new BusinessException("Contexto de empresa não disponível.");
        }
        return t;
    }
}
