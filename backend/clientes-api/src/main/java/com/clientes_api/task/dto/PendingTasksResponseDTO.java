package com.clientes_api.task.dto;

import java.util.List;

public record PendingTasksResponseDTO(
        long pendingCount,
        List<WorkTaskResponseDTO> tasks
) {
}
