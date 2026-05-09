package com.clientes_api.service;

import com.clientes_api.config.TenantContext;
import com.clientes_api.dto.PublicTrialRegistrationDTO;
import com.clientes_api.exception.BusinessException;
import com.clientes_api.model.Assinatura;
import com.clientes_api.model.Tenant;
import com.clientes_api.model.Usuario;
import com.clientes_api.model.enums.StatusAssinatura;
import com.clientes_api.model.enums.StatusEmpresa;
import com.clientes_api.model.UsuarioRole;
import com.clientes_api.repository.TenantRepository;
import com.clientes_api.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Fluxo público: landing → cadastro trial 7 dias sem cartão → empresa TRIAL + ADMIN + assinatura TRIAL.
 */
@Service
public class TrialSignupService {

    private static final int DIAS_TRIAL = 7;

    private final TenantRepository tenantRepository;
    private final UsuarioRepository usuarioRepository;
    private final AssinaturaService assinaturaService;
    private final PasswordEncoder passwordEncoder;

    public TrialSignupService(TenantRepository tenantRepository,
                              UsuarioRepository usuarioRepository,
                              AssinaturaService assinaturaService,
                              PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.usuarioRepository = usuarioRepository;
        this.assinaturaService = assinaturaService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario cadastrarTrial(PublicTrialRegistrationDTO dados) {
        if (usuarioRepository.findByLogin(dados.email()) != null) {
            throw new BusinessException("Já existe usuário com este e-mail.");
        }
        if (tenantRepository.existsByNomeIgnoreCase(dados.nomeEmpresa().trim())) {
            throw new BusinessException("Já existe empresa cadastrada com este nome.");
        }
        if (tenantRepository.existsByEmailIgnoreCase(dados.email().trim())) {
            throw new BusinessException("Este e-mail já está vinculado a outra empresa.");
        }

        LocalDateTime inicio = LocalDateTime.now();
        LocalDateTime fim = inicio.plusDays(DIAS_TRIAL);

        Tenant empresa = new Tenant();
        empresa.setNome(dados.nomeEmpresa().trim());
        empresa.setEmail(dados.email().trim().toLowerCase());
        empresa.setTelefone(dados.telefone().trim());
        empresa.setStatus(StatusEmpresa.TRIAL);
        empresa.setTrialInicio(inicio);
        empresa.setTrialFim(fim);
        empresa = tenantRepository.save(empresa);

        try {
            TenantContext.setCurrentTenant(empresa.getId());

            Usuario admin = new Usuario();
            admin.setLogin(dados.email().trim().toLowerCase());
            admin.setUsername(dados.nomeResponsavel().trim());
            admin.setSenha(passwordEncoder.encode(dados.senha()));
            admin.setRole(UsuarioRole.ADMIN);
            admin.setTenantId(empresa.getId());
            admin.setRedefinirSenha(false);
            admin = usuarioRepository.save(admin);

            Assinatura assinatura = new Assinatura();
            assinatura.setTenantId(empresa.getId());
            assinatura.setStatus(StatusAssinatura.TRIAL);
            assinatura.setPlano(null);
            assinaturaService.salvar(assinatura);

            return admin;
        } finally {
            TenantContext.clear();
        }
    }
}
