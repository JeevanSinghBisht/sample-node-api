package com.platform.workflow.registry.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.platform.workflow.registry.domain.ActorType;
import com.platform.workflow.registry.domain.TaskState;
import com.platform.workflow.registry.domain.WorkflowTask;
import com.platform.workflow.registry.repository.WorkflowTaskRepository;
import com.platform.workflow.registry.service.TaskLifecycleService;
import com.platform.workflow.registry.service.TaskTransitionService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GitHubWebhookService {

    private static final Pattern ISSUE_REF =
            Pattern.compile("(?i)(?:fixes|closes|resolves)\\s+#(\\d+)");
    private static final Pattern TASK_ID = Pattern.compile("(?i)Task-Id:\\s*([0-9a-fA-F-]{36})");

    private final TaskLifecycleService lifecycleService;
    private final TaskTransitionService transitionService;
    private final WorkflowTaskRepository taskRepository;
    private final com.platform.workflow.registry.config.PlatformProperties properties;

    public GitHubWebhookService(
            TaskLifecycleService lifecycleService,
            TaskTransitionService transitionService,
            WorkflowTaskRepository taskRepository,
            com.platform.workflow.registry.config.PlatformProperties properties) {
        this.lifecycleService = lifecycleService;
        this.transitionService = transitionService;
        this.taskRepository = taskRepository;
        this.properties = properties;
    }

    @Transactional
    public void handle(String event, JsonNode payload) {
        switch (event) {
            case "issues" -> handleIssues(payload);
            case "pull_request" -> handlePullRequest(payload);
            default -> {}
        }
    }

    private void handleIssues(JsonNode payload) {
        String action = payload.path("action").asText("");
        JsonNode issue = payload.get("issue");
        JsonNode repository = payload.get("repository");
        if (issue == null || repository == null) {
            return;
        }

        if ("labeled".equals(action)) {
            String label = payload.path("label").path("name").asText("");
            if (!properties.getGithub().getReadyLabel().equals(label)) {
                return;
            }
            markIssueReady(repository, issue);
            return;
        }

        if ("opened".equals(action) && issueHasReadyLabel(issue, properties.getGithub().getReadyLabel())) {
            markIssueReady(repository, issue);
        }
    }

    private void handlePullRequest(JsonNode payload) {
        if (!"opened".equals(payload.path("action").asText(""))) {
            return;
        }
        JsonNode pr = payload.get("pull_request");
        JsonNode repository = payload.get("repository");
        if (pr == null || repository == null) {
            return;
        }

        String repoFullName = repository.path("full_name").asText();
        String prUrl = pr.path("html_url").asText();
        int prNumber = pr.path("number").asInt();
        String headSha = pr.path("head").path("sha").asText(null);
        String body = pr.path("body").asText("");

        Optional<WorkflowTask> task = resolveTaskForPullRequest(repoFullName, body);
        task.ifPresent(t -> openPullRequest(t, prUrl, prNumber, headSha));
    }

    private void markIssueReady(JsonNode repository, JsonNode issue) {
        String repoFullName = repository.path("full_name").asText();
        long repoId = repository.path("id").asLong();
        int issueNumber = issue.path("number").asInt();
        String issueUrl = issue.path("html_url").asText();
        String title = issue.path("title").asText("Untitled");
        String idempotencyKey = "github-issue-ready-" + repoFullName + "-" + issueNumber;

        Optional<WorkflowTask> existing =
                taskRepository.findByRepoFullNameAndIssueNumber(repoFullName, issueNumber);
        if (existing.isPresent()) {
            WorkflowTask task = existing.get();
            if (task.getState() == TaskState.READY
                    || task.getState() == TaskState.DEVELOPING
                    || task.getState() == TaskState.IN_REVIEW) {
                return;
            }
            if (task.getState() == TaskState.DRAFT) {
                transitionService.transition(
                        task.getId(),
                        TaskState.READY,
                        "github_issue_ready",
                        idempotencyKey + "-draft",
                        ActorType.GITHUB_WEBHOOK,
                        "github",
                        null);
            }
            return;
        }

        lifecycleService.createFromIssue(new TaskLifecycleService.CreateIssueTaskRequest(
                repoFullName,
                repoId,
                issueNumber,
                issueUrl,
                title,
                issue.path("body").asText(null),
                idempotencyKey,
                ActorType.GITHUB_WEBHOOK,
                "github"));
    }

    private void openPullRequest(WorkflowTask task, String prUrl, int prNumber, String headSha) {
        if (task.getState() == TaskState.IN_REVIEW) {
            return;
        }
        if (task.getState() != TaskState.DEVELOPING && task.getState() != TaskState.DEV_FAILED) {
            return;
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("prUrl", prUrl);
        metadata.put("prNumber", prNumber);
        metadata.put("headSha", headSha);

        transitionService.transition(
                task.getId(),
                TaskState.IN_REVIEW,
                "pull_request_opened",
                "github-pr-" + prNumber + "-opened",
                ActorType.GITHUB_WEBHOOK,
                "github",
                metadata);
    }

    private Optional<WorkflowTask> resolveTaskForPullRequest(String repoFullName, String body) {
        Matcher taskMatcher = TASK_ID.matcher(body != null ? body : "");
        if (taskMatcher.find()) {
            return taskRepository.findById(taskMatcher.group(1));
        }
        List<Integer> issues = parseLinkedIssues(body);
        for (int issueNumber : issues) {
            Optional<WorkflowTask> task =
                    taskRepository.findByRepoFullNameAndIssueNumber(repoFullName, issueNumber);
            if (task.isPresent()) {
                return task;
            }
        }
        return Optional.empty();
    }

    static List<Integer> parseLinkedIssues(String body) {
        List<Integer> issues = new ArrayList<>();
        if (body == null) {
            return issues;
        }
        Matcher matcher = ISSUE_REF.matcher(body);
        while (matcher.find()) {
            issues.add(Integer.parseInt(matcher.group(1)));
        }
        return issues;
    }

    static boolean issueHasReadyLabel(JsonNode issue, String readyLabel) {
        JsonNode labels = issue.path("labels");
        if (!labels.isArray()) {
            return false;
        }
        for (JsonNode label : labels) {
            if (readyLabel.equals(label.path("name").asText())) {
                return true;
            }
        }
        return false;
    }
}
