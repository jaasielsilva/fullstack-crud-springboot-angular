package com.clientes_api.service;

import com.clientes_api.dto.PublicTrialRegistrationDTO;
import com.clientes_api.exception.BusinessException;
import com.clientes_api.model.Tenant;
import com.clientes_api.model.Usuario;
import com.clientes_api.model.enums.StatusEmpresa;
import com.clientes_api.repository.TenantRepository;
import com.clientes_api.repository.UsuarioRepository;
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
    private final TrialTenantUserBootstrapService trialTenantUserBootstrapService;

    public TrialSignupService(TenantRepository tenantRepository,
                              UsuarioRepository usuarioRepository,
                              TrialTenantUserBootstrapService trialTenantUserBootstrapService) {
        this.tenantRepository = tenantRepository;
        this.usuarioRepository = usuarioRepository;
        this.trialTenantUserBootstrapService = trialTenantUserBootstrapService;
    }

    @Transactional
    public Usuario cadastrarTrial(PublicTrialRegistrationDTO dados) {
        if (usuarioRepository.countByLoginGlobal(dados.email().trim()) > 0) {
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

        return trialTenantUserBootstrapService.criarAdminEAssinaturaTrial(empresa.getId(), dados);
    }
}
