package com.clientes_api.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clientes_api.model.Assinatura;
import com.clientes_api.model.Tenant;
import com.clientes_api.model.enums.StatusAssinatura;
import com.clientes_api.model.enums.StatusEmpresa;
import com.clientes_api.repository.AssinaturaRepository;
import com.clientes_api.repository.TenantRepository;

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

    @Scheduled(cron = "${app.trial.verification-cron}")
    @Transactional
    public void expirarTrialsSemPagamento() {

        LocalDateTime agora = LocalDateTime.now();

        log.info("Iniciando verificação de trials expirados em {}", agora);

        List<Tenant> expirados =
                tenantRepository.findByStatusAndTrialFimBefore(
                        StatusEmpresa.TRIAL,
                        agora
                );

        log.info("Total de empresas TRIAL expiradas encontradas: {}", expirados.size());

        for (Tenant empresa : expirados) {

            try {

                log.info(
                        "Validando empresa ID={} Nome={}",
                        empresa.getId(),
                        empresa.getNome()
                );

                if (empresaService.isMatriz(empresa.getId())) {
                    log.info(
                            "Empresa ID={} ignorada por ser MATRIZ",
                            empresa.getId()
                    );
                    continue;
                }

                boolean temAssinaturaAtiva = assinaturaRepository
                        .findByTenantIdAndStatus(
                                empresa.getId(),
                                StatusAssinatura.ATIVA
                        )
                        .stream()
                        .anyMatch(a ->
                                a.getDataFim() == null
                                        || !a.getDataFim().isBefore(agora)
                        );

                log.info(
                        "Empresa ID={} possui assinatura ativa? {}",
                        empresa.getId(),
                        temAssinaturaAtiva
                );

                if (temAssinaturaAtiva) {

                    empresa.setStatus(StatusEmpresa.ATIVA);

                    tenantRepository.save(empresa);

                    log.info(
                            "Empresa ID={} reativada automaticamente",
                            empresa.getId()
                    );

                    continue;
                }

                empresa.setStatus(StatusEmpresa.BLOQUEADA);

                tenantRepository.save(empresa);

                log.warn(
                        "Empresa ID={} bloqueada por trial expirado",
                        empresa.getId()
                );

                List<Assinatura> trials =
                        assinaturaRepository.findByTenantIdAndStatus(
                                empresa.getId(),
                                StatusAssinatura.TRIAL
                        );

                for (Assinatura ass : trials) {

                    ass.setStatus(StatusAssinatura.EXPIRADA);

                    assinaturaRepository.save(ass);

                    log.info(
                            "Assinatura ID={} marcada como EXPIRADA",
                            ass.getId()
                    );
                }

            } catch (Exception ex) {

                log.error(
                        "Erro ao processar empresa ID={}: {}",
                        empresa.getId(),
                        ex.getMessage(),
                        ex
                );
            }
        }

        log.info("Finalizada verificação de trials expirados");
    }
}