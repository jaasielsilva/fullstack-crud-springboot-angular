package com.clientes_api.gmud.repository;

import com.clientes_api.gmud.enums.ChangeStatus;
import com.clientes_api.gmud.enums.DeployEnvironment;
import com.clientes_api.gmud.model.ChangeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, Long> {

    Page<ChangeRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<ChangeRequest> findByEnvironmentOrderByCreatedAtDesc(DeployEnvironment environment, Pageable pageable);

    Page<ChangeRequest> findByStatusOrderByCreatedAtDesc(ChangeStatus status, Pageable pageable);

    Page<ChangeRequest> findByEnvironmentAndStatusOrderByCreatedAtDesc(
            DeployEnvironment environment, ChangeStatus status, Pageable pageable);

    Optional<ChangeRequest> findByPipelineRunId(String pipelineRunId);

    Page<ChangeRequest> findByTaskIdOrderByCreatedAtDesc(Long taskId, Pageable pageable);
}
