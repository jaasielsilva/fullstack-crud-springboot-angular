-- GMUD — executar manualmente em PROD antes do deploy com JPA_DDL_AUTO=validate
CREATE TABLE IF NOT EXISTS change_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    environment VARCHAR(16) NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    impact_description TEXT,
    rollback_plan TEXT,
    deployment_window_start DATETIME,
    deployment_window_end DATETIME,
    created_at DATETIME NOT NULL,
    approved_at DATETIME,
    deployed_at DATETIME,
    rolled_back_at DATETIME,
    created_by VARCHAR(255) NOT NULL,
    version VARCHAR(128),
    artifact VARCHAR(512),
    pipeline_run_id VARCHAR(64) UNIQUE,
    commit_sha VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS change_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    change_id BIGINT NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    changed_by VARCHAR(255) NOT NULL,
    changed_at DATETIME NOT NULL,
    comment TEXT,
    CONSTRAINT fk_change_logs_request FOREIGN KEY (change_id) REFERENCES change_requests(id)
);

CREATE INDEX IF NOT EXISTS idx_change_requests_env ON change_requests(environment);
CREATE INDEX IF NOT EXISTS idx_change_requests_status ON change_requests(status);
