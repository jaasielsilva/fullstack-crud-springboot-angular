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
        logger.info("Tentativa de login para o usuário: {}", username);

        String loginNormalized = username == null ? null : username.trim().toLowerCase();

        // Em rotas sem JWT (ex: /api/auth/login), o TenantContext pode estar vazio.
        // Como o Hibernate valida @TenantId, precisamos setar o tenant antes de materializar Usuario.
        Long previousTenant = TenantContext.getCurrentTenant();
        Long tenantId = repository.findTenantIdByLoginOrUsernameGlobal(loginNormalized);
        if (tenantId == null) {
            logger.warn("Usuário não encontrado no banco de dados (Busca Global): {}", username);
            throw new UsernameNotFoundException("Usuário não encontrado!");
        }

        TenantContext.setCurrentTenant(tenantId);
        Usuario usuario;
        try {
            // Agora a consulta respeita o tenant (multi-tenancy) e evita conflito com @TenantId
            usuario = repository.findByLoginOrUsername(loginNormalized, loginNormalized);
        } finally {
            // Restaura o contexto original para não vazar tenant entre requisições.
            if (previousTenant != null) {
                TenantContext.setCurrentTenant(previousTenant);
            } else {
                TenantContext.clear();
            }
        }

        if (usuario == null) {
            logger.warn("Usuário não encontrado no banco de dados (Login/Username com tenant): {}", username);
            throw new UsernameNotFoundException("Usuário não encontrado!");
        }

        // 2. Verifica se a empresa (Tenant) está ativa
        // Agora o Java reconhece o getTenant() pois a variável é do tipo Usuario
        if (usuario.getTenant() != null && !usuario.getTenant().getAtivo()) {
            logger.warn("Tentativa de login em empresa inativa: {}", usuario.getTenant().getNome());
            throw new RuntimeException("Sua empresa está inativa. Entre em contato com o suporte.");
        }
        
        logger.info("Usuário encontrado e empresa ativa. Verificando credenciais...");
        return usuario;
    }
}
