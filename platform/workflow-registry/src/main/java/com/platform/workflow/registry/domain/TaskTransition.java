package com.platform.workflow.registry.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "task_transition")
public class TaskTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private WorkflowTask task;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_state", length = 32)
    private TaskState fromState;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_state", nullable = false, length = 32)
    private TaskState toState;

    @Column(nullable = false, length = 128)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false)
    private ActorType actorType;

    @Column(name = "actor_id")
    private String actorId;

    @Column(name = "idempotency_key", nullable = false, length = 128, unique = true)
    private String idempotencyKey;

    @Column(name = "metadata_json", columnDefinition = "json")
    private String metadataJson;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public TaskTransition setTask(WorkflowTask task) {
        this.task = task;
        return this;
    }

    public TaskTransition setFromState(TaskState fromState) {
        this.fromState = fromState;
        return this;
    }

    public TaskTransition setToState(TaskState toState) {
        this.toState = toState;
        return this;
    }

    public TaskTransition setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public TaskTransition setActorType(ActorType actorType) {
        this.actorType = actorType;
        return this;
    }

    public TaskTransition setActorId(String actorId) {
        this.actorId = actorId;
        return this;
    }

    public TaskTransition setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
        return this;
    }

    public TaskTransition setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
        return this;
    }

    public TaskState getFromState() {
        return fromState;
    }

    public TaskState getToState() {
        return toState;
    }

    public String getReason() {
        return reason;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
