package com.clientes_api.gmud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clientes_api.exception.BusinessException;
import com.clientes_api.gmud.dto.CreateChangeRequestDTO;
import com.clientes_api.gmud.dto.StatusTransitionDTO;
import com.clientes_api.gmud.enums.ChangeStatus;
import com.clientes_api.gmud.enums.ChangeType;
import com.clientes_api.gmud.enums.DeployEnvironment;
import com.clientes_api.gmud.enums.RiskLevel;
import com.clientes_api.gmud.model.ChangeLog;
import com.clientes_api.gmud.model.ChangeRequest;
import com.clientes_api.gmud.repository.ChangeLogRepository;
import com.clientes_api.gmud.repository.ChangeRequestRepository;
import com.clientes_api.gmud.support.SuperAdminSupport;
import com.clientes_api.task.repository.WorkTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChangeRequestServiceTest {

    @Mock
    private ChangeRequestRepository changeRequestRepository;

    @Mock
    private ChangeLogRepository changeLogRepository;

    @Mock
    private WorkTaskRepository workTaskRepository;

    @Mock
    private SuperAdminSupport superAdminSupport;

    @InjectMocks
    private ChangeRequestService changeRequestService;

    private ChangeRequest change;

    @BeforeEach
    void setUp() {
        doNothing().when(superAdminSupport).assertSuperAdmin();
        when(superAdminSupport.currentUsername()).thenReturn("admin@lexcrm.com.br");

        change = new ChangeRequest();
        change.setId(1L);
        change.setTitle("Deploy API");
        change.setType(ChangeType.NORMAL);
        change.setEnvironment(DeployEnvironment.HML);
        change.setRiskLevel(RiskLevel.LOW);
        change.setRollbackPlan("Reverter tag Docker");
        change.setStatus(ChangeStatus.OPEN);
        change.setCreatedBy("admin@lexcrm.com.br");
    }

    @Test
    void listar_retornaPaginaComMetadados() {
        change.setCreatedAt(LocalDateTime.now());
        when(changeRequestRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(change), Pageable.ofSize(10), 1));

        var page = changeRequestService.listar(null, null, null, 0, 10);

        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(page.number()).isZero();
        assertThat(page.size()).isEqualTo(10);
    }

    @Test
    void submeter_deOpenParaInApproval() {
        when(changeRequestRepository.findById(1L)).thenReturn(Optional.of(change));
        when(changeRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(changeLogRepository.findByChangeRequestIdOrderByChangedAtAsc(1L)).thenReturn(List.of());

        changeRequestService.submeter(1L, new StatusTransitionDTO("ok"));

        ArgumentCaptor<ChangeRequest> captor = ArgumentCaptor.forClass(ChangeRequest.class);
        verify(changeRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ChangeStatus.IN_APPROVAL);
        verify(changeLogRepository).save(any(ChangeLog.class));
    }

    @Test
    void submeter_semRollbackPlan_lancaBusinessException() {
        change.setRollbackPlan(null);
        when(changeRequestRepository.findById(1L)).thenReturn(Optional.of(change));

        assertThatThrownBy(() -> changeRequestService.submeter(1L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("rollback");
    }

    @Test
    void aprovar_deInApprovalParaApproved() {
        change.setStatus(ChangeStatus.IN_APPROVAL);
        when(changeRequestRepository.findById(1L)).thenReturn(Optional.of(change));
        when(changeRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(changeLogRepository.findByChangeRequestIdOrderByChangedAtAsc(1L)).thenReturn(List.of());

        changeRequestService.aprovar(1L, null);

        ArgumentCaptor<ChangeRequest> captor = ArgumentCaptor.forClass(ChangeRequest.class);
        verify(changeRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ChangeStatus.APPROVED);
        assertThat(captor.getValue().getApprovedAt()).isNotNull();
    }

    @Test
    void criar_persisteComStatusOpen() {
        CreateChangeRequestDTO dto = new CreateChangeRequestDTO(
                "Nova GMUD",
                "desc",
                ChangeType.STANDARD,
                DeployEnvironment.PROD,
                RiskLevel.HIGH,
                "impacto",
                "rollback",
                null,
                null,
                null
        );
        when(changeRequestRepository.save(any())).thenAnswer(inv -> {
            ChangeRequest cr = inv.getArgument(0);
            cr.setId(10L);
            return cr;
        });
        when(changeRequestRepository.findById(10L)).thenAnswer(inv -> {
            ChangeRequest cr = new ChangeRequest();
            cr.setId(10L);
            cr.setTitle(dto.title());
            cr.setStatus(ChangeStatus.OPEN);
            cr.setType(dto.type());
            cr.setEnvironment(dto.environment());
            cr.setRiskLevel(dto.riskLevel());
            cr.setCreatedBy("admin@lexcrm.com.br");
            return Optional.of(cr);
        });
        when(changeLogRepository.findByChangeRequestIdOrderByChangedAtAsc(10L)).thenReturn(List.of());

        var response = changeRequestService.criar(dto);

        assertThat(response.status()).isEqualTo(ChangeStatus.OPEN);
        assertThat(response.title()).isEqualTo("Nova GMUD");
    }
}
