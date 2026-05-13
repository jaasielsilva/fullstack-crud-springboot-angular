package com.clientes_api.service;

import com.clientes_api.dto.admin.AdminDashboardMetricsDTO;
import com.clientes_api.dto.admin.AssinanteAdminDTO;
import com.clientes_api.model.enums.StatusEmpresa;
import com.clientes_api.repository.AdminAssinantesRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminAssinantesService {

    private final AdminAssinantesRepository adminAssinantesRepository;
    private final EmpresaService empresaService;

    public AdminAssinantesService(AdminAssinantesRepository adminAssinantesRepository, EmpresaService empresaService) {
        this.adminAssinantesRepository = adminAssinantesRepository;
        this.empresaService = empresaService;
    }

    public List<AssinanteAdminDTO> listarAssinantes() {
        return adminAssinantesRepository.buscarTodosAssinantes();
    }

    public AdminDashboardMetricsDTO obterMetricas() {
        return adminAssinantesRepository.obterMetricas();
    }

    public void atualizarStatusEmpresa(Long empresaId, StatusEmpresa novoStatus) {
        // Bloqueio de segurança: Não permitir alterar a matriz
        if (empresaId != null && empresaId == 1L) {
            throw new IllegalArgumentException("Não é permitido alterar o status da empresa Matriz.");
        }
        
        empresaService.atualizarStatus(empresaId, novoStatus);
    }
}
