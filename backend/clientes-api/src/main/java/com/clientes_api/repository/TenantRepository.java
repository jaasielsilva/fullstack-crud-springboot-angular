package com.clientes_api.repository;

import com.clientes_api.model.Tenant;
import com.clientes_api.model.enums.StatusEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    List<Tenant> findByStatusAndTrialFimBefore(StatusEmpresa status, LocalDateTime data);

    List<Tenant> findByStatus(StatusEmpresa status);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByEmailIgnoreCase(String email);
}
