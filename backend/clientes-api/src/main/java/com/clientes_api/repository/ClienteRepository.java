package com.clientes_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.clientes_api.model.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findFirstByEmailAndTenantId(String email, Long tenantId);

    List<Cliente> findAllByTenantId(Long tenantId);

    long countByTenantId(Long tenantId);

    @Query("SELECT COUNT(c) FROM Cliente c WHERE c.tenantId = :tenantId AND (c.ativo IS NULL OR c.ativo = true)")
    long countAtivosByTenantId(@Param("tenantId") Long tenantId);

    Optional<Cliente> findByIdAndTenantId(Long id, Long tenantId);
}
