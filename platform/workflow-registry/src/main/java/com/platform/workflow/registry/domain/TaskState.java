package com.platform.workflow.registry.domain;

public enum TaskState {
    DRAFT,
    READY,
    DEVELOPING,
    DEV_FAILED,
    IN_REVIEW,
    REVIEW_REJECTED,
    IN_QA,
    QA_FAILED,
    AWAITING_HUMAN,
    DONE,
    CANCELLED
}
