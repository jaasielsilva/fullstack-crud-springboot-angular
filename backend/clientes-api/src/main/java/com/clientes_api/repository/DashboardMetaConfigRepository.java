package com.clientes_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clientes_api.model.DashboardMetaConfig;

public interface DashboardMetaConfigRepository extends JpaRepository<DashboardMetaConfig, Long> {

    Optional<DashboardMetaConfig> findFirstByLoginOrderByIdDesc(String login);

    Optional<DashboardMetaConfig> findFirstByRoleOrderByIdDesc(String role);

    Optional<DashboardMetaConfig> findFirstByLoginIsNullAndRoleIsNullOrderByIdDesc();

    Optional<DashboardMetaConfig> findFirstByLoginAndRoleOrderByIdDesc(String login, String role);

    Optional<DashboardMetaConfig> findFirstByLoginIsNullAndRoleOrderByIdDesc(String role);
}
