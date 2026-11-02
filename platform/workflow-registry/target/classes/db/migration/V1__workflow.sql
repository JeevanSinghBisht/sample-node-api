-- Multi-agent SD platform: orchestration schema (MySQL 8.0+)
-- Source of truth for task state; agents and n8n never write directly except via registry API.

CREATE TABLE repo_config (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    github_repo_id  BIGINT NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    default_branch  VARCHAR(128) NOT NULL DEFAULT 'master',
    cursor_env_id   VARCHAR(64) NULL COMMENT 'Cursor environment public id',
    max_dev_attempts TINYINT UNSIGNED NOT NULL DEFAULT 3,
    max_review_attempts TINYINT UNSIGNED NOT NULL DEFAULT 2,
    max_qa_attempts   TINYINT UNSIGNED NOT NULL DEFAULT 2,
    enabled         TINYINT(1) NOT NULL DEFAULT 1,
    created_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_repo_full_name (full_name),
    UNIQUE KEY uk_github_repo_id (github_repo_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE workflow_task (
    id              CHAR(36) NOT NULL PRIMARY KEY,
    repo_config_id  BIGINT UNSIGNED NOT NULL,
    issue_number    INT UNSIGNED NOT NULL,
    issue_url       VARCHAR(512) NOT NULL,
    title           VARCHAR(512) NOT NULL,
    state           VARCHAR(32) NOT NULL,
    attempt_dev     TINYINT UNSIGNED NOT NULL DEFAULT 0,
    attempt_review  TINYINT UNSIGNED NOT NULL DEFAULT 0,
    attempt_qa      TINYINT UNSIGNED NOT NULL DEFAULT 0,
    paused          TINYINT(1) NOT NULL DEFAULT 0,
    next_retry_at   TIMESTAMP(3) NULL,
    pr_number       INT UNSIGNED NULL,
    head_sha        CHAR(40) NULL,
    pr_url          VARCHAR(512) NULL,
    metadata_json   JSON NULL COMMENT 'review/qa feedback blobs',
    created_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_task_repo FOREIGN KEY (repo_config_id) REFERENCES repo_config(id),
    UNIQUE KEY uk_repo_issue (repo_config_id, issue_number),
    KEY idx_state_retry (state, next_retry_at),
    KEY idx_state_updated (state, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Append-only audit of state changes
CREATE TABLE task_transition (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    task_id         CHAR(36) NOT NULL,
    from_state      VARCHAR(32) NULL,
    to_state        VARCHAR(32) NOT NULL,
    reason          VARCHAR(128) NOT NULL,
    actor_type      ENUM('SYSTEM','N8N','HUMAN','GITHUB_WEBHOOK') NOT NULL,
    actor_id        VARCHAR(255) NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    metadata_json   JSON NULL,
    created_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_transition_task FOREIGN KEY (task_id) REFERENCES workflow_task(id),
    UNIQUE KEY uk_transition_idempotency (idempotency_key),
    KEY idx_transition_task_time (task_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE task_execution (
    id              CHAR(36) NOT NULL PRIMARY KEY,
    task_id         CHAR(36) NOT NULL,
    role            ENUM('DEVELOPER','REVIEWER','TESTER') NOT NULL,
    status          ENUM('PENDING','RUNNING','SUCCEEDED','FAILED','CANCELLED','TIMED_OUT') NOT NULL,
    bc_id           VARCHAR(64) NULL COMMENT 'Cursor cloud agent id',
    agent_url       VARCHAR(512) NULL,
    n8n_execution_id VARCHAR(128) NULL,
    branch_name     VARCHAR(255) NULL,
    head_sha        CHAR(40) NULL,
    outcome         VARCHAR(32) NULL,
    summary         TEXT NULL,
    findings_json   JSON NULL,
    started_at      TIMESTAMP(3) NULL,
    finished_at     TIMESTAMP(3) NULL,
    created_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_execution_task FOREIGN KEY (task_id) REFERENCES workflow_task(id),
    UNIQUE KEY uk_bc_id (bc_id),
    KEY idx_execution_task_role (task_id, role, created_at),
    KEY idx_execution_status (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE task_artifact (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    task_id         CHAR(36) NOT NULL,
    execution_id    CHAR(36) NULL,
    artifact_type   ENUM('PULL_REQUEST','COMMIT','CHECK_RUN','DOCUMENT') NOT NULL,
    external_id     VARCHAR(255) NOT NULL COMMENT 'e.g. PR number or check run id',
    url             VARCHAR(512) NULL,
    sha             CHAR(40) NULL,
    created_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_artifact_task FOREIGN KEY (task_id) REFERENCES workflow_task(id),
    CONSTRAINT fk_artifact_execution FOREIGN KEY (execution_id) REFERENCES task_execution(id),
    KEY idx_artifact_task (task_id, artifact_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Transactional outbox for RabbitMQ
CREATE TABLE outbox_event (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_type      VARCHAR(128) NOT NULL,
    routing_key     VARCHAR(128) NOT NULL,
    payload_json    JSON NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    published_at    TIMESTAMP(3) NULL,
    created_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_outbox_idempotency (idempotency_key),
    KEY idx_outbox_unpublished (published_at, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Allowed transitions (optional validation table)
CREATE TABLE state_transition_rule (
    id              INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    from_state      VARCHAR(32) NOT NULL,
    to_state        VARCHAR(32) NOT NULL,
    UNIQUE KEY uk_from_to (from_state, to_state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO state_transition_rule (from_state, to_state) VALUES
('DRAFT', 'READY'),
('READY', 'DEVELOPING'),
('DEVELOPING', 'DEV_FAILED'),
('DEVELOPING', 'IN_REVIEW'),
('DEV_FAILED', 'DEVELOPING'),
('DEV_FAILED', 'CANCELLED'),
('IN_REVIEW', 'REVIEW_REJECTED'),
('IN_REVIEW', 'IN_QA'),
('REVIEW_REJECTED', 'DEVELOPING'),
('IN_QA', 'QA_FAILED'),
('IN_QA', 'AWAITING_HUMAN'),
('QA_FAILED', 'DEVELOPING'),
('AWAITING_HUMAN', 'DONE'),
('AWAITING_HUMAN', 'CANCELLED');
