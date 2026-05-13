package com.clientes_api.service;

import com.clientes_api.config.TenantContext;
import com.clientes_api.dto.SubscriptionSnapshotDTO;
import com.clientes_api.model.Assinatura;
import com.clientes_api.model.Plano;
import com.clientes_api.model.Tenant;
import com.clientes_api.model.enums.StatusAssinatura;
import com.clientes_api.model.enums.StatusEmpresa;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class SubscriptionSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionSnapshotService.class);

    private static final List<String> RECURSOS_TRIAL_PADRAO = List.of(
            "CLIENTES", "PRODUTOS", "USUARIOS", "PEDIDOS", "DASHBOARD_BASICO"
    );

    private final EmpresaService empresaService;
    private final AssinaturaService assinaturaService;
    private final ObjectMapper objectMapper;

    public SubscriptionSnapshotService(EmpresaService empresaService,
                                       AssinaturaService assinaturaService,
                                       ObjectMapper objectMapper) {
        this.empresaService = empresaService;
        this.assinaturaService = assinaturaService;
        this.objectMapper = objectMapper;
    }

    /**
     * Monta o contexto exibido no login /auth/me para trial, bloqueio e pendências de pagamento.
     */
    public SubscriptionSnapshotDTO montar(Long empresaId) {
        Long previous = TenantContext.getCurrentTenant();
        log.info(
                "SUBSCRIPTION | Montagem snapshot | empresaId={} | TenantContext antes={}",
                empresaId,
                previous
        );
        TenantContext.setCurrentTenant(empresaId);
        log.info(
                "SUBSCRIPTION | TenantContext após set (consultas Hibernate tenant-scoped) | empresaId={} | tenantContext={}",
                empresaId,
                TenantContext.getCurrentTenant()
        );
        try {
            Tenant empresa = empresaService.buscarPorIdOuErro(empresaId);
            log.info(
                    "SUBSCRIPTION | Tenant Hibernate carregado | empresaId={} | statusEmpresa={}",
                    empresaId,
                    empresa.getStatus()
            );
            Optional<Assinatura> opAss = assinaturaService.buscarUltimaPorTenant(empresaId);

            StatusAssinatura assinaturaStatus = opAss.map(Assinatura::getStatus).orElse(StatusAssinatura.TRIAL);

            Long diasTrial = null;
            if (empresa.getStatus() == StatusEmpresa.TRIAL && empresa.getTrialFim() != null) {
                long d = ChronoUnit.DAYS.between(LocalDate.now(), empresa.getTrialFim().toLocalDate());
                diasTrial = Math.max(0, d);
            }

            boolean pagamentoPendente = assinaturaStatus == StatusAssinatura.PENDENTE;
            String mensagemPendente = null;
            if (pagamentoPendente) {
                Assinatura pend = opAss.orElse(null);
                if (pend != null && pend.getAbacatePayBillingId() != null && !pend.getAbacatePayBillingId().isBlank()) {
                    mensagemPendente = "Aguarde a confirmação do pagamento (Abacate Pay).";
                } else if (pend != null && pend.getMercadoPagoPreferenceId() != null
                        && !pend.getMercadoPagoPreferenceId().isBlank()) {
                    mensagemPendente = "Aguarde a confirmação do pagamento (Mercado Pago).";
                } else {
                    mensagemPendente = "Aguarde a confirmação do pagamento.";
                }
            }

            Long diasAteVencimento = null;
            String mensagemRenovacao = null;
            if (opAss.isPresent() && assinaturaStatus == StatusAssinatura.ATIVA) {
                Assinatura ativa = opAss.get();
                if (ativa.getDataFim() != null) {
                    long d = ChronoUnit.DAYS.between(LocalDate.now(), ativa.getDataFim().toLocalDate());
                    if (d >= 0 && d <= 7) {
                        diasAteVencimento = d;
                        if (d == 0) {
                            mensagemRenovacao = "Sua assinatura renova hoje.";
                        } else if (d == 1) {
                            mensagemRenovacao = "Falta 1 dia para a renovação da sua assinatura.";
                        } else {
                            mensagemRenovacao = "Faltam " + d + " dias para a renovação da sua assinatura.";
                        }
                    }
                }
            }

            List<String> recursos = resolverRecursos(empresa, opAss.orElse(null));

            SubscriptionSnapshotDTO dto = new SubscriptionSnapshotDTO(
                    empresa.getStatus().name(),
                    assinaturaStatus.name(),
                    empresa.getTrialInicio(),
                    empresa.getTrialFim(),
                    diasTrial,
                    pagamentoPendente,
                    mensagemPendente,
                    diasAteVencimento,
                    mensagemRenovacao,
                    recursos
            );
            log.info(
                    "SUBSCRIPTION | Snapshot montado | empresaId={} | empresaStatus={} | assinaturaStatus={}",
                    empresaId,
                    dto.empresaStatus(),
                    dto.assinaturaStatus()
            );
            return dto;
        } finally {
            if (previous != null) {
                TenantContext.setCurrentTenant(previous);
                log.info("SUBSCRIPTION | TenantContext restaurado | tenantId={}", previous);
            } else {
                TenantContext.clear();
                log.info("SUBSCRIPTION | TenantContext limpo (não havia tenant prévio na thread)");
            }
        }
    }

    private List<String> resolverRecursos(Tenant empresa, Assinatura assinatura) {
        if (assinatura == null || assinatura.getPlano() == null) {
            if (empresa.getStatus() == StatusEmpresa.TRIAL) {
                return RECURSOS_TRIAL_PADRAO;
            }
            return Collections.emptyList();
        }
        Plano plano = assinatura.getPlano();
        String raw = plano.getRecursosLiberados();
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
