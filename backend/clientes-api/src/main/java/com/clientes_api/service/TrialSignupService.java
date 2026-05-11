package com.clientes_api.service;

import com.clientes_api.config.TenantContext;
import com.clientes_api.dto.PublicTrialRegistrationDTO;
import com.clientes_api.exception.BusinessException;
import com.clientes_api.model.Assinatura;
import com.clientes_api.model.Tenant;
import com.clientes_api.model.Usuario;
import com.clientes_api.model.UsuarioRole;
import com.clientes_api.model.enums.StatusAssinatura;
import com.clientes_api.model.enums.StatusEmpresa;
import com.clientes_api.repository.AssinaturaRepository;
import com.clientes_api.repository.TenantRepository;
import com.clientes_api.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Fluxo público: landing → cadastro trial 7 dias sem cartão → empresa TRIAL + ADMIN + assinatura TRIAL.
 * Tudo em uma única transação para evitar deadlock (Lock wait timeout) que ocorria
 * com REQUIRES_NEW abrindo transação paralela enquanto a principal segurava locks.
 */
@Service
public class TrialSignupService {

    private static final Logger log = LoggerFactory.getLogger(TrialSignupService.class);
    private static final int DIAS_TRIAL = 7;

    private final TenantRepository tenantRepository;
    private final UsuarioRepository usuarioRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final PasswordEncoder passwordEncoder;

    public TrialSignupService(TenantRepository tenantRepository,
                              UsuarioRepository usuarioRepository,
                              AssinaturaRepository assinaturaRepository,
                              PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.usuarioRepository = usuarioRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario cadastrarTrial(PublicTrialRegistrationDTO dados) {
        String email = dados.email().trim().toLowerCase();
        String nomeEmpresa = dados.nomeEmpresa().trim();

        log.info("TRIAL | Início cadastro trial | empresaNome={} | email={} | diasTrial={}", nomeEmpresa, email, DIAS_TRIAL);

        if (usuarioRepository.countByLoginGlobal(email) > 0) {
            log.warn("TRIAL | Cadastro recusado | motivo=email_ja_cadastrado | email={}", email);
            throw new BusinessException("Já existe usuário com este e-mail.");
        }
        if (tenantRepository.existsByNomeIgnoreCase(nomeEmpresa)) {
            log.warn("TRIAL | Cadastro recusado | motivo=nome_empresa_duplicado | empresaNome={}", nomeEmpresa);
            throw new BusinessException("Já existe empresa cadastrada com este nome.");
        }
        if (tenantRepository.existsByEmailIgnoreCase(email)) {
            log.warn("TRIAL | Cadastro recusado | motivo=email_vinculado_outra_empresa | email={}", email);
            throw new BusinessException("Este e-mail já está vinculado a outra empresa.");
        }

        // 1. Cria o tenant
        LocalDateTime inicio = LocalDateTime.now();
        Tenant empresa = new Tenant();
        empresa.setNome(nomeEmpresa);
        empresa.setEmail(email);
        empresa.setTelefone(dados.telefone().trim());
        empresa.setStatus(StatusEmpresa.TRIAL);
        empresa.setTrialInicio(inicio);
        empresa.setTrialFim(inicio.plusDays(DIAS_TRIAL));
        empresa = tenantRepository.save(empresa);
        log.info("TRIAL | Tenant persistido | tenantId={} | trialFim={}", empresa.getId(), empresa.getTrialFim());

        // 2. Seta o TenantContext para que @PrePersist preencha tenant_id corretamente
        TenantContext.setCurrentTenant(empresa.getId());
        log.info("TRIAL | TenantContext setado | tenantId={}", empresa.getId());

        try {
            // 3. Cria o usuário ADMIN
            Usuario admin = new Usuario();
            admin.setLogin(email);
            admin.setUsername(dados.nomeResponsavel().trim());
            admin.setSenha(passwordEncoder.encode(dados.senha()));
            admin.setRole(UsuarioRole.ADMIN);
            admin.setTenantId(empresa.getId());
            admin.setRedefinirSenha(false);
            admin = usuarioRepository.save(admin);
            log.info("TRIAL | Admin persistido | userId={} | tenantId={}", admin.getId(), empresa.getId());

            // 4. Cria a assinatura TRIAL
            Assinatura assinatura = new Assinatura();
            assinatura.setTenantId(empresa.getId());
            assinatura.setStatus(StatusAssinatura.TRIAL);
            assinatura.setPlano(null);
            assinaturaRepository.save(assinatura);
            log.info("TRIAL | Assinatura TRIAL persistida | tenantId={}", empresa.getId());

            log.info("TRIAL | Cadastro trial concluído | tenantId={} | adminUserId={}", empresa.getId(), admin.getId());
            return admin;
        } finally {
            TenantContext.clear();
        }
    }
}
