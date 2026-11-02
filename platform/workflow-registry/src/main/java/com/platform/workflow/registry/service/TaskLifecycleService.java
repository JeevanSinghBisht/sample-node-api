package com.platform.workflow.registry.service;

import com.platform.workflow.registry.domain.RepoConfig;
import com.platform.workflow.registry.domain.TaskState;
import com.platform.workflow.registry.domain.WorkflowTask;
import com.platform.workflow.registry.repository.RepoConfigRepository;
import com.platform.workflow.registry.repository.WorkflowTaskRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskLifecycleService {

    private final RepoConfigRepository repoConfigRepository;
    private final WorkflowTaskRepository taskRepository;
    private final TaskTransitionService transitionService;

    public TaskLifecycleService(
            RepoConfigRepository repoConfigRepository,
            WorkflowTaskRepository taskRepository,
            TaskTransitionService transitionService) {
        this.repoConfigRepository = repoConfigRepository;
        this.taskRepository = taskRepository;
        this.transitionService = transitionService;
    }

    @Transactional
    public WorkflowTask createFromIssue(CreateIssueTaskRequest request) {
        RepoConfig repo = repoConfigRepository
                .findByFullName(request.repoFullName())
                .orElseGet(() -> repoConfigRepository.save(newRepoConfig(request)));

        taskRepository
                .findByRepoFullNameAndIssueNumber(request.repoFullName(), request.issueNumber())
                .ifPresent(t -> {
                    throw new DuplicateIdempotencyException("Task already exists for issue");
                });

        WorkflowTask task = new WorkflowTask();
        task.setId(UUID.randomUUID().toString());
        task.setRepoConfig(repo);
        task.setIssueNumber(request.issueNumber());
        task.setIssueUrl(request.issueUrl());
        task.setTitle(request.title());
        task.setState(TaskState.DRAFT);
        task.setAttemptDev(0);
        task.setAttemptReview(0);
        task.setAttemptQa(0);
        task.setPaused(false);
        taskRepository.save(task);

        return transitionService.transition(
                task.getId(),
                TaskState.READY,
                "issue_ready",
                request.idempotencyKey(),
                request.actorType(),
                request.actorId(),
                null);
    }

    private RepoConfig newRepoConfig(CreateIssueTaskRequest request) {
        RepoConfig repo = new RepoConfig();
        repo.setFullName(request.repoFullName());
        repo.setGithubRepoId(request.githubRepoId() != null ? request.githubRepoId() : 0L);
        repo.setDefaultBranch("master");
        repo.setEnabled(true);
        return repo;
    }

    public record CreateIssueTaskRequest(
            String repoFullName,
            Long githubRepoId,
            int issueNumber,
            String issueUrl,
            String title,
            String idempotencyKey,
            com.platform.workflow.registry.domain.ActorType actorType,
            String actorId) {}
}
