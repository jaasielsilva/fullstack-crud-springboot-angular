package com.clientes_api.gmud.repository;

import com.clientes_api.gmud.enums.ChangeStatus;
import com.clientes_api.gmud.enums.DeployEnvironment;
import com.clientes_api.gmud.model.ChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, Long> {

    List<ChangeRequest> findByEnvironmentOrderByCreatedAtDesc(DeployEnvironment environment);

    List<ChangeRequest> findByStatusOrderByCreatedAtDesc(ChangeStatus status);

    List<ChangeRequest> findByEnvironmentAndStatusOrderByCreatedAtDesc(
            DeployEnvironment environment, ChangeStatus status);

    Optional<ChangeRequest> findByPipelineRunId(String pipelineRunId);
}
