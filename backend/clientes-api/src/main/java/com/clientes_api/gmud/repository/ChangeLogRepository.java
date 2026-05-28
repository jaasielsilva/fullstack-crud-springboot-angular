package com.clientes_api.gmud.repository;

import com.clientes_api.gmud.model.ChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChangeLogRepository extends JpaRepository<ChangeLog, Long> {

    List<ChangeLog> findByChangeRequestIdOrderByChangedAtAsc(Long changeRequestId);
}
