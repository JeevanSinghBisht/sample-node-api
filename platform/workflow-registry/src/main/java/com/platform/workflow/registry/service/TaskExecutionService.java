package com.platform.workflow.registry.service;

import com.platform.workflow.registry.domain.ExecutionRole;
import com.platform.workflow.registry.domain.ExecutionStatus;
import com.platform.workflow.registry.domain.RepoConfig;
import com.platform.workflow.registry.domain.TaskExecution;
import com.platform.workflow.registry.domain.TaskState;
import com.platform.workflow.registry.domain.WorkflowTask;
import com.platform.workflow.registry.repository.TaskExecutionRepository;
import com.platform.workflow.registry.repository.WorkflowTaskRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskExecutionService {

    private final WorkflowTaskRepository taskRepository;
    private final TaskExecutionRepository executionRepository;

    public TaskExecutionService(
            WorkflowTaskRepository taskRepository, TaskExecutionRepository executionRepository) {
        this.taskRepository = taskRepository;
        this.executionRepository = executionRepository;
    }

    @Transactional
    public TaskExecution createExecution(String taskId, ExecutionRole role, String n8nExecutionId) {
        WorkflowTask task =
                taskRepository.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        enforceAttemptLimits(task, role);

        TaskExecution execution = new TaskExecution();
        execution.setId(UUID.randomUUID().toString());
        execution.setTask(task);
        execution.setRole(role);
        execution.setStatus(ExecutionStatus.PENDING);
        execution.setN8nExecutionId(n8nExecutionId);
        incrementAttempt(task, role);
        taskRepository.save(task);
        return executionRepository.save(execution);
    }

    @Transactional
    public TaskExecution updateExecution(String executionId, ExecutionUpdate update) {
        TaskExecution execution = executionRepository
                .findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Execution not found"));

        if (update.status() != null) {
            execution.setStatus(update.status());
            if (update.status() == ExecutionStatus.RUNNING && execution.getStartedAt() == null) {
                execution.setStartedAt(Instant.now());
            }
            if (isTerminal(update.status())) {
                execution.setFinishedAt(Instant.now());
            }
        }
        if (update.bcId() != null) {
            execution.setBcId(update.bcId());
        }
        if (update.agentUrl() != null) {
            execution.setAgentUrl(update.agentUrl());
        }
        if (update.branchName() != null) {
            execution.setBranchName(update.branchName());
        }
        if (update.headSha() != null) {
            execution.setHeadSha(update.headSha());
        }
        if (update.outcome() != null) {
            execution.setOutcome(update.outcome());
        }
        if (update.summary() != null) {
            execution.setSummary(update.summary());
        }
        if (update.findingsJson() != null) {
            execution.setFindingsJson(update.findingsJson());
        }
        return executionRepository.save(execution);
    }

    private void enforceAttemptLimits(WorkflowTask task, ExecutionRole role) {
        RepoConfig repo = task.getRepoConfig();
        switch (role) {
            case DEVELOPER -> {
                if (task.getAttemptDev() >= repo.getMaxDevAttempts()) {
                    throw new InvalidTransitionException("Developer attempt limit exceeded");
                }
            }
            case REVIEWER -> {
                if (task.getAttemptReview() >= repo.getMaxReviewAttempts()) {
                    throw new InvalidTransitionException("Reviewer attempt limit exceeded");
                }
            }
            case TESTER -> {
                if (task.getAttemptQa() >= repo.getMaxQaAttempts()) {
                    throw new InvalidTransitionException("QA attempt limit exceeded");
                }
            }
        }
    }

    private void incrementAttempt(WorkflowTask task, ExecutionRole role) {
        switch (role) {
            case DEVELOPER -> task.setAttemptDev(task.getAttemptDev() + 1);
            case REVIEWER -> task.setAttemptReview(task.getAttemptReview() + 1);
            case TESTER -> task.setAttemptQa(task.getAttemptQa() + 1);
        }
    }

    private boolean isTerminal(ExecutionStatus status) {
        return status == ExecutionStatus.SUCCEEDED
                || status == ExecutionStatus.FAILED
                || status == ExecutionStatus.CANCELLED
                || status == ExecutionStatus.TIMED_OUT;
    }

    public record ExecutionUpdate(
            ExecutionStatus status,
            String bcId,
            String agentUrl,
            String branchName,
            String headSha,
            String outcome,
            String summary,
            String findingsJson) {}
}
