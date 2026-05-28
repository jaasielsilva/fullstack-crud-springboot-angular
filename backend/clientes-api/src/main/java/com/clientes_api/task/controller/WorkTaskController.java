package com.clientes_api.task.controller;

import com.clientes_api.dto.PageResponseDTO;
import com.clientes_api.task.dto.CreateWorkTaskDTO;
import com.clientes_api.task.dto.LinkGmudDTO;
import com.clientes_api.task.dto.WorkTaskResponseDTO;
import com.clientes_api.task.enums.TaskStatus;
import com.clientes_api.task.service.WorkTaskService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Tarefas", description = "Planejamento de trabalho da plataforma (super admin)")
@RestController
@RequestMapping("/api/tasks")
public class WorkTaskController {

    private final WorkTaskService workTaskService;

    public WorkTaskController(WorkTaskService workTaskService) {
        this.workTaskService = workTaskService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponseDTO<WorkTaskResponseDTO> listar(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return workTaskService.listar(status, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public WorkTaskResponseDTO buscar(@PathVariable Long id) {
        return workTaskService.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WorkTaskResponseDTO> criar(@Valid @RequestBody CreateWorkTaskDTO dto) {
        return ResponseEntity.ok(workTaskService.criar(dto));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('ADMIN')")
    public WorkTaskResponseDTO iniciar(@PathVariable Long id) {
        return workTaskService.iniciar(id);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public WorkTaskResponseDTO concluir(@PathVariable Long id) {
        return workTaskService.concluir(id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public WorkTaskResponseDTO cancelar(@PathVariable Long id) {
        return workTaskService.cancelar(id);
    }

    @PostMapping("/{id}/link-gmud")
    @PreAuthorize("hasRole('ADMIN')")
    public WorkTaskResponseDTO vincularGmud(@PathVariable Long id, @Valid @RequestBody LinkGmudDTO dto) {
        return workTaskService.vincularGmud(id, dto);
    }
}
