package com.clientes_api.service;

import com.clientes_api.dto.PublicTrialRegistrationDTO;
import com.clientes_api.exception.BusinessException;
import com.clientes_api.model.Tenant;
import com.clientes_api.model.Usuario;
import com.clientes_api.model.enums.StatusEmpresa;
import com.clientes_api.repository.TenantRepository;
import com.clientes_api.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Fluxo público: landing → cadastro trial 7 dias sem cartão → empresa TRIAL + ADMIN + assinatura TRIAL.
 */
@Service
public class TrialSignupService {

    private static final Logger log = LoggerFactory.getLogger(TrialSignupService.class);

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
        log.info(
                "TRIAL | Início cadastro trial | empresaNome={} | email={} | diasTrial={}",
                dados.nomeEmpresa().trim(),
                dados.email().trim().toLowerCase(),
                DIAS_TRIAL
        );
        if (usuarioRepository.countByLoginGlobal(dados.email().trim()) > 0) {
            log.warn("TRIAL | Cadastro recusado | motivo=email_ja_cadastrado_como_usuario | email={}", dados.email().trim());
            throw new BusinessException("Já existe usuário com este e-mail.");
        }
        if (tenantRepository.existsByNomeIgnoreCase(dados.nomeEmpresa().trim())) {
            log.warn("TRIAL | Cadastro recusado | motivo=nome_empresa_duplicado | empresaNome={}", dados.nomeEmpresa().trim());
            throw new BusinessException("Já existe empresa cadastrada com este nome.");
        }
        if (tenantRepository.existsByEmailIgnoreCase(dados.email().trim())) {
            log.warn("TRIAL | Cadastro recusado | motivo=email_vinculado_outra_empresa | email={}", dados.email().trim());
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
        log.info(
                "TRIAL | Tenant persistido (Hibernate) | tenantId={} | status={} | trialInicio={} | trialFim={}",
                empresa.getId(),
                empresa.getStatus(),
                inicio,
                fim
        );

        Usuario admin = trialTenantUserBootstrapService.criarAdminEAssinaturaTrial(empresa.getId(), dados);
        log.info(
                "TRIAL | Cadastro trial concluído | tenantId={} | adminUserId={}",
                empresa.getId(),
                admin.getId()
        );
        return admin;
    }
}
