package com.clientes_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clientes_api.model.DashboardMetaConfig;

public interface DashboardMetaConfigRepository extends JpaRepository<DashboardMetaConfig, Long> {

    Optional<DashboardMetaConfig> findFirstByTenantIdAndLoginOrderByIdDesc(Long tenantId, String login);

    Optional<DashboardMetaConfig> findFirstByTenantIdAndRoleOrderByIdDesc(Long tenantId, String role);

    Optional<DashboardMetaConfig> findFirstByTenantIdAndLoginIsNullAndRoleIsNullOrderByIdDesc(Long tenantId);

    Optional<DashboardMetaConfig> findFirstByTenantIdAndLoginIsNullAndRoleOrderByIdDesc(Long tenantId, String role);
}
