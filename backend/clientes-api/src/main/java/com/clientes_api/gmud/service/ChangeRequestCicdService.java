package com.clientes_api.gmud.service;

import com.clientes_api.exception.BusinessException;
import com.clientes_api.gmud.dto.CicdDeployEventDTO;
import com.clientes_api.gmud.dto.CicdDeployResponseDTO;
import com.clientes_api.gmud.enums.ChangeStatus;
import com.clientes_api.gmud.enums.DeployEnvironment;
import com.clientes_api.gmud.enums.ChangeType;
import com.clientes_api.gmud.enums.RiskLevel;
import com.clientes_api.gmud.model.ChangeRequest;
import com.clientes_api.gmud.repository.ChangeRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ChangeRequestCicdService {

    private static final String SYSTEM_USER = "system:ci";

    private final ChangeRequestRepository changeRequestRepository;
    private final ChangeRequestService changeRequestService;
    private final boolean gmudEnabled;

    public ChangeRequestCicdService(
            ChangeRequestRepository changeRequestRepository,
            ChangeRequestService changeRequestService,
            @Value("${app.gmud.enabled:true}") boolean gmudEnabled) {
        this.changeRequestRepository = changeRequestRepository;
        this.changeRequestService = changeRequestService;
        this.gmudEnabled = gmudEnabled;
    }

    @Transactional
    public CicdDeployResponseDTO onDeployStart(CicdDeployEventDTO event) {
        if (!gmudEnabled) {
            return new CicdDeployResponseDTO(null, null, "GMUD desabilitado");
        }
        ChangeRequest entity = resolveChange(event);
        if (entity.getPipelineRunId() == null && event.pipelineRunId() != null) {
            entity.setPipelineRunId(event.pipelineRunId());
        }
        applyCiMetadata(entity, event);

        if (entity.getStatus() == ChangeStatus.OPEN) {
            changeRequestService.transitionInternal(entity, ChangeStatus.IN_APPROVAL, SYSTEM_USER,
                    commentOr(event, "Deploy iniciado pelo pipeline"));
        }

        if (isAutoApproveEnvironment(event.environment())) {
            if (entity.getStatus() == ChangeStatus.IN_APPROVAL) {
                changeRequestService.transitionInternal(entity, ChangeStatus.APPROVED, SYSTEM_USER,
                        "Aprovação automática (HML/DEV)");
            }
        }

        return new CicdDeployResponseDTO(entity.getId(), entity.getStatus(), "Deploy start registrado");
    }

    @Transactional
    public CicdDeployResponseDTO onDeploySuccess(CicdDeployEventDTO event) {
        if (!gmudEnabled) {
            return new CicdDeployResponseDTO(null, null, "GMUD desabilitado");
        }
        ChangeRequest entity = resolveChange(event);
        applyCiMetadata(entity, event);

        if (entity.getStatus() != ChangeStatus.APPROVED) {
            if (event.environment() == DeployEnvironment.PROD) {
                throw new BusinessException(
                        "Deploy PROD bloqueado: GMUD deve estar APPROVED (aprovação manual pendente).");
            }
            if (isAutoApproveEnvironment(event.environment()) && entity.getStatus() == ChangeStatus.IN_APPROVAL) {
                changeRequestService.transitionInternal(entity, ChangeStatus.APPROVED, SYSTEM_USER,
                        "Aprovação automática antes do deploy");
            } else {
                throw new BusinessException("Deploy bloqueado: status atual " + entity.getStatus());
            }
        }

        changeRequestService.transitionInternal(entity, ChangeStatus.DEPLOYED, SYSTEM_USER,
                commentOr(event, "Deploy concluído com sucesso"));
        return new CicdDeployResponseDTO(entity.getId(), entity.getStatus(), "Deploy success registrado");
    }

    @Transactional
    public CicdDeployResponseDTO onDeployFailure(CicdDeployEventDTO event) {
        if (!gmudEnabled) {
            return new CicdDeployResponseDTO(null, null, "GMUD desabilitado");
        }
        ChangeRequest entity = resolveChange(event);
        applyCiMetadata(entity, event);

        ChangeStatus status = entity.getStatus();
        if (status == ChangeStatus.DEPLOYED || status == ChangeStatus.APPROVED) {
            changeRequestService.transitionInternal(entity, ChangeStatus.ROLLBACK, SYSTEM_USER,
                    commentOr(event, "Rollback automático: falha no deploy"));
        } else {
            changeRequestService.appendLog(entity, status, status, SYSTEM_USER,
                    commentOr(event, "Falha no pipeline (status mantido: " + status + ")"));
        }
        return new CicdDeployResponseDTO(entity.getId(), entity.getStatus(), "Deploy failure registrado");
    }

    private ChangeRequest resolveChange(CicdDeployEventDTO event) {
        if (event.changeId() != null) {
            return changeRequestService.findEntity(event.changeId());
        }
        if (event.pipelineRunId() != null) {
            return changeRequestRepository.findByPipelineRunId(event.pipelineRunId())
                    .orElseGet(() -> createFromPipeline(event));
        }
        return createFromPipeline(event);
    }

    private ChangeRequest createFromPipeline(CicdDeployEventDTO event) {
        ChangeRequest entity = new ChangeRequest();
        entity.setTitle(event.title() != null ? event.title() : "Deploy " + event.environment());
        entity.setDescription("Registro automático via CI/CD");
        entity.setType(ChangeType.NORMAL);
        entity.setEnvironment(event.environment());
        entity.setRiskLevel(RiskLevel.MEDIUM);
        entity.setRollbackPlan("Reverter imagem Docker para tag anterior no Compose");
        entity.setStatus(ChangeStatus.OPEN);
        entity.setCreatedBy(SYSTEM_USER);
        entity.setCreatedAt(LocalDateTime.now());
        applyCiMetadata(entity, event);
        entity = changeRequestRepository.save(entity);
        changeRequestService.appendLog(entity, null, ChangeStatus.OPEN, SYSTEM_USER, "GMUD criada pelo pipeline");
        return entity;
    }

    private void applyCiMetadata(ChangeRequest entity, CicdDeployEventDTO event) {
        if (event.version() != null) {
            entity.setVersion(event.version());
        }
        if (event.artifact() != null) {
            entity.setArtifact(event.artifact());
        }
        if (event.pipelineRunId() != null) {
            entity.setPipelineRunId(event.pipelineRunId());
        }
        if (event.commitSha() != null) {
            entity.setCommitSha(event.commitSha());
        }
        changeRequestRepository.save(entity);
    }

    private boolean isAutoApproveEnvironment(DeployEnvironment environment) {
        return environment == DeployEnvironment.HML || environment == DeployEnvironment.DEV;
    }

    private String commentOr(CicdDeployEventDTO event, String defaultComment) {
        if (event.comment() != null && !event.comment().isBlank()) {
            return event.comment();
        }
        return defaultComment;
    }
}
