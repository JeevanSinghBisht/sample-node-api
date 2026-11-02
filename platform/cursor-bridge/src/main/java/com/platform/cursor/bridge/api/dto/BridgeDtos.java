package com.platform.cursor.bridge.api.dto;

import com.platform.cursor.bridge.domain.AgentRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public final class BridgeDtos {

    private BridgeDtos() {}

    public record SpawnAgentRequest(
            @NotBlank String taskId,
            @NotBlank String executionId,
            @NotNull AgentRole role,
            @NotBlank String repoUrl,
            String baseRef,
            String branchName,
            String environmentId,
            String modelId,
            String prUrl,
            Map<String, Object> promptContext) {}

    public record SpawnAgentResponse(String bcId, String agentUrl, String runId, String branchName) {}
}
