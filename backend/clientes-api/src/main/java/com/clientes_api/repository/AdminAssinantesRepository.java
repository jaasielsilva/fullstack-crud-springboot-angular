package com.clientes_api.repository;

import com.clientes_api.dto.admin.AdminDashboardMetricsDTO;
import com.clientes_api.dto.admin.AssinanteAdminDTO;

import java.util.List;

public interface AdminAssinantesRepository {
    List<AssinanteAdminDTO> buscarTodosAssinantes();
    AdminDashboardMetricsDTO obterMetricas();
}
