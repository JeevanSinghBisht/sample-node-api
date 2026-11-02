package com.platform.workflow.registry.api.dto;

import com.platform.workflow.registry.domain.ActorType;
import com.platform.workflow.registry.domain.ExecutionRole;
import com.platform.workflow.registry.domain.ExecutionStatus;
import com.platform.workflow.registry.domain.TaskState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class TaskDtos {

    private TaskDtos() {}

    public record TransitionRequest(
            @NotNull TaskState targetState,
            @NotBlank String reason,
            @NotBlank String idempotencyKey,
            Map<String, Object> metadata) {}

    public record CreateExecutionRequest(
            @NotNull ExecutionRole role, String n8nExecutionId) {}

    public record UpdateExecutionRequest(
            ExecutionStatus status,
            String bcId,
            String agentUrl,
            String branchName,
            String headSha,
            String outcome,
            String summary,
            String findingsJson) {}

    public record CreateIssueTaskRequest(
            @NotBlank String repoFullName,
            Long githubRepoId,
            int issueNumber,
            @NotBlank String issueUrl,
            @NotBlank String title,
            @NotBlank String idempotencyKey,
            ActorType actorType,
            String actorId) {}

    public record TaskSummaryResponse(
            String id,
            String repoFullName,
            int issueNumber,
            String issueUrl,
            String title,
            TaskState state,
            int attemptDev,
            int attemptReview,
            int attemptQa,
            Integer prNumber,
            String headSha,
            String prUrl,
            Instant updatedAt) {}

    public record TransitionResponse(
            String id, TaskState state, Instant updatedAt) {}

    public record ExecutionResponse(
            String id,
            ExecutionRole role,
            ExecutionStatus status,
            String bcId,
            String agentUrl,
            String branchName,
            Instant startedAt,
            Instant finishedAt) {}

    public record TransitionHistoryItem(
            TaskState fromState, TaskState toState, String reason, Instant createdAt) {}

    public record TaskDetailResponse(
            TaskSummaryResponse task,
            List<TransitionHistoryItem> transitions,
            List<ExecutionResponse> executions) {}
}
