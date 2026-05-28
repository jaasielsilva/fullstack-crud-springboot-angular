package com.clientes_api.task.service;

import com.clientes_api.dto.PageResponseDTO;
import com.clientes_api.exception.BusinessException;
import com.clientes_api.exception.ResourceNotFoundException;
import com.clientes_api.gmud.model.ChangeRequest;
import com.clientes_api.gmud.repository.ChangeRequestRepository;
import com.clientes_api.gmud.support.SuperAdminSupport;
import com.clientes_api.task.dto.CreateWorkTaskDTO;
import com.clientes_api.task.dto.LinkGmudDTO;
import com.clientes_api.task.dto.PendingTasksResponseDTO;
import com.clientes_api.task.dto.WorkTaskResponseDTO;
import com.clientes_api.task.enums.TaskStatus;
import com.clientes_api.task.model.WorkTask;
import com.clientes_api.task.repository.WorkTaskRepository;
import com.clientes_api.task.support.TaskBranchSupport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

import java.time.LocalDateTime;

@Service
public class WorkTaskService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_PENDING_LIST = 20;
    private static final EnumSet<TaskStatus> PENDING_STATUSES = EnumSet.of(TaskStatus.OPEN, TaskStatus.IN_PROGRESS);

    private final WorkTaskRepository workTaskRepository;
    private final ChangeRequestRepository changeRequestRepository;
    private final SuperAdminSupport superAdminSupport;

    public WorkTaskService(
            WorkTaskRepository workTaskRepository,
            ChangeRequestRepository changeRequestRepository,
            SuperAdminSupport superAdminSupport) {
        this.workTaskRepository = workTaskRepository;
        this.changeRequestRepository = changeRequestRepository;
        this.superAdminSupport = superAdminSupport;
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<WorkTaskResponseDTO> listar(TaskStatus status, int page, int size) {
        superAdminSupport.assertSuperAdmin();
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<WorkTask> result = status != null
                ? workTaskRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : workTaskRepository.findAllByOrderByCreatedAtDesc(pageable);

        return PageResponseDTO.from(result.map(WorkTaskMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public WorkTaskResponseDTO buscarPorId(Long id) {
        superAdminSupport.assertSuperAdmin();
        return WorkTaskMapper.toResponse(findEntity(id));
    }

    @Transactional(readOnly = true)
    public PendingTasksResponseDTO listarPendentesDoUsuarioLogado(int limit) {
        superAdminSupport.assertSuperAdmin();
        String username = superAdminSupport.currentUsername();
        int safeLimit = Math.min(Math.max(limit, 1), MAX_PENDING_LIST);
        long count = workTaskRepository.countByCreatedByAndStatusIn(username, PENDING_STATUSES);
        List<WorkTaskResponseDTO> tasks = workTaskRepository
                .findByCreatedByAndStatusInOrderByUpdatedAtDescCreatedAtDesc(
                        username, PENDING_STATUSES, PageRequest.of(0, safeLimit))
                .stream()
                .map(WorkTaskMapper::toResponse)
                .toList();
        return new PendingTasksResponseDTO(count, tasks);
    }

    @Transactional
    public WorkTaskResponseDTO criar(CreateWorkTaskDTO dto) {
        superAdminSupport.assertSuperAdmin();
        WorkTask entity = new WorkTask();
        entity.setTitle(dto.title().trim());
        entity.setDescription(dto.description());
        entity.setStatus(TaskStatus.OPEN);
        entity.setCreatedBy(superAdminSupport.currentUsername());
        entity.setCreatedAt(LocalDateTime.now());
        entity = workTaskRepository.save(entity);
        entity.setBranchName(TaskBranchSupport.suggestBranch(entity.getId(), entity.getTitle()));
        entity.setUpdatedAt(LocalDateTime.now());
        entity = workTaskRepository.save(entity);
        return WorkTaskMapper.toResponse(entity);
    }

    @Transactional
    public WorkTaskResponseDTO iniciar(Long id) {
        superAdminSupport.assertSuperAdmin();
        WorkTask entity = findEntity(id);
        if (entity.getStatus() != TaskStatus.OPEN) {
            throw new BusinessException("Somente tarefas OPEN podem ser iniciadas.");
        }
        entity.setStatus(TaskStatus.IN_PROGRESS);
        entity.setStartedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return WorkTaskMapper.toResponse(workTaskRepository.save(entity));
    }

    @Transactional
    public WorkTaskResponseDTO concluir(Long id) {
        superAdminSupport.assertSuperAdmin();
        WorkTask entity = findEntity(id);
        if (entity.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new BusinessException("Somente tarefas IN_PROGRESS podem ser concluídas.");
        }
        entity.setStatus(TaskStatus.DONE);
        entity.setCompletedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return WorkTaskMapper.toResponse(workTaskRepository.save(entity));
    }

    @Transactional
    public WorkTaskResponseDTO cancelar(Long id) {
        superAdminSupport.assertSuperAdmin();
        WorkTask entity = findEntity(id);
        if (entity.getStatus() == TaskStatus.DONE) {
            throw new BusinessException("Tarefa concluída não pode ser cancelada.");
        }
        entity.setStatus(TaskStatus.CANCELLED);
        entity.setUpdatedAt(LocalDateTime.now());
        return WorkTaskMapper.toResponse(workTaskRepository.save(entity));
    }

    @Transactional
    public WorkTaskResponseDTO vincularGmud(Long id, LinkGmudDTO dto) {
        superAdminSupport.assertSuperAdmin();
        WorkTask entity = findEntity(id);
        ChangeRequest change = changeRequestRepository.findById(dto.changeId())
                .orElseThrow(() -> new ResourceNotFoundException("GMUD não encontrada: " + dto.changeId()));
        change.setTaskId(id);
        changeRequestRepository.save(change);
        entity.setLinkedChangeId(change.getId());
        entity.setUpdatedAt(LocalDateTime.now());
        return WorkTaskMapper.toResponse(workTaskRepository.save(entity));
    }

    WorkTask findEntity(Long id) {
        return workTaskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada: " + id));
    }
}
