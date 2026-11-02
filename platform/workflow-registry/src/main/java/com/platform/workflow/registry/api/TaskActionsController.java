package com.platform.workflow.registry.api;

import com.platform.workflow.registry.api.dto.TaskDtos;
import com.platform.workflow.registry.domain.ExecutionRole;
import com.platform.workflow.registry.service.TaskOperatorService;
import com.platform.workflow.registry.service.TaskQueryService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/actions")
public class TaskActionsController {

    private final TaskOperatorService operatorService;
    private final TaskQueryService queryService;

    public TaskActionsController(TaskOperatorService operatorService, TaskQueryService queryService) {
        this.operatorService = operatorService;
        this.queryService = queryService;
    }

    @GetMapping
    public TaskDtos.TaskActionsResponse listActions(@PathVariable String taskId) {
        return new TaskDtos.TaskActionsResponse(operatorService.availableActions(taskId));
    }

    @PostMapping("/run-developer")
    public TaskDtos.RunAgentActionResponse runDeveloper(
            @PathVariable String taskId, @RequestBody(required = false) TaskDtos.RunAgentActionRequest request) {
        return toResponse(operatorService.runAgent(
                taskId,
                ExecutionRole.DEVELOPER,
                operatorId(request),
                idempotencyKey(request, "dev")));
    }

    @PostMapping("/run-reviewer")
    public TaskDtos.RunAgentActionResponse runReviewer(
            @PathVariable String taskId, @RequestBody(required = false) TaskDtos.RunAgentActionRequest request) {
        return toResponse(operatorService.runAgent(
                taskId,
                ExecutionRole.REVIEWER,
                operatorId(request),
                idempotencyKey(request, "rev")));
    }

    @PostMapping("/run-tester")
    public TaskDtos.RunAgentActionResponse runTester(
            @PathVariable String taskId, @RequestBody(required = false) TaskDtos.RunAgentActionRequest request) {
        return toResponse(operatorService.runAgent(
                taskId,
                ExecutionRole.TESTER,
                operatorId(request),
                idempotencyKey(request, "qa")));
    }

    @PostMapping("/executions/{executionId}/complete")
    public TaskDtos.TaskDetailResponse reportCompletion(
            @PathVariable String taskId,
            @PathVariable String executionId,
            @Valid @RequestBody TaskDtos.ReportCompletionRequest request) {
        operatorService.reportCompletion(
                taskId,
                executionId,
                request.outcome(),
                request.summary(),
                request.findingsJson(),
                request.prUrl(),
                request.headSha(),
                request.prNumber(),
                request.idempotencyKey(),
                request.operatorId());
        return queryService.getTaskDetail(taskId);
    }

    private static TaskDtos.RunAgentActionResponse toResponse(TaskOperatorService.RunAgentResult result) {
        return new TaskDtos.RunAgentActionResponse(
                result.executionId(), result.bcId(), result.agentUrl(), result.branchName(), result.runId());
    }

    private static String idempotencyKey(TaskDtos.RunAgentActionRequest request, String prefix) {
        if (request != null && request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            return request.idempotencyKey();
        }
        return prefix + "-" + UUID.randomUUID();
    }

    private static String operatorId(TaskDtos.RunAgentActionRequest request) {
        if (request != null && request.operatorId() != null && !request.operatorId().isBlank()) {
            return request.operatorId();
        }
        return "dashboard-operator";
    }
}
