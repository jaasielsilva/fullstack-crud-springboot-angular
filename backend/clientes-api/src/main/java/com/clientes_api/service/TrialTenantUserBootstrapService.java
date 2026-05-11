package com.clientes_api.service;

import com.clientes_api.config.TenantContext;
import com.clientes_api.dto.PublicTrialRegistrationDTO;
import com.clientes_api.model.Assinatura;
import com.clientes_api.model.Usuario;
import com.clientes_api.model.enums.StatusAssinatura;
import com.clientes_api.model.UsuarioRole;
import com.clientes_api.repository.UsuarioRepository;
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
        Long previous = TenantContext.getCurrentTenant();
        TenantContext.setCurrentTenant(tenantId);
        try {
            Usuario admin = new Usuario();
            admin.setLogin(dados.email().trim().toLowerCase());
            admin.setUsername(dados.nomeResponsavel().trim());
            admin.setSenha(passwordEncoder.encode(dados.senha()));
            admin.setRole(UsuarioRole.ADMIN);
            admin.setTenantId(tenantId);
            admin.setRedefinirSenha(false);
            admin = usuarioRepository.save(admin);

            Assinatura assinatura = new Assinatura();
            assinatura.setTenantId(tenantId);
            assinatura.setStatus(StatusAssinatura.TRIAL);
            assinatura.setPlano(null);
            assinaturaService.salvar(assinatura);

            return admin;
        } finally {
            if (previous != null) {
                TenantContext.setCurrentTenant(previous);
            } else {
                TenantContext.clear();
            }
        }
    }
}
