package com.clientes_api.service;

import com.clientes_api.model.Assinatura;
import com.clientes_api.model.Tenant;
import com.clientes_api.model.enums.StatusAssinatura;
import com.clientes_api.model.enums.StatusEmpresa;
import com.clientes_api.repository.AssinaturaRepository;
import com.clientes_api.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Verifica empresas em TRIAL com período expirado e bloqueia quem não possui assinatura ATIVA válida.
 */
@Service
public class TrialService {

    private static final Logger log = LoggerFactory.getLogger(TrialService.class);

    private final TenantRepository tenantRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final EmpresaService empresaService;

    public TrialService(TenantRepository tenantRepository,
                        AssinaturaRepository assinaturaRepository,
                        EmpresaService empresaService) {
        this.tenantRepository = tenantRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.empresaService = empresaService;
    }

    /** Executado periodicamente; pode ser ajustado via cron em produção. */
    @Scheduled(cron = "${app.trial.verification-cron}")
    @Transactional
    public void expirarTrialsSemPagamento() {
        LocalDateTime agora = LocalDateTime.now();
        List<Tenant> expirados = tenantRepository.findByStatusAndTrialFimBefore(StatusEmpresa.TRIAL, agora);

        for (Tenant empresa : expirados) {
            if (empresaService.isMatriz(empresa.getId())) {
                continue;
            }
            boolean temAssinaturaAtiva = assinaturaRepository
                    .findByTenantIdAndStatus(empresa.getId(), StatusAssinatura.ATIVA)
                    .stream()
                    .anyMatch(a -> a.getDataFim() == null || !a.getDataFim().isBefore(agora));

            if (temAssinaturaAtiva) {
                empresa.setStatus(StatusEmpresa.ATIVA);
                tenantRepository.save(empresa);
                continue;
            }

            empresa.setStatus(StatusEmpresa.BLOQUEADA);
            tenantRepository.save(empresa);

            List<Assinatura> trials = assinaturaRepository.findByTenantIdAndStatus(empresa.getId(), StatusAssinatura.TRIAL);
            for (Assinatura ass : trials) {
                ass.setStatus(StatusAssinatura.EXPIRADA);
                assinaturaRepository.save(ass);
            }
            log.info("Trial expirado: empresa {} bloqueada por falta de pagamento aprovado.", empresa.getId());
        }
    }
}
