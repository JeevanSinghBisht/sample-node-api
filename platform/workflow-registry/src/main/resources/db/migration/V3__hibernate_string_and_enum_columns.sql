-- Hibernate ddl-auto=validate: String @Column(length=N) → VARCHAR(N); @Enumerated(STRING) → VARCHAR(255).
-- Drop FKs before CHAR→VARCHAR on workflow_task.id / task_execution.id (MySQL 8 requires matching types).

ALTER TABLE task_artifact DROP FOREIGN KEY fk_artifact_execution;
ALTER TABLE task_artifact DROP FOREIGN KEY fk_artifact_task;
ALTER TABLE task_execution DROP FOREIGN KEY fk_execution_task;
ALTER TABLE task_transition DROP FOREIGN KEY fk_transition_task;

ALTER TABLE workflow_task
    MODIFY COLUMN id VARCHAR(36) NOT NULL,
    MODIFY COLUMN head_sha VARCHAR(40) NULL,
    MODIFY COLUMN issue_number INT NOT NULL,
    MODIFY COLUMN pr_number INT NULL;

ALTER TABLE task_execution
    MODIFY COLUMN id VARCHAR(36) NOT NULL,
    MODIFY COLUMN task_id VARCHAR(36) NOT NULL,
    MODIFY COLUMN head_sha VARCHAR(40) NULL,
    MODIFY COLUMN role VARCHAR(255) NOT NULL,
    MODIFY COLUMN status VARCHAR(255) NOT NULL;

ALTER TABLE task_transition
    MODIFY COLUMN task_id VARCHAR(36) NOT NULL,
    MODIFY COLUMN actor_type VARCHAR(255) NOT NULL;

ALTER TABLE task_artifact
    MODIFY COLUMN task_id VARCHAR(36) NOT NULL,
    MODIFY COLUMN execution_id VARCHAR(36) NULL,
    MODIFY COLUMN sha VARCHAR(40) NULL,
    MODIFY COLUMN artifact_type VARCHAR(255) NOT NULL;

ALTER TABLE state_transition_rule
    MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT;

ALTER TABLE task_transition
    ADD CONSTRAINT fk_transition_task FOREIGN KEY (task_id) REFERENCES workflow_task (id);

ALTER TABLE task_execution
    ADD CONSTRAINT fk_execution_task FOREIGN KEY (task_id) REFERENCES workflow_task (id);

ALTER TABLE task_artifact
    ADD CONSTRAINT fk_artifact_task FOREIGN KEY (task_id) REFERENCES workflow_task (id);

ALTER TABLE task_artifact
    ADD CONSTRAINT fk_artifact_execution FOREIGN KEY (execution_id) REFERENCES task_execution (id);
