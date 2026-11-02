package com.platform.workflow.registry.api;

import com.platform.workflow.registry.api.dto.TaskDtos;
import com.platform.workflow.registry.domain.ActorType;
import com.platform.workflow.registry.domain.TaskState;
import com.platform.workflow.registry.domain.WorkflowTask;
import com.platform.workflow.registry.service.TaskQueryService;
import com.platform.workflow.registry.service.TaskTransitionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskQueryService queryService;
    private final TaskTransitionService transitionService;

    public TaskController(TaskQueryService queryService, TaskTransitionService transitionService) {
        this.queryService = queryService;
        this.transitionService = transitionService;
    }

    @GetMapping
    public Page<TaskDtos.TaskSummaryResponse> listTasks(
            @RequestParam(required = false) TaskState state,
            @PageableDefault(size = 20) Pageable pageable) {
        return queryService.listTasks(state, pageable).map(TaskMapper::toSummary);
    }

    @GetMapping("/{taskId}")
    public TaskDtos.TaskDetailResponse getTask(@PathVariable String taskId) {
        WorkflowTask task = queryService.getTask(taskId);
        return TaskMapper.toDetail(
                task, queryService.getTransitions(taskId), queryService.getExecutions(taskId));
    }

    @PostMapping("/{taskId}/human-approve")
    public TaskDtos.TaskSummaryResponse humanApprove(
            @PathVariable String taskId, @Valid @RequestBody TaskDtos.HumanApproveRequest request) {
        WorkflowTask task = transitionService.transition(
                taskId,
                TaskState.DONE,
                "human_merge_acknowledged",
                request.idempotencyKey(),
                ActorType.HUMAN,
                request.mergedBy() != null ? request.mergedBy() : "operator",
                null);
        return TaskMapper.toSummary(task);
    }
}
