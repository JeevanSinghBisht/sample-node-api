package com.platform.workflow.registry.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "workflow_task")
public class WorkflowTask {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "repo_config_id", nullable = false)
    private RepoConfig repoConfig;

    @Column(name = "issue_number", nullable = false)
    private int issueNumber;

    @Column(name = "issue_url", nullable = false, length = 512)
    private String issueUrl;

    @Column(nullable = false, length = 512)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TaskState state;

    @Column(name = "attempt_dev", nullable = false)
    private int attemptDev;

    @Column(name = "attempt_review", nullable = false)
    private int attemptReview;

    @Column(name = "attempt_qa", nullable = false)
    private int attemptQa;

    @Column(nullable = false)
    private boolean paused;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "pr_number")
    private Integer prNumber;

    @Column(name = "head_sha", length = 40)
    private String headSha;

    @Column(name = "pr_url", length = 512)
    private String prUrl;

    @Column(name = "metadata_json", columnDefinition = "json")
    private String metadataJson;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public RepoConfig getRepoConfig() {
        return repoConfig;
    }

    public void setRepoConfig(RepoConfig repoConfig) {
        this.repoConfig = repoConfig;
    }

    public int getIssueNumber() {
        return issueNumber;
    }

    public void setIssueNumber(int issueNumber) {
        this.issueNumber = issueNumber;
    }

    public String getIssueUrl() {
        return issueUrl;
    }

    public void setIssueUrl(String issueUrl) {
        this.issueUrl = issueUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public TaskState getState() {
        return state;
    }

    public void setState(TaskState state) {
        this.state = state;
    }

    public int getAttemptDev() {
        return attemptDev;
    }

    public void setAttemptDev(int attemptDev) {
        this.attemptDev = attemptDev;
    }

    public int getAttemptReview() {
        return attemptReview;
    }

    public void setAttemptReview(int attemptReview) {
        this.attemptReview = attemptReview;
    }

    public int getAttemptQa() {
        return attemptQa;
    }

    public void setAttemptQa(int attemptQa) {
        this.attemptQa = attemptQa;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public Integer getPrNumber() {
        return prNumber;
    }

    public void setPrNumber(Integer prNumber) {
        this.prNumber = prNumber;
    }

    public String getHeadSha() {
        return headSha;
    }

    public void setHeadSha(String headSha) {
        this.headSha = headSha;
    }

    public String getPrUrl() {
        return prUrl;
    }

    public void setPrUrl(String prUrl) {
        this.prUrl = prUrl;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
