package com.platform.workflow.registry.api.internal;

import com.platform.workflow.registry.api.TaskMapper;
import com.platform.workflow.registry.api.dto.TaskDtos;
import com.platform.workflow.registry.domain.TaskExecution;
import com.platform.workflow.registry.service.TaskExecutionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1")
public class InternalExecutionController {

    private final TaskExecutionService executionService;

    public InternalExecutionController(TaskExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping("/tasks/{taskId}/executions")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDtos.ExecutionResponse createExecution(
            @PathVariable String taskId, @Valid @RequestBody TaskDtos.CreateExecutionRequest request) {
        TaskExecution execution =
                executionService.createExecution(taskId, request.role(), request.n8nExecutionId());
        return TaskMapper.toExecution(execution);
    }

    @PatchMapping("/executions/{executionId}")
    public TaskDtos.ExecutionResponse updateExecution(
            @PathVariable String executionId, @Valid @RequestBody TaskDtos.UpdateExecutionRequest request) {
        TaskExecution execution = executionService.updateExecution(
                executionId,
                new TaskExecutionService.ExecutionUpdate(
                        request.status(),
                        request.bcId(),
                        request.agentUrl(),
                        request.branchName(),
                        request.headSha(),
                        request.outcome(),
                        request.summary(),
                        request.findingsJson()));
        return TaskMapper.toExecution(execution);
    }
}
