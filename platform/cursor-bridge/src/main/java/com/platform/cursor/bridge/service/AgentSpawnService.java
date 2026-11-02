package com.platform.cursor.bridge.service;

import com.platform.cursor.bridge.cursor.CursorAgentCreated;
import com.platform.cursor.bridge.cursor.CursorAgentsClient;
import com.platform.cursor.bridge.prompt.PromptRenderer;
import com.platform.cursor.bridge.registry.RegistryClient;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AgentSpawnService {

    private final PromptRenderer promptRenderer;
    private final CursorAgentsClient cursorAgentsClient;
    private final RegistryClient registryClient;

    public AgentSpawnService(
            PromptRenderer promptRenderer, CursorAgentsClient cursorAgentsClient, RegistryClient registryClient) {
        this.promptRenderer = promptRenderer;
        this.cursorAgentsClient = cursorAgentsClient;
        this.registryClient = registryClient;
    }

    public SpawnAgentResult spawn(SpawnAgentCommand command) {
        Map<String, Object> context = new HashMap<>();
        if (command.promptContext() != null) {
            context.putAll(command.promptContext());
        }
        context.putIfAbsent("taskId", command.taskId());
        context.putIfAbsent("executionId", command.executionId());

        String prompt = promptRenderer.render(command.role(), context);
        CursorAgentCreated created = cursorAgentsClient.createAgent(command, prompt);

        String branch = created.branchName() != null ? created.branchName() : command.branchName();
        registryClient.patchExecution(
                command.executionId(),
                new RegistryClient.ExecutionPatch(
                        "RUNNING",
                        created.bcId(),
                        created.agentUrl(),
                        branch,
                        null,
                        null,
                        null,
                        null));

        return new SpawnAgentResult(created.bcId(), created.agentUrl(), created.runId(), branch);
    }

    public record SpawnAgentResult(String bcId, String agentUrl, String runId, String branchName) {}
}
