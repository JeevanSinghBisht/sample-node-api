package com.platform.workflow.registry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.workflow.registry.domain.ActorType;
import com.platform.workflow.registry.domain.ExecutionRole;
import com.platform.workflow.registry.domain.ExecutionStatus;
import com.platform.workflow.registry.domain.TaskExecution;
import com.platform.workflow.registry.domain.TaskState;
import com.platform.workflow.registry.domain.WorkflowTask;
import com.platform.workflow.registry.integration.CursorBridgeClient;
import com.platform.workflow.registry.integration.CursorBridgeClientException;
import com.platform.workflow.registry.repository.TaskExecutionRepository;
import com.platform.workflow.registry.repository.WorkflowTaskRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskOperatorService {

    private static final Set<TaskState> DEV_ENTRY_STATES =
            Set.of(TaskState.READY, TaskState.DEV_FAILED, TaskState.REVIEW_REJECTED, TaskState.QA_FAILED);

    private final WorkflowTaskRepository taskRepository;
    private final TaskExecutionRepository executionRepository;
    private final TaskTransitionService transitionService;
    private final TaskExecutionService executionService;
    private final TaskOperatorRunService runService;
    private final CursorBridgeClient bridgeClient;
    private final ObjectMapper objectMapper;

    public TaskOperatorService(
            WorkflowTaskRepository taskRepository,
            TaskExecutionRepository executionRepository,
            TaskTransitionService transitionService,
            TaskExecutionService executionService,
            TaskOperatorRunService runService,
            CursorBridgeClient bridgeClient,
            ObjectMapper objectMapper) {
        this.taskRepository = taskRepository;
        this.executionRepository = executionRepository;
        this.transitionService = transitionService;
        this.executionService = executionService;
        this.runService = runService;
        this.bridgeClient = bridgeClient;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<String> availableActions(String taskId) {
        WorkflowTask task = loadTask(taskId);
        List<String> actions = new ArrayList<>();
        TaskState state = task.getState();
        if (DEV_ENTRY_STATES.contains(state)) {
            actions.add("RUN_DEVELOPER");
        }
        if (state == TaskState.IN_REVIEW) {
            actions.add("RUN_REVIEWER");
        }
        if (state == TaskState.IN_QA) {
            actions.add("RUN_TESTER");
        }
        if (state == TaskState.AWAITING_HUMAN) {
            actions.add("HUMAN_APPROVE");
        }
        TaskExecution active = findActiveExecution(taskId);
        if (active != null) {
            actions.add("REPORT_COMPLETION");
        }
        return actions;
    }

    public RunAgentResult runAgent(String taskId, ExecutionRole role, String operatorId, String idempotencyKey) {
        TaskOperatorRunService.PreparedAgentRun prepared =
                runService.prepareRun(taskId, role, operatorId, idempotencyKey);
        try {
            CursorBridgeClient.SpawnAgentResult spawned = bridgeClient.spawnAgent(prepared.spawnCommand());
            return new RunAgentResult(
                    prepared.executionId(),
                    spawned.bcId(),
                    spawned.agentUrl(),
                    spawned.branchName(),
                    spawned.runId());
        } catch (CursorBridgeClientException e) {
            runService.markSpawnFailed(prepared.executionId(), e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void reportCompletion(
            String taskId,
            String executionId,
            String outcome,
            String summary,
            String findingsJson,
            String prUrl,
            String headSha,
            Integer prNumber,
            String idempotencyKey,
            String operatorId) {
        TaskExecution execution = executionRepository
                .findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Execution not found"));
        if (!execution.getTask().getId().equals(taskId)) {
            throw new InvalidTransitionException("Execution does not belong to task");
        }

        String normalized = outcome == null ? "" : outcome.trim().toLowerCase();
        ExecutionStatus terminalStatus =
                normalized.equals("success") || normalized.equals("approved") || normalized.equals("passed")
                        ? ExecutionStatus.SUCCEEDED
                        : ExecutionStatus.FAILED;

        executionService.updateExecution(
                execution.getId(),
                new TaskExecutionService.ExecutionUpdate(
                        terminalStatus,
                        null,
                        null,
                        null,
                        headSha,
                        normalized,
                        summary,
                        findingsJson));

        Map<String, Object> metadata = new HashMap<>();
        if (prUrl != null) {
            metadata.put("prUrl", prUrl);
        }
        if (headSha != null) {
            metadata.put("headSha", headSha);
        }
        if (prNumber != null) {
            metadata.put("prNumber", prNumber);
        }
        if (summary != null) {
            metadata.put("feedback", summary);
        }

        TransitionPlan plan = transitionPlan(execution.getRole(), normalized);
        transitionService.transition(
                taskId,
                plan.targetState(),
                plan.reason(),
                idempotencyKey != null ? idempotencyKey : "completion-" + executionId,
                ActorType.HUMAN,
                operatorId != null ? operatorId : "dashboard-operator",
                metadata.isEmpty() ? null : metadata);
    }

    private TaskExecution findActiveExecution(String taskId) {
        return executionRepository.findByTaskIdOrderByCreatedAtDesc(taskId).stream()
                .filter(ex -> ex.getStatus() == ExecutionStatus.PENDING || ex.getStatus() == ExecutionStatus.RUNNING)
                .findFirst()
                .orElse(null);
    }

    private WorkflowTask loadTask(String taskId) {
        return taskRepository.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    private java.util.Optional<String> issueBodyFromTask(WorkflowTask task) {
        String json = task.getMetadataJson();
        if (json == null || json.isBlank()) {
            return java.util.Optional.empty();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.hasNonNull("issueBody")) {
                return java.util.Optional.of(node.get("issueBody").asText());
            }
        } catch (JsonProcessingException ignored) {
            // ignore
        }
        return java.util.Optional.empty();
    }

    private TransitionPlan transitionPlan(ExecutionRole role, String outcome) {
        return switch (role) {
            case DEVELOPER -> outcome.equals("success")
                    ? new TransitionPlan(TaskState.IN_REVIEW, "developer_completed")
                    : new TransitionPlan(TaskState.DEV_FAILED, "developer_failed");
            case REVIEWER -> outcome.equals("rejected")
                    ? new TransitionPlan(TaskState.REVIEW_REJECTED, "reviewer_rejected")
                    : new TransitionPlan(TaskState.IN_QA, "reviewer_approved");
            case TESTER -> outcome.equals("success")
                    ? new TransitionPlan(TaskState.AWAITING_HUMAN, "qa_passed")
                    : new TransitionPlan(TaskState.QA_FAILED, "qa_failed");
        };
    }

    public record RunAgentResult(
            String executionId, String bcId, String agentUrl, String branchName, String runId) {}

    private record TransitionPlan(TaskState targetState, String reason) {}
}
