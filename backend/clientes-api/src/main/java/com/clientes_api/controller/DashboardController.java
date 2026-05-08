package com.clientes_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.clientes_api.dto.dashboard.DashboardExecutivoResponseDTO;
import com.clientes_api.dto.dashboard.DashboardMetaConfigRequestDTO;
import com.clientes_api.dto.dashboard.DashboardMetaConfigResponseDTO;
import com.clientes_api.model.Usuario;
import com.clientes_api.service.DashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/executivo")
    public ResponseEntity<DashboardExecutivoResponseDTO> resumoExecutivo(
            @RequestParam(defaultValue = "30d") String periodo) {
        Usuario usuario = usuarioLogado();
        DashboardExecutivoResponseDTO response = dashboardService.montarResumoExecutivo(
                periodo,
                usuario.getTenantId(),
                usuario.getLogin(),
                usuario.getRole().name());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/meta")
    public ResponseEntity<DashboardMetaConfigResponseDTO> buscarMeta() {
        return ResponseEntity.ok(dashboardService.buscarMetaConfigurada());
    }

    @PostMapping("/meta")
    public ResponseEntity<DashboardMetaConfigResponseDTO> salvarMeta(@RequestBody DashboardMetaConfigRequestDTO request) {
        return ResponseEntity.ok(dashboardService.salvarMeta(request));
    }

    private Usuario usuarioLogado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Usuario usuario) {
            return usuario;
        }
        throw new RuntimeException("Usuário não autenticado");
    }
}
