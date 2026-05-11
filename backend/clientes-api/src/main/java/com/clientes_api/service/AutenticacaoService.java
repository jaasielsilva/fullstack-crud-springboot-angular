package com.clientes_api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.clientes_api.config.TenantContext;
import com.clientes_api.model.Usuario;
import com.clientes_api.repository.UsuarioRepository;

@Service
public class AutenticacaoService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(AutenticacaoService.class);

    @Autowired
    private UsuarioRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String loginNormalized = username == null ? null : username.trim().toLowerCase();

        logger.info("AUTH | loadUserByUsername | login={}", loginNormalized);

        Long tenantId = repository.findTenantIdByLoginOrUsernameGlobal(loginNormalized);

        if (tenantId == null) {
            logger.warn("AUTH | Tenant não encontrado para login={}", loginNormalized);
            throw new UsernameNotFoundException("Usuário não encontrado!");
        }

        logger.info("AUTH | Tenant resolvido | tenantId={}", tenantId);

        TenantContext.setCurrentTenant(tenantId);

        logger.info("AUTH | TenantContext setado | tenantId={}", TenantContext.getCurrentTenant());

        Usuario usuario = repository.findByLoginOrUsername(loginNormalized, loginNormalized);

        if (usuario == null) {
            logger.warn("AUTH | Usuário não encontrado | login={} | tenantId={}", loginNormalized, tenantId);
            throw new UsernameNotFoundException("Usuário não encontrado!");
        }

        logger.info(
                "AUTH | Usuário encontrado | id={} | login={} | tenantId={} | role={}",
                usuario.getId(),
                usuario.getLogin(),
                usuario.getTenantId(),
                usuario.getRole()
        );

        return usuario;
    }
}
