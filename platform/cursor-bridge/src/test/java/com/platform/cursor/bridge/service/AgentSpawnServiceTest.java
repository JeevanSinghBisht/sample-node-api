package com.platform.cursor.bridge.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.platform.cursor.bridge.cursor.CursorAgentCreated;
import com.platform.cursor.bridge.cursor.CursorAgentsClient;
import com.platform.cursor.bridge.domain.AgentRole;
import com.platform.cursor.bridge.prompt.PromptRenderer;
import com.platform.cursor.bridge.registry.RegistryClient;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentSpawnServiceTest {

    @Mock
    private PromptRenderer promptRenderer;

    @Mock
    private CursorAgentsClient cursorAgentsClient;

    @Mock
    private RegistryClient registryClient;

    private AgentSpawnService service;

    @BeforeEach
    void setUp() {
        service = new AgentSpawnService(promptRenderer, cursorAgentsClient, registryClient);
    }

    @Test
    void spawn_updatesRegistryWithRunningAgent() {
        SpawnAgentCommand command = new SpawnAgentCommand(
                "task-1",
                "exec-1",
                AgentRole.DEVELOPER,
                "https://github.com/org/repo",
                "master",
                "cursor/task-1-dev-ec4b",
                null,
                null,
                null,
                Map.of("issueBody", "Fix bug"));

        when(promptRenderer.render(eq(AgentRole.DEVELOPER), any())).thenReturn("prompt");
        when(cursorAgentsClient.createAgent(eq(command), eq("prompt")))
                .thenReturn(new CursorAgentCreated(
                        "bc-1", "https://cursor.com/agents/bc-1", "run-1", "cursor/task-1-dev-ec4b"));

        AgentSpawnService.SpawnAgentResult result = service.spawn(command);

        assertEquals("bc-1", result.bcId());
        verify(registryClient)
                .patchExecution(
                        eq("exec-1"),
                        eq(new RegistryClient.ExecutionPatch(
                                "RUNNING",
                                "bc-1",
                                "https://cursor.com/agents/bc-1",
                                "cursor/task-1-dev-ec4b",
                                null,
                                null,
                                null,
                                null)));
    }
}
