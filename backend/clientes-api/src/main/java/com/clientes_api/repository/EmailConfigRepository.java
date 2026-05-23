package com.clientes_api.repository;

import com.clientes_api.model.EmailConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailConfigRepository extends JpaRepository<EmailConfig, Long> {
    Optional<EmailConfig> findByTenantId(Long tenantId);
}
