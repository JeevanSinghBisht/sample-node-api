package com.platform.workflow.registry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.workflow.registry.domain.ActorType;
import com.platform.workflow.registry.domain.TaskState;
import com.platform.workflow.registry.domain.TaskTransition;
import com.platform.workflow.registry.domain.WorkflowTask;
import com.platform.workflow.registry.events.PlatformEventEnvelope;
import com.platform.workflow.registry.events.PlatformEventTypes;
import com.platform.workflow.registry.repository.StateTransitionRuleRepository;
import com.platform.workflow.registry.repository.TaskTransitionRepository;
import com.platform.workflow.registry.repository.WorkflowTaskRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskTransitionService {

    private final WorkflowTaskRepository taskRepository;
    private final TaskTransitionRepository transitionRepository;
    private final StateTransitionRuleRepository ruleRepository;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    public TaskTransitionService(
            WorkflowTaskRepository taskRepository,
            TaskTransitionRepository transitionRepository,
            StateTransitionRuleRepository ruleRepository,
            OutboxService outboxService,
            ObjectMapper objectMapper) {
        this.taskRepository = taskRepository;
        this.transitionRepository = transitionRepository;
        this.ruleRepository = ruleRepository;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WorkflowTask transition(
            String taskId,
            TaskState targetState,
            String reason,
            String idempotencyKey,
            ActorType actorType,
            String actorId,
            Map<String, Object> metadata) {
        Optional<TaskTransition> existing = transitionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            throw new DuplicateIdempotencyException("Transition already applied: " + idempotencyKey);
        }

        WorkflowTask task =
                taskRepository.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        if (task.isPaused()) {
            throw new InvalidTransitionException("Task is paused");
        }

        TaskState fromState = task.getState();
        if (fromState == targetState) {
            throw new InvalidTransitionException("Task already in state " + targetState);
        }

        if (!ruleRepository.existsByFromStateAndToState(fromState.name(), targetState.name())) {
            throw new InvalidTransitionException("Transition not allowed: " + fromState + " -> " + targetState);
        }

        applyMetadata(task, metadata);

        task.setState(targetState);
        taskRepository.save(task);

        String metadataJson = serializeMetadata(metadata);
        transitionRepository.save(new TaskTransition()
                .setTask(task)
                .setFromState(fromState)
                .setToState(targetState)
                .setReason(reason)
                .setActorType(actorType)
                .setActorId(actorId)
                .setIdempotencyKey(idempotencyKey)
                .setMetadataJson(metadataJson));

        publishStateChanged(task, fromState, targetState, idempotencyKey, metadata);
        return task;
    }

    private void applyMetadata(WorkflowTask task, Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return;
        }
        Object prNumber = metadata.get("prNumber");
        if (prNumber instanceof Number number) {
            task.setPrNumber(number.intValue());
        }
        Object headSha = metadata.get("headSha");
        if (headSha instanceof String sha) {
            task.setHeadSha(sha);
        }
        Object prUrl = metadata.get("prUrl");
        if (prUrl instanceof String url) {
            task.setPrUrl(url);
        }
        Object feedback = metadata.get("feedback");
        if (feedback != null) {
            try {
                task.setMetadataJson(objectMapper.writeValueAsString(feedback));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid feedback metadata", e);
            }
        }
    }

    private void publishStateChanged(
            WorkflowTask task,
            TaskState fromState,
            TaskState toState,
            String idempotencyKey,
            Map<String, Object> metadata) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", task.getId());
        payload.put("repoFullName", task.getRepoConfig().getFullName());
        payload.put("issueNumber", task.getIssueNumber());
        payload.put("issueUrl", task.getIssueUrl());
        payload.put("issueTitle", task.getTitle());
        issueBodyFromTask(task).ifPresent(body -> payload.put("issueBody", body));
        payload.put("previousState", fromState.name());
        payload.put("state", toState.name());
        payload.put("attemptDev", task.getAttemptDev());
        payload.put("attemptReview", task.getAttemptReview());
        payload.put("attemptQa", task.getAttemptQa());
        payload.put("prNumber", task.getPrNumber());
        payload.put("prUrl", task.getPrUrl());
        payload.put("headSha", task.getHeadSha());
        if (metadata != null) {
            payload.put("metadata", metadata);
        }

        String eventIdempotency = "state-changed-" + idempotencyKey;
        PlatformEventEnvelope envelope = PlatformEventEnvelope.create(
                PlatformEventTypes.TASK_STATE_CHANGED, task.getId(), eventIdempotency, payload);
        String routingKey = "platform.task.state." + toState.name();
        outboxService.enqueue(envelope, routingKey);
    }

    private Optional<String> issueBodyFromTask(WorkflowTask task) {
        String json = task.getMetadataJson();
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.hasNonNull("issueBody")) {
                return Optional.of(node.get("issueBody").asText());
            }
        } catch (JsonProcessingException ignored) {
            // ignore malformed metadata
        }
        return Optional.empty();
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid transition metadata", e);
        }
    }
}
