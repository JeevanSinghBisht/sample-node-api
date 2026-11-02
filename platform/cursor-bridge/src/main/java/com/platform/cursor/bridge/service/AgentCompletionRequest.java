package com.platform.cursor.bridge.service;

import com.platform.cursor.bridge.domain.AgentRole;
import jakarta.validation.constraints.NotBlank;

public record AgentCompletionRequest(
        @NotBlank String taskId,
        @NotBlank String executionId,
        AgentRole role,
        @NotBlank String outcome,
        @NotBlank String idempotencyKey,
        String bcId,
        String agentUrl,
        String branchName,
        String headSha,
        String prUrl,
        Integer prNumber,
        String summary,
        String findingsJson) {}
