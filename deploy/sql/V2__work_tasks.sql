-- Tarefas de desenvolvimento (plataforma) + vínculo opcional com GMUD
CREATE TABLE IF NOT EXISTS work_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL,
    branch_name VARCHAR(128),
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    started_at DATETIME,
    completed_at DATETIME,
    created_by VARCHAR(255) NOT NULL,
    linked_change_id BIGINT
);

CREATE INDEX IF NOT EXISTS idx_work_tasks_status ON work_tasks(status);
CREATE INDEX IF NOT EXISTS idx_work_tasks_created_at ON work_tasks(created_at);

-- Executar uma vez; ignorar erro se a coluna já existir.
ALTER TABLE change_requests ADD COLUMN task_id BIGINT NULL;
CREATE INDEX IF NOT EXISTS idx_change_requests_task_id ON change_requests(task_id);
