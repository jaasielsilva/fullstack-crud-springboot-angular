package com.clientes_api.service;

import com.clientes_api.exception.ResourceNotFoundException;
import com.clientes_api.model.Tenant;
import com.clientes_api.model.enums.StatusEmpresa;
import com.clientes_api.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmpresaService {

    private final TenantRepository tenantRepository;

    public EmpresaService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public Tenant buscarPorIdOuErro(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada."));
    }

    public Tenant salvar(Tenant tenant) {
        return tenantRepository.save(tenant);
    }

    /** Tenant matriz (id fixo 1) ignora bloqueios comerciais no restante do sistema. */
    public boolean isMatriz(Long tenantId) {
        return tenantId != null && tenantId == 1L;
    }

    @Transactional
    public void atualizarStatus(Long empresaId, StatusEmpresa novoStatus) {
        Tenant t = buscarPorIdOuErro(empresaId);
        t.setStatus(novoStatus);
        tenantRepository.save(t);
    }
}
