package com.platform.workflow.registry.api;

import com.platform.workflow.registry.api.dto.TaskDtos;
import com.platform.workflow.registry.domain.TaskState;
import com.platform.workflow.registry.domain.WorkflowTask;
import com.platform.workflow.registry.service.TaskQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskQueryService queryService;

    public TaskController(TaskQueryService queryService) {
        this.queryService = queryService;
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
}
