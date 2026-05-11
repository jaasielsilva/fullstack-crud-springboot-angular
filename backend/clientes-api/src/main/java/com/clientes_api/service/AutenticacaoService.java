package com.clientes_api.service;

import com.clientes_api.model.Usuario;
import com.clientes_api.repository.UsuarioRepository;
import com.clientes_api.config.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AutenticacaoService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(AutenticacaoService.class);

    @Autowired
    private UsuarioRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String loginNormalized = username == null ? null : username.trim().toLowerCase();
        logger.info("AUTH | UserDetailsService.loadUserByUsername | loginRecebido={}", username);

        // Em rotas sem JWT (ex: /api/auth/login), o TenantContext pode estar vazio.
        // Como o Hibernate valida @TenantId, precisamos setar o tenant antes de materializar Usuario.
        Long previousTenant = TenantContext.getCurrentTenant();
        logger.info("AUTH | TenantContext atual (antes da resolução multi-tenant) | tenantId={}", previousTenant);

        Long tenantId = repository.findTenantIdByLoginOrUsernameGlobal(loginNormalized);
        if (tenantId == null) {
            logger.warn(
                    "AUTH | Tenant não resolvido (lookup global) | loginNormalizado={} | motivo=usuario_inexistente_ou_sem_tenant",
                    loginNormalized
            );
            throw new UsernameNotFoundException("Usuário não encontrado!");
        }

        logger.info("AUTH | Tenant encontrado (lookup global) | tenantId={} | loginNormalizado={}", tenantId, loginNormalized);

        TenantContext.setCurrentTenant(tenantId);
        logger.info("AUTH | TenantContext após set ( Hibernate @TenantId / filtros) | tenantId={}", TenantContext.getCurrentTenant());

        Usuario usuario;
        try {
            // Agora a consulta respeita o tenant (multi-tenancy) e evita conflito com @TenantId
            usuario = repository.findByLoginOrUsername(loginNormalized, loginNormalized);
        } finally {
            // Restaura o contexto original para não vazar tenant entre requisições.
            if (previousTenant != null) {
                TenantContext.setCurrentTenant(previousTenant);
                logger.info("AUTH | TenantContext restaurado após materialização Usuario | tenantId={}", previousTenant);
            } else {
                TenantContext.clear();
                logger.info("AUTH | TenantContext limpo após materialização Usuario (sem tenant prévio na thread)");
            }
        }

        if (usuario == null) {
            logger.warn(
                    "AUTH | Usuário não encontrado após escopo tenant | loginNormalizado={} | tenantIdEsperado={}",
                    loginNormalized,
                    tenantId
            );
            throw new UsernameNotFoundException("Usuário não encontrado!");
        }

        logger.info(
                "AUTH | Usuário encontrado | userId={} | tenantId={} | login={} | role={}",
                usuario.getId(),
                usuario.getTenantId(),
                usuario.getLogin(),
                usuario.getRole()
        );

        if (usuario.getTenant() != null && !usuario.getTenant().getAtivo()) {
            logger.warn(
                    "AUTH | Login bloqueado | empresa_inativa | tenantId={} | nomeEmpresa={}",
                    usuario.getTenantId(),
                    usuario.getTenant().getNome()
            );
            throw new RuntimeException("Sua empresa está inativa. Entre em contato com o suporte.");
        }

        logger.info("AUTH | Pré-checagem de senha (PasswordEncoder) | userId={} | tenantId={}", usuario.getId(), usuario.getTenantId());
        return usuario;
    }
}
