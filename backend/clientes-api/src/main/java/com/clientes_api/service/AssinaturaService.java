package com.clientes_api.service;

import com.clientes_api.exception.ResourceNotFoundException;
import com.clientes_api.model.Assinatura;
import com.clientes_api.model.enums.StatusAssinatura;
import com.clientes_api.repository.AssinaturaRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AssinaturaService {

    private final AssinaturaRepository assinaturaRepository;

    public AssinaturaService(AssinaturaRepository assinaturaRepository) {
        this.assinaturaRepository = assinaturaRepository;
    }

    public Assinatura salvar(Assinatura assinatura) {
        return assinaturaRepository.save(assinatura);
    }

    public Assinatura buscarPorIdETenantOuErro(Long assinaturaId, Long tenantId) {
        return assinaturaRepository.findByIdAndTenantId(assinaturaId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura não encontrada para a empresa."));
    }

    public Optional<Assinatura> buscarUltimaPorTenant(Long tenantId) {
        return assinaturaRepository.findFirstByTenantIdOrderByIdDesc(tenantId);
    }

    public boolean existeAssinaturaAtivaValida(Long tenantId, java.time.LocalDateTime agora) {
        return assinaturaRepository.findByTenantIdAndStatus(tenantId, StatusAssinatura.ATIVA).stream()
                .anyMatch(a -> a.getDataFim() == null || !a.getDataFim().isBefore(agora));
    }
}
