package com.clientes_api.gmud.controller;

import com.clientes_api.dto.PageResponseDTO;
import com.clientes_api.gmud.dto.ChangeRequestResponseDTO;
import com.clientes_api.gmud.dto.CreateChangeRequestDTO;
import com.clientes_api.gmud.dto.StatusTransitionDTO;
import com.clientes_api.gmud.enums.ChangeStatus;
import com.clientes_api.gmud.enums.DeployEnvironment;
import com.clientes_api.gmud.service.ChangeRequestService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "GMUD", description = "Gestão de mudanças (plataforma)")
@RestController
@RequestMapping("/api/gmud/changes")
public class ChangeRequestController {

    private final ChangeRequestService changeRequestService;

    public ChangeRequestController(ChangeRequestService changeRequestService) {
        this.changeRequestService = changeRequestService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponseDTO<ChangeRequestResponseDTO> listar(
            @RequestParam(required = false) ChangeStatus status,
            @RequestParam(required = false) DeployEnvironment environment,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return changeRequestService.listar(status, environment, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ChangeRequestResponseDTO buscar(@PathVariable Long id) {
        return changeRequestService.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChangeRequestResponseDTO> criar(@Valid @RequestBody CreateChangeRequestDTO dto) {
        return ResponseEntity.ok(changeRequestService.criar(dto));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('ADMIN')")
    public ChangeRequestResponseDTO submeter(
            @PathVariable Long id,
            @RequestBody(required = false) StatusTransitionDTO dto) {
        return changeRequestService.submeter(id, dto);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ChangeRequestResponseDTO aprovar(
            @PathVariable Long id,
            @RequestBody(required = false) StatusTransitionDTO dto) {
        return changeRequestService.aprovar(id, dto);
    }

    @PostMapping("/{id}/deploy")
    @PreAuthorize("hasRole('ADMIN')")
    public ChangeRequestResponseDTO implantar(
            @PathVariable Long id,
            @RequestBody(required = false) StatusTransitionDTO dto) {
        return changeRequestService.implantar(id, dto);
    }

    @PostMapping("/{id}/rollback")
    @PreAuthorize("hasRole('ADMIN')")
    public ChangeRequestResponseDTO rollback(
            @PathVariable Long id,
            @Valid @RequestBody StatusTransitionDTO dto) {
        return changeRequestService.rollback(id, dto);
    }
}
