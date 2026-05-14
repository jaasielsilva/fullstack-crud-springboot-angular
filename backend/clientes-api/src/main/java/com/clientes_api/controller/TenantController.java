package com.clientes_api.controller;

import com.clientes_api.dto.TenantRegistrationDTO;
import com.clientes_api.exception.BusinessException;
import com.clientes_api.model.Tenant;
import com.clientes_api.model.Usuario;
import com.clientes_api.service.EmpresaService;
import com.clientes_api.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants")
@Tag(name = "Empresas", description = "Gestão de Tenants e Onboarding (Apenas Super Admin)")
public class TenantController {

    @Autowired
    private TenantService service;

    @Autowired
    private EmpresaService empresaService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lista todas as empresas cadastradas")
    public ResponseEntity<List<Tenant>> listar(@AuthenticationPrincipal Usuario usuario) {
        requireMatriz(usuario);
        return ResponseEntity.ok(service.listarTodos());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cadastra uma nova empresa e seu administrador inicial")
    public ResponseEntity<Tenant> cadastrar(@RequestBody TenantRegistrationDTO data, @AuthenticationPrincipal Usuario usuario) {
        requireMatriz(usuario);
        Tenant tenant = service.cadastrarEmpresa(data);
        return ResponseEntity.ok(tenant);
    }

    private void requireMatriz(Usuario usuario) {
        if (usuario == null || usuario.getTenantId() == null || !empresaService.isMatriz(usuario.getTenantId())) {
            throw new BusinessException("Operação restrita à empresa matriz.");
        }
    }
}
