package com.clientes_api.gmud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.clientes_api.exception.BusinessException;
import com.clientes_api.gmud.dto.CicdDeployEventDTO;
import com.clientes_api.gmud.enums.ChangeStatus;
import com.clientes_api.gmud.enums.ChangeType;
import com.clientes_api.gmud.enums.DeployEnvironment;
import com.clientes_api.gmud.enums.RiskLevel;
import com.clientes_api.gmud.model.ChangeRequest;
import com.clientes_api.gmud.repository.ChangeLogRepository;
import com.clientes_api.gmud.repository.ChangeRequestRepository;
import com.clientes_api.gmud.support.SuperAdminSupport;
import com.clientes_api.task.repository.WorkTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChangeRequestCicdServiceTest {

    @Mock
    private ChangeRequestRepository changeRequestRepository;

    @Mock
    private ChangeLogRepository changeLogRepository;

    @Mock
    private WorkTaskRepository workTaskRepository;

    private ChangeRequestService changeRequestService;
    private ChangeRequestCicdService cicdService;

    @BeforeEach
    void setUp() {
        SuperAdminSupport superAdminSupport = org.mockito.Mockito.mock(SuperAdminSupport.class);
        changeRequestService = new ChangeRequestService(
                changeRequestRepository, changeLogRepository, workTaskRepository, superAdminSupport);
        cicdService = new ChangeRequestCicdService(changeRequestRepository, changeRequestService, true);

        when(changeRequestRepository.save(any())).thenAnswer(inv -> {
            ChangeRequest cr = inv.getArgument(0);
            if (cr.getId() == null) {
                cr.setId(99L);
            }
            return cr;
        });
        when(changeLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void onDeployStart_hml_autoAprova() {
        CicdDeployEventDTO event = new CicdDeployEventDTO(
                null, DeployEnvironment.HML, "Deploy HML", "abc1234",
                "ghcr.io/img:hml", "run-1", "abc1234", "ci");

        var response = cicdService.onDeployStart(event);

        assertThat(response.changeId()).isNotNull();
        assertThat(response.status()).isEqualTo(ChangeStatus.APPROVED);
    }

    @Test
    void onDeployStart_prod_ficaInApproval() {
        CicdDeployEventDTO event = new CicdDeployEventDTO(
                null, DeployEnvironment.PROD, "Deploy PROD", "v1",
                "ghcr.io/img:prod", "run-2", "def5678", "ci");

        var response = cicdService.onDeployStart(event);

        assertThat(response.status()).isEqualTo(ChangeStatus.IN_APPROVAL);
    }

    @Test
    void onDeploySuccess_prod_semAprovacao_lancaErro() {
        ChangeRequest existing = new ChangeRequest();
        existing.setId(5L);
        existing.setTitle("Deploy PROD");
        existing.setType(ChangeType.NORMAL);
        existing.setEnvironment(DeployEnvironment.PROD);
        existing.setRiskLevel(RiskLevel.MEDIUM);
        existing.setRollbackPlan("rollback");
        existing.setStatus(ChangeStatus.IN_APPROVAL);
        existing.setCreatedBy("system:ci");

        when(changeRequestRepository.findByPipelineRunId("run-prod")).thenReturn(Optional.of(existing));

        CicdDeployEventDTO event = new CicdDeployEventDTO(
                null, DeployEnvironment.PROD, null, null, null, "run-prod", null, null);

        assertThatThrownBy(() -> cicdService.onDeploySuccess(event))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("APPROVED");
    }

    @Test
    void onDeploySuccess_hml_marcaDeployed() {
        ChangeRequest existing = new ChangeRequest();
        existing.setId(6L);
        existing.setTitle("Deploy HML");
        existing.setType(ChangeType.NORMAL);
        existing.setEnvironment(DeployEnvironment.HML);
        existing.setRiskLevel(RiskLevel.LOW);
        existing.setRollbackPlan("rollback");
        existing.setStatus(ChangeStatus.APPROVED);
        existing.setCreatedBy("system:ci");

        when(changeRequestRepository.findByPipelineRunId("run-hml")).thenReturn(Optional.of(existing));

        var response = cicdService.onDeploySuccess(new CicdDeployEventDTO(
                null, DeployEnvironment.HML, null, null, null, "run-hml", null, "ok"));

        assertThat(response.status()).isEqualTo(ChangeStatus.DEPLOYED);
    }

    @Test
    void onDeploySuccess_prod_aprovado_marcaDeployed() {
        ChangeRequest existing = new ChangeRequest();
        existing.setId(7L);
        existing.setTitle("Deploy PROD aprovado");
        existing.setType(ChangeType.NORMAL);
        existing.setEnvironment(DeployEnvironment.PROD);
        existing.setRiskLevel(RiskLevel.HIGH);
        existing.setRollbackPlan("rollback");
        existing.setStatus(ChangeStatus.APPROVED);
        existing.setCreatedBy("system:ci");

        when(changeRequestRepository.findByPipelineRunId("run-prod-approved"))
                .thenReturn(Optional.of(existing));

        var response = cicdService.onDeploySuccess(new CicdDeployEventDTO(
                null, DeployEnvironment.PROD, null, null, null, "run-prod-approved", null, "ok"));

        assertThat(response.status()).isEqualTo(ChangeStatus.DEPLOYED);
    }
}
