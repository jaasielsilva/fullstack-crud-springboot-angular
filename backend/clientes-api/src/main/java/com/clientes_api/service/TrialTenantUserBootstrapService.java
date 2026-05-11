package com.clientes_api.service;

import com.clientes_api.config.TenantContext;
import com.clientes_api.dto.PublicTrialRegistrationDTO;
import com.clientes_api.model.Assinatura;
import com.clientes_api.model.Usuario;
import com.clientes_api.model.enums.StatusAssinatura;
import com.clientes_api.model.UsuarioRole;
import com.clientes_api.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abre uma nova transação/sessão Hibernate já com o {@link TenantContext} do tenant recém-criado.
 * Evita o conflito em que a primeira operação JPA da requisição fixa o tenant em 0 e persistências
 * posteriores com {@code tenant_id} do novo tenant falham.
 */
@Service
public class TrialTenantUserBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(TrialTenantUserBootstrapService.class);

    private final UsuarioRepository usuarioRepository;
    private final AssinaturaService assinaturaService;
    private final PasswordEncoder passwordEncoder;

    public TrialTenantUserBootstrapService(UsuarioRepository usuarioRepository,
                                           AssinaturaService assinaturaService,
                                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.assinaturaService = assinaturaService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Usuario criarAdminEAssinaturaTrial(Long tenantId, PublicTrialRegistrationDTO dados) {
        log.info(
                "TRIAL-BOOT | Início transação REQUIRES_NEW (admin + assinatura) | tenantId={} | loginAdmin={}",
                tenantId,
                dados.email().trim().toLowerCase()
        );
        Long previous = TenantContext.getCurrentTenant();
        log.info("TRIAL-BOOT | TenantContext atual (antes do bootstrap) | tenantId={}", previous);
        TenantContext.setCurrentTenant(tenantId);
        log.info(
                "TRIAL-BOOT | TenantContext após set (Hibernate multi-tenant para persistência) | tenantId={}",
                TenantContext.getCurrentTenant()
        );
        try {
            Usuario admin = new Usuario();
            admin.setLogin(dados.email().trim().toLowerCase());
            admin.setUsername(dados.nomeResponsavel().trim());
            admin.setSenha(passwordEncoder.encode(dados.senha()));
            admin.setRole(UsuarioRole.ADMIN);
            admin.setRedefinirSenha(false);
            // tenant_id é gerenciado pelo @TenantId do Hibernate via TenantContext (já setado acima)
            admin = usuarioRepository.save(admin);
            log.info(
                    "TRIAL-BOOT | Usuário ADMIN persistido (JPA) | userId={} | tenantId={} | role={}",
                    admin.getId(),
                    tenantId,
                    admin.getRole()
            );

            Assinatura assinatura = new Assinatura();
            // tenant_id é gerenciado pelo @TenantId do Hibernate via TenantContext (já setado acima)
            assinatura.setStatus(StatusAssinatura.TRIAL);
            assinatura.setPlano(null);
            assinaturaService.salvar(assinatura);
            log.info(
                    "TRIAL-BOOT | Assinatura TRIAL persistida | tenantId={} | status={}",
                    tenantId,
                    StatusAssinatura.TRIAL
            );

            return admin;
        } finally {
            if (previous != null) {
                TenantContext.setCurrentTenant(previous);
                log.info("TRIAL-BOOT | TenantContext restaurado | tenantId={}", previous);
            } else {
                TenantContext.clear();
                log.info("TRIAL-BOOT | TenantContext limpo ao sair do bootstrap");
            }
        }
    }
}
