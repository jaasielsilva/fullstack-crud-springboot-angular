package com.clientes_api.gmud.support;

import com.clientes_api.exception.BusinessException;
import com.clientes_api.model.Usuario;
import com.clientes_api.model.UsuarioRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SuperAdminSupport {

    private static final long PLATFORM_TENANT_ID = 1L;

    public void assertSuperAdmin() {
        if (!isSuperAdmin()) {
            throw new BusinessException("Acesso restrito ao super administrador da plataforma.");
        }
    }

    public boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Usuario usuario)) {
            return false;
        }
        return usuario.getRole() == UsuarioRole.ADMIN
                && usuario.getTenantId() != null
                && usuario.getTenantId().equals(PLATFORM_TENANT_ID);
    }

    public String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return "anonymous";
        }
        if (auth.getPrincipal() instanceof Usuario usuario) {
            return usuario.getLogin();
        }
        return auth.getName();
    }
}
