package com.clientes_api.controller;

import com.clientes_api.config.TenantContext;
import com.clientes_api.dto.admin.AdminDashboardMetricsDTO;
import com.clientes_api.dto.admin.AssinanteAdminDTO;
import com.clientes_api.dto.admin.StatusUpdateRequestDTO;
import com.clientes_api.service.AdminAssinantesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/assinantes")
public class AdminAssinantesController {

    private final AdminAssinantesService adminAssinantesService;

    public AdminAssinantesController(AdminAssinantesService adminAssinantesService) {
        this.adminAssinantesService = adminAssinantesService;
    }

    private void validarAcessoMatriz() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null || tenantId != 1L) {
            throw new RuntimeException("Acesso negado: área restrita ao administrador do sistema.");
        }
    }

    @GetMapping
    public ResponseEntity<List<AssinanteAdminDTO>> listarAssinantes() {
        validarAcessoMatriz();
        return ResponseEntity.ok(adminAssinantesService.listarAssinantes());
    }

    @GetMapping("/metricas")
    public ResponseEntity<AdminDashboardMetricsDTO> obterMetricas() {
        validarAcessoMatriz();
        return ResponseEntity.ok(adminAssinantesService.obterMetricas());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> atualizarStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequestDTO request) {
        validarAcessoMatriz();
        adminAssinantesService.atualizarStatusEmpresa(id, request.getStatus());
        return ResponseEntity.noContent().build();
    }
}
