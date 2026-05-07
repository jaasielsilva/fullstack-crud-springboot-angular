package com.clientes_api.controller;

import com.clientes_api.dto.TenantRegistrationDTO;
import com.clientes_api.model.Tenant;
import com.clientes_api.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants")
@Tag(name = "Empresas", description = "Gestão de Tenants e Onboarding (Apenas Super Admin)")
public class TenantController {

    @Autowired
    private TenantService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')") // Adicionalmente deveríamos checar se é do Tenant 1
    @Operation(summary = "Lista todas as empresas cadastradas")
    public ResponseEntity<List<Tenant>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cadastra uma nova empresa e seu administrador inicial")
    public ResponseEntity<Tenant> cadastrar(@RequestBody TenantRegistrationDTO data) {
        Tenant tenant = service.cadastrarEmpresa(data);
        return ResponseEntity.ok(tenant);
    }
}
