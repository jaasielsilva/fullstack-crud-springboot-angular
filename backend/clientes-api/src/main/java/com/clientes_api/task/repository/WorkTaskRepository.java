package com.clientes_api.task.repository;

import com.clientes_api.task.enums.TaskStatus;
import com.clientes_api.task.model.WorkTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface WorkTaskRepository extends JpaRepository<WorkTask, Long> {

    Page<WorkTask> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<WorkTask> findByStatusOrderByCreatedAtDesc(TaskStatus status, Pageable pageable);

    long countByCreatedByAndStatusIn(String createdBy, Collection<TaskStatus> statuses);

    List<WorkTask> findByCreatedByAndStatusInOrderByUpdatedAtDescCreatedAtDesc(
            String createdBy, Collection<TaskStatus> statuses, Pageable pageable);
}
