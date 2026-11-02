package com.platform.workflow.registry.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "repo_config")
public class RepoConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "github_repo_id", nullable = false)
    private Long githubRepoId;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "default_branch", nullable = false, length = 128)
    private String defaultBranch = "master";

    @Column(name = "cursor_env_id", length = 64)
    private String cursorEnvId;

    @Column(name = "max_dev_attempts", nullable = false)
    private int maxDevAttempts = 3;

    @Column(name = "max_review_attempts", nullable = false)
    private int maxReviewAttempts = 2;

    @Column(name = "max_qa_attempts", nullable = false)
    private int maxQaAttempts = 2;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGithubRepoId() {
        return githubRepoId;
    }

    public void setGithubRepoId(Long githubRepoId) {
        this.githubRepoId = githubRepoId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public String getCursorEnvId() {
        return cursorEnvId;
    }

    public void setCursorEnvId(String cursorEnvId) {
        this.cursorEnvId = cursorEnvId;
    }

    public int getMaxDevAttempts() {
        return maxDevAttempts;
    }

    public void setMaxDevAttempts(int maxDevAttempts) {
        this.maxDevAttempts = maxDevAttempts;
    }

    public int getMaxReviewAttempts() {
        return maxReviewAttempts;
    }

    public void setMaxReviewAttempts(int maxReviewAttempts) {
        this.maxReviewAttempts = maxReviewAttempts;
    }

    public int getMaxQaAttempts() {
        return maxQaAttempts;
    }

    public void setMaxQaAttempts(int maxQaAttempts) {
        this.maxQaAttempts = maxQaAttempts;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
