package com.platform.workflow.registry.api.internal;

import com.platform.workflow.registry.api.TaskMapper;
import com.platform.workflow.registry.api.dto.TaskDtos;
import com.platform.workflow.registry.domain.ActorType;
import com.platform.workflow.registry.domain.WorkflowTask;
import com.platform.workflow.registry.service.TaskLifecycleService;
import com.platform.workflow.registry.service.TaskQueryService;
import com.platform.workflow.registry.service.TaskTransitionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/tasks")
public class InternalTaskController {

    private final TaskQueryService queryService;
    private final TaskTransitionService transitionService;
    private final TaskLifecycleService lifecycleService;

    public InternalTaskController(
            TaskQueryService queryService,
            TaskTransitionService transitionService,
            TaskLifecycleService lifecycleService) {
        this.queryService = queryService;
        this.transitionService = transitionService;
        this.lifecycleService = lifecycleService;
    }

    @GetMapping("/{taskId}")
    public TaskDtos.TaskDetailResponse getTask(@PathVariable String taskId) {
        WorkflowTask task = queryService.getTask(taskId);
        return TaskMapper.toDetail(
                task, queryService.getTransitions(taskId), queryService.getExecutions(taskId));
    }

    @PostMapping("/from-issue")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDtos.TaskSummaryResponse createFromIssue(@Valid @RequestBody TaskDtos.CreateIssueTaskRequest request) {
        WorkflowTask task = lifecycleService.createFromIssue(new TaskLifecycleService.CreateIssueTaskRequest(
                request.repoFullName(),
                request.githubRepoId(),
                request.issueNumber(),
                request.issueUrl(),
                request.title(),
                request.issueBody(),
                request.idempotencyKey(),
                request.actorType() != null ? request.actorType() : ActorType.N8N,
                request.actorId()));
        return TaskMapper.toSummary(task);
    }

    @PostMapping("/{taskId}/transition")
    public TaskDtos.TransitionResponse transition(
            @PathVariable String taskId, @Valid @RequestBody TaskDtos.TransitionRequest request) {
        WorkflowTask task = transitionService.transition(
                taskId,
                request.targetState(),
                request.reason(),
                request.idempotencyKey(),
                ActorType.N8N,
                "n8n",
                request.metadata());
        return new TaskDtos.TransitionResponse(task.getId(), task.getState(), task.getUpdatedAt());
    }
}
