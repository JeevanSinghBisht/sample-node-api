package com.platform.cursor.bridge.service;

import com.platform.cursor.bridge.domain.AgentRole;
import java.util.Map;

public record SpawnAgentCommand(
        String taskId,
        String executionId,
        AgentRole role,
        String repoUrl,
        String baseRef,
        String branchName,
        String environmentId,
        String modelId,
        String prUrl,
        Map<String, Object> promptContext) {}
