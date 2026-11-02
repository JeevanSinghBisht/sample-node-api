-- Align attempt counters with JPA int fields (Hibernate validate expects INTEGER, not TINYINT UNSIGNED).

ALTER TABLE repo_config
    MODIFY COLUMN max_dev_attempts INT NOT NULL DEFAULT 3,
    MODIFY COLUMN max_review_attempts INT NOT NULL DEFAULT 2,
    MODIFY COLUMN max_qa_attempts INT NOT NULL DEFAULT 2;

ALTER TABLE workflow_task
    MODIFY COLUMN attempt_dev INT NOT NULL DEFAULT 0,
    MODIFY COLUMN attempt_review INT NOT NULL DEFAULT 0,
    MODIFY COLUMN attempt_qa INT NOT NULL DEFAULT 0;
