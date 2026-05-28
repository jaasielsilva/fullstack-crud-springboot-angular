package com.clientes_api.gmud.service;

import com.clientes_api.dto.PageResponseDTO;
import com.clientes_api.exception.BusinessException;
import com.clientes_api.exception.ResourceNotFoundException;
import com.clientes_api.gmud.dto.ChangeRequestResponseDTO;
import com.clientes_api.gmud.dto.CreateChangeRequestDTO;
import com.clientes_api.gmud.dto.StatusTransitionDTO;
import com.clientes_api.gmud.enums.ChangeStatus;
import com.clientes_api.gmud.enums.DeployEnvironment;
import com.clientes_api.gmud.model.ChangeLog;
import com.clientes_api.gmud.model.ChangeRequest;
import com.clientes_api.gmud.repository.ChangeLogRepository;
import com.clientes_api.gmud.repository.ChangeRequestRepository;
import com.clientes_api.gmud.support.SuperAdminSupport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChangeRequestService {

    private final ChangeRequestRepository changeRequestRepository;
    private final ChangeLogRepository changeLogRepository;
    private final SuperAdminSupport superAdminSupport;

    public ChangeRequestService(
            ChangeRequestRepository changeRequestRepository,
            ChangeLogRepository changeLogRepository,
            SuperAdminSupport superAdminSupport) {
        this.changeRequestRepository = changeRequestRepository;
        this.changeLogRepository = changeLogRepository;
        this.superAdminSupport = superAdminSupport;
    }

    private static final int MAX_PAGE_SIZE = 100;

    @Transactional(readOnly = true)
    public PageResponseDTO<ChangeRequestResponseDTO> listar(
            ChangeStatus status,
            DeployEnvironment environment,
            int page,
            int size) {
        superAdminSupport.assertSuperAdmin();
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ChangeRequest> result;
        if (status != null && environment != null) {
            result = changeRequestRepository.findByEnvironmentAndStatusOrderByCreatedAtDesc(
                    environment, status, pageable);
        } else if (status != null) {
            result = changeRequestRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else if (environment != null) {
            result = changeRequestRepository.findByEnvironmentOrderByCreatedAtDesc(environment, pageable);
        } else {
            result = changeRequestRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return PageResponseDTO.from(result.map(cr -> ChangeRequestMapper.toResponse(cr, List.of())));
    }

    @Transactional(readOnly = true)
    public ChangeRequestResponseDTO buscarPorId(Long id) {
        superAdminSupport.assertSuperAdmin();
        ChangeRequest entity = findEntity(id);
        List<ChangeLog> logs = changeLogRepository.findByChangeRequestIdOrderByChangedAtAsc(id);
        return ChangeRequestMapper.toResponse(entity, logs);
    }

    @Transactional
    public ChangeRequestResponseDTO criar(CreateChangeRequestDTO dto) {
        superAdminSupport.assertSuperAdmin();
        ChangeRequest entity = new ChangeRequest();
        entity.setTitle(dto.title());
        entity.setDescription(dto.description());
        entity.setType(dto.type());
        entity.setEnvironment(dto.environment());
        entity.setRiskLevel(dto.riskLevel());
        entity.setImpactDescription(dto.impactDescription());
        entity.setRollbackPlan(dto.rollbackPlan());
        entity.setDeploymentWindowStart(dto.deploymentWindowStart());
        entity.setDeploymentWindowEnd(dto.deploymentWindowEnd());
        entity.setStatus(ChangeStatus.OPEN);
        entity.setCreatedBy(superAdminSupport.currentUsername());
        entity.setCreatedAt(LocalDateTime.now());
        entity = changeRequestRepository.save(entity);
        appendLog(entity, null, ChangeStatus.OPEN, superAdminSupport.currentUsername(), "GMUD criada");
        return buscarPorId(entity.getId());
    }

    @Transactional
    public ChangeRequestResponseDTO submeter(Long id, StatusTransitionDTO dto) {
        superAdminSupport.assertSuperAdmin();
        ChangeRequest entity = findEntity(id);
        assertStatus(entity, ChangeStatus.OPEN);
        validateRequiredFields(entity);
        transition(entity, ChangeStatus.IN_APPROVAL, superAdminSupport.currentUsername(),
                commentOrDefault(dto, "Enviada para aprovação"));
        return buscarPorId(id);
    }

    @Transactional
    public ChangeRequestResponseDTO aprovar(Long id, StatusTransitionDTO dto) {
        superAdminSupport.assertSuperAdmin();
        ChangeRequest entity = findEntity(id);
        assertStatus(entity, ChangeStatus.IN_APPROVAL);
        entity.setApprovedAt(LocalDateTime.now());
        transition(entity, ChangeStatus.APPROVED, superAdminSupport.currentUsername(),
                commentOrDefault(dto, "Aprovada pelo super administrador"));
        return buscarPorId(id);
    }

    @Transactional
    public ChangeRequestResponseDTO implantar(Long id, StatusTransitionDTO dto) {
        superAdminSupport.assertSuperAdmin();
        ChangeRequest entity = findEntity(id);
        assertStatus(entity, ChangeStatus.APPROVED);
        entity.setDeployedAt(LocalDateTime.now());
        transition(entity, ChangeStatus.DEPLOYED, superAdminSupport.currentUsername(),
                commentOrDefault(dto, "Deploy registrado manualmente"));
        return buscarPorId(id);
    }

    @Transactional
    public ChangeRequestResponseDTO rollback(Long id, StatusTransitionDTO dto) {
        superAdminSupport.assertSuperAdmin();
        ChangeRequest entity = findEntity(id);
        if (entity.getStatus() != ChangeStatus.DEPLOYED) {
            throw new BusinessException("Rollback permitido apenas a partir do status DEPLOYED.");
        }
        String comment = dto != null && dto.comment() != null && !dto.comment().isBlank()
                ? dto.comment()
                : null;
        if (comment == null) {
            throw new BusinessException("Comentário obrigatório para rollback.");
        }
        entity.setRolledBackAt(LocalDateTime.now());
        transition(entity, ChangeStatus.ROLLBACK, superAdminSupport.currentUsername(), comment);
        return buscarPorId(id);
    }

    @Transactional
    public ChangeRequest transitionInternal(
            ChangeRequest entity,
            ChangeStatus toStatus,
            String changedBy,
            String comment) {
        switch (toStatus) {
            case APPROVED -> entity.setApprovedAt(LocalDateTime.now());
            case DEPLOYED -> entity.setDeployedAt(LocalDateTime.now());
            case ROLLBACK -> entity.setRolledBackAt(LocalDateTime.now());
            default -> { }
        }
        transition(entity, toStatus, changedBy, comment);
        return entity;
    }

    ChangeRequest findEntity(Long id) {
        return changeRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GMUD não encontrada: " + id));
    }

    void appendLog(ChangeRequest entity, ChangeStatus from, ChangeStatus to, String changedBy, String comment) {
        ChangeLog log = new ChangeLog();
        log.setChangeRequest(entity);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setChangedBy(changedBy);
        log.setChangedAt(LocalDateTime.now());
        log.setComment(comment);
        changeLogRepository.save(log);
    }

    private void transition(ChangeRequest entity, ChangeStatus toStatus, String changedBy, String comment) {
        ChangeStatus from = entity.getStatus();
        entity.setStatus(toStatus);
        changeRequestRepository.save(entity);
        appendLog(entity, from, toStatus, changedBy, comment);
    }

    private void assertStatus(ChangeRequest entity, ChangeStatus expected) {
        if (entity.getStatus() != expected) {
            throw new BusinessException(
                    "Transição inválida: status atual é " + entity.getStatus() + ", esperado " + expected);
        }
    }

    private void validateRequiredFields(ChangeRequest entity) {
        if (entity.getTitle() == null || entity.getTitle().isBlank()) {
            throw new BusinessException("Título é obrigatório.");
        }
        if (entity.getRollbackPlan() == null || entity.getRollbackPlan().isBlank()) {
            throw new BusinessException("Plano de rollback é obrigatório para submeter.");
        }
    }

    private String commentOrDefault(StatusTransitionDTO dto, String defaultComment) {
        if (dto != null && dto.comment() != null && !dto.comment().isBlank()) {
            return dto.comment();
        }
        return defaultComment;
    }
}
