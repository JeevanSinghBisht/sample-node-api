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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** DB-only steps for operator agent runs (kept separate so HTTP to cursor-bridge is not inside a DB txn). */
@Service
public class TaskOperatorRunService {

    private static final Set<TaskState> DEV_ENTRY_STATES =
            Set.of(TaskState.READY, TaskState.DEV_FAILED, TaskState.REVIEW_REJECTED, TaskState.QA_FAILED);

    private final WorkflowTaskRepository taskRepository;
    private final TaskExecutionRepository executionRepository;
    private final TaskTransitionService transitionService;
    private final TaskExecutionService executionService;
    private final com.platform.workflow.registry.config.PlatformProperties platformProperties;
    private final ObjectMapper objectMapper;

    public TaskOperatorRunService(
            WorkflowTaskRepository taskRepository,
            TaskExecutionRepository executionRepository,
            TaskTransitionService transitionService,
            TaskExecutionService executionService,
            com.platform.workflow.registry.config.PlatformProperties platformProperties,
            ObjectMapper objectMapper) {
        this.taskRepository = taskRepository;
        this.executionRepository = executionRepository;
        this.transitionService = transitionService;
        this.executionService = executionService;
        this.platformProperties = platformProperties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PreparedAgentRun prepareRun(String taskId, ExecutionRole role, String operatorId, String idempotencyKey) {
        WorkflowTask task = loadTask(taskId);
        if (task.isPaused()) {
            throw new InvalidTransitionException("Task is paused");
        }
        if (findActiveExecution(taskId) != null) {
            throw new InvalidTransitionException("An agent run is already in progress for this task");
        }

        switch (role) {
            case DEVELOPER -> ensureDeveloperState(task, idempotencyKey, operatorId);
            case REVIEWER -> requireState(task, TaskState.IN_REVIEW, role);
            case TESTER -> requireState(task, TaskState.IN_QA, role);
        }

        String dashboardExecKey = idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString();
        TaskExecution execution =
                executionService.createExecution(taskId, role, "dashboard-" + dashboardExecKey.substring(0, 8));
        WorkflowTask freshTask = loadTask(taskId);
        CursorBridgeClient.SpawnAgentCommand command = buildSpawnCommand(freshTask, execution, role);
        return new PreparedAgentRun(execution.getId(), command);
    }

    @Transactional
    public void markSpawnFailed(String executionId, String message) {
        executionService.updateExecution(
                executionId,
                new TaskExecutionService.ExecutionUpdate(
                        ExecutionStatus.FAILED,
                        null,
                        null,
                        null,
                        null,
                        "spawn_failed",
                        message,
                        null));
    }

    private void ensureDeveloperState(WorkflowTask task, String idempotencyKey, String operatorId) {
        TaskState state = task.getState();
        if (state == TaskState.DEVELOPING) {
            return;
        }
        if (!DEV_ENTRY_STATES.contains(state)) {
            throw new InvalidTransitionException("Cannot start developer from state " + state);
        }
        String key = idempotencyKey != null ? idempotencyKey + "-to-developing" : UUID.randomUUID().toString();
        transitionService.transition(
                task.getId(),
                TaskState.DEVELOPING,
                "operator_spawn_developer",
                key,
                ActorType.HUMAN,
                operatorId != null ? operatorId : "dashboard-operator",
                null);
    }

    private void requireState(WorkflowTask task, TaskState expected, ExecutionRole role) {
        if (task.getState() != expected) {
            throw new InvalidTransitionException("Cannot start " + role.name().toLowerCase() + " from state "
                    + task.getState());
        }
    }

    CursorBridgeClient.SpawnAgentCommand buildSpawnCommand(
            WorkflowTask task, TaskExecution execution, ExecutionRole role) {
        String repoUrl = agentRepoUrl();
        String baseRef = platformProperties.getAgent().getDefaultRef();
        Map<String, Object> promptContext = new LinkedHashMap<>();
        promptContext.put("taskId", task.getId());
        promptContext.put("executionId", execution.getId());
        promptContext.put("issueNumber", String.valueOf(task.getIssueNumber()));
        promptContext.put("issueUrl", task.getIssueUrl());
        promptContext.put("issueTitle", task.getTitle());
        issueBodyFromTask(task).ifPresent(body -> promptContext.put("issueBody", body));
        promptContext.put("reviewFeedback", "none");
        promptContext.put("qaFeedback", "none");

        String branchName = null;
        String prUrl = task.getPrUrl();
        switch (role) {
            case DEVELOPER -> branchName = "cursor/task-" + task.getId() + "-dev-" + task.getAttemptDev() + "-ec4b";
            case REVIEWER -> promptContext.put("prUrl", prUrl != null ? prUrl : "");
            case TESTER -> promptContext.put("prUrl", prUrl != null ? prUrl : "");
        }

        return new CursorBridgeClient.SpawnAgentCommand(
                task.getId(), execution.getId(), role, repoUrl, baseRef, branchName, prUrl, promptContext);
    }

    private String agentRepoUrl() {
        String url = platformProperties.getAgent().getRepoUrl();
        if (url == null || url.isBlank()) {
            throw new InvalidTransitionException(
                    "AGENT_REPO_URL is not configured on workflow-registry (set in platform/.env)");
        }
        return url.replaceAll("/$", "");
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

    public record PreparedAgentRun(String executionId, CursorBridgeClient.SpawnAgentCommand spawnCommand) {}
}
