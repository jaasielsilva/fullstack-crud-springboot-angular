package com.clientes_api.repository;

import com.clientes_api.model.Assinatura;
import com.clientes_api.model.enums.StatusAssinatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssinaturaRepository extends JpaRepository<Assinatura, Long> {

    Optional<Assinatura> findByIdAndTenantId(Long id, Long tenantId);

    List<Assinatura> findByTenantIdAndStatus(Long tenantId, StatusAssinatura status);

    Optional<Assinatura> findFirstByTenantIdOrderByIdDesc(Long tenantId);
}
