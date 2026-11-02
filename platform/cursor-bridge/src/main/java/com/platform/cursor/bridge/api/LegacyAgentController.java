package com.platform.cursor.bridge.api;

import com.platform.cursor.bridge.api.dto.BridgeDtos;
import com.platform.cursor.bridge.service.AgentSpawnService;
import com.platform.cursor.bridge.service.SpawnAgentCommand;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LegacyAgentController {

    private final AgentSpawnService agentSpawnService;

    public LegacyAgentController(AgentSpawnService agentSpawnService) {
        this.agentSpawnService = agentSpawnService;
    }

    /** n8n workflow alias documented in platform/n8n/state-machine.md */
    @PostMapping("/agents")
    public BridgeDtos.SpawnAgentResponse spawn(@Valid @RequestBody BridgeDtos.SpawnAgentRequest request) {
        AgentSpawnService.SpawnAgentResult result = agentSpawnService.spawn(new SpawnAgentCommand(
                request.taskId(),
                request.executionId(),
                request.role(),
                request.repoUrl(),
                request.baseRef(),
                request.branchName(),
                request.environmentId(),
                request.modelId(),
                request.prUrl(),
                request.promptContext()));
        return new BridgeDtos.SpawnAgentResponse(
                result.bcId(), result.agentUrl(), result.runId(), result.branchName());
    }
}
