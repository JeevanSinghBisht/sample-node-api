package com.platform.workflow.registry.api;

import com.platform.workflow.registry.api.dto.TaskDtos;
import com.platform.workflow.registry.domain.TaskExecution;
import com.platform.workflow.registry.domain.TaskTransition;
import com.platform.workflow.registry.domain.WorkflowTask;
import java.util.List;

public final class TaskMapper {

    private TaskMapper() {}

    public static TaskDtos.TaskSummaryResponse toSummary(WorkflowTask task) {
        return new TaskDtos.TaskSummaryResponse(
                task.getId(),
                task.getRepoConfig().getFullName(),
                task.getIssueNumber(),
                task.getIssueUrl(),
                task.getTitle(),
                task.getState(),
                task.getAttemptDev(),
                task.getAttemptReview(),
                task.getAttemptQa(),
                task.getPrNumber(),
                task.getHeadSha(),
                task.getPrUrl(),
                task.getUpdatedAt());
    }

    public static TaskDtos.ExecutionResponse toExecution(TaskExecution execution) {
        return new TaskDtos.ExecutionResponse(
                execution.getId(),
                execution.getRole(),
                execution.getStatus(),
                execution.getBcId(),
                execution.getAgentUrl(),
                execution.getBranchName(),
                execution.getStartedAt(),
                execution.getFinishedAt());
    }

    public static TaskDtos.TransitionHistoryItem toTransitionItem(TaskTransition transition) {
        return new TaskDtos.TransitionHistoryItem(
                transition.getFromState(),
                transition.getToState(),
                transition.getReason(),
                transition.getCreatedAt());
    }

    public static TaskDtos.TaskDetailResponse toDetail(
            WorkflowTask task, List<TaskTransition> transitions, List<TaskExecution> executions) {
        return new TaskDtos.TaskDetailResponse(
                toSummary(task),
                transitions.stream().map(TaskMapper::toTransitionItem).toList(),
                executions.stream().map(TaskMapper::toExecution).toList());
    }
}
