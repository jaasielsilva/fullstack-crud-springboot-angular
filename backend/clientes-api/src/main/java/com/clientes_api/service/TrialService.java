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
    private final AssinaturaService assinaturaService;

    public TrialService(TenantRepository tenantRepository,
                        AssinaturaRepository assinaturaRepository,
                        EmpresaService empresaService,
                        AssinaturaService assinaturaService) {
        this.tenantRepository = tenantRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.empresaService = empresaService;
        this.assinaturaService = assinaturaService;
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

    /**
     * Empresas {@link StatusEmpresa#ATIVA} sem assinatura paga vigente e sem checkout {@link StatusAssinatura#PENDENTE}
     * em andamento passam a {@link StatusEmpresa#BLOQUEADA} (período de 30 dias após o último pagamento esgotado).
     */
    @Scheduled(cron = "${app.trial.verification-cron}")
    @Transactional
    public void bloquearPorAssinaturaMensalVencida() {
        LocalDateTime agora = LocalDateTime.now();
        log.info("Iniciando verificação de assinaturas pagas vencidas (renovação) em {}", agora);

        List<Tenant> empresasAtivas = tenantRepository.findByStatus(StatusEmpresa.ATIVA);
        int bloqueadas = 0;
        for (Tenant empresa : empresasAtivas) {
            try {
                if (empresaService.isMatriz(empresa.getId())) {
                    continue;
                }
                if (assinaturaService.existeAssinaturaAtivaValida(empresa.getId(), agora)) {
                    continue;
                }
                boolean temPendente = !assinaturaRepository
                        .findByTenantIdAndStatus(empresa.getId(), StatusAssinatura.PENDENTE)
                        .isEmpty();
                if (temPendente) {
                    continue;
                }
                empresa.setStatus(StatusEmpresa.BLOQUEADA);
                tenantRepository.save(empresa);
                for (Assinatura a : assinaturaRepository.findByTenantIdAndStatus(empresa.getId(), StatusAssinatura.ATIVA)) {
                    if (a.getDataFim() != null && a.getDataFim().isBefore(agora)) {
                        a.setStatus(StatusAssinatura.EXPIRADA);
                        assinaturaRepository.save(a);
                    }
                }
                bloqueadas++;
                log.warn("Empresa ID={} bloqueada: período da mensalidade encerrado sem renovação", empresa.getId());
            } catch (Exception ex) {
                log.error(
                        "Erro ao bloquear por vencimento de assinatura empresa ID={}: {}",
                        empresa.getId(),
                        ex.getMessage(),
                        ex
                );
            }
        }
        log.info(
                "Finalizada verificação de vencimento mensal | empresas ATIVAS={} | bloqueadas nesta execução={}",
                empresasAtivas.size(),
                bloqueadas
        );
    }
}