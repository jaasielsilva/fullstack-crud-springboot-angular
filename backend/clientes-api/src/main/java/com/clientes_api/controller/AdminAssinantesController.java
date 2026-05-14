package com.clientes_api.controller;

import com.clientes_api.config.TenantContext;
import com.clientes_api.dto.admin.AdminDashboardMetricsDTO;
import com.clientes_api.dto.admin.AssinanteAdminDTO;
import com.clientes_api.dto.admin.StatusUpdateRequestDTO;
import com.clientes_api.service.AdminAssinantesExportService;
import com.clientes_api.service.AdminAssinantesService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/assinantes")
public class AdminAssinantesController {

    private final AdminAssinantesService adminAssinantesService;
    private final AdminAssinantesExportService adminAssinantesExportService;

    public AdminAssinantesController(AdminAssinantesService adminAssinantesService,
                                     AdminAssinantesExportService adminAssinantesExportService) {
        this.adminAssinantesService = adminAssinantesService;
        this.adminAssinantesExportService = adminAssinantesExportService;
    }

    private void validarAcessoMatriz() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null || tenantId != 1L) {
            throw new RuntimeException("Acesso negado: área restrita ao administrador do sistema.");
        }
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportarCsv() {
        validarAcessoMatriz();
        byte[] csv = adminAssinantesExportService.exportarAssinantesCsvUtf8();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"assinantes-lexcrm.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
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
