package com.clientes_api.task.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.clientes_api.task.dto.CreateWorkTaskDTO;
import com.clientes_api.task.enums.TaskStatus;
import com.clientes_api.task.model.WorkTask;
import com.clientes_api.task.repository.WorkTaskRepository;
import com.clientes_api.gmud.repository.ChangeRequestRepository;
import com.clientes_api.gmud.support.SuperAdminSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkTaskServiceTest {

    @Mock
    private WorkTaskRepository workTaskRepository;

    @Mock
    private ChangeRequestRepository changeRequestRepository;

    @Mock
    private SuperAdminSupport superAdminSupport;

    @InjectMocks
    private WorkTaskService workTaskService;

    @BeforeEach
    void setUp() {
        doNothing().when(superAdminSupport).assertSuperAdmin();
        when(superAdminSupport.currentUsername()).thenReturn("admin@lexcrm.com.br");
    }

    @Test
    void criar_defineBranchSugerida() {
        when(workTaskRepository.save(any())).thenAnswer(inv -> {
            WorkTask task = inv.getArgument(0);
            if (task.getId() == null) {
                task.setId(12L);
            }
            return task;
        });

        var response = workTaskService.criar(new CreateWorkTaskDTO("Paginação GMUD", "Implementar listagem paginada"));

        assertThat(response.id()).isEqualTo(12L);
        assertThat(response.status()).isEqualTo(TaskStatus.OPEN);
        assertThat(response.branchName()).isEqualTo("feature/TASK-12-paginacao-gmud");
    }
}
