package com.platform.cursor.bridge.prompt;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.platform.cursor.bridge.domain.AgentRole;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PromptRendererTest {

    private final PromptRenderer renderer = new PromptRenderer();

    @Test
    void render_substitutesContext() {
        String prompt = renderer.render(
                AgentRole.DEVELOPER,
                Map.of(
                        "taskId", "task-1",
                        "executionId", "exec-1",
                        "issueUrl", "https://github.com/org/repo/issues/1",
                        "issueTitle", "Title",
                        "issueBody", "Body",
                        "issueNumber", "1",
                        "reviewFeedback", "none",
                        "qaFeedback", "none"));
        assertTrue(prompt.contains("task-1"));
        assertTrue(prompt.contains("Body"));
        assertTrue(prompt.contains("exec-1"));
    }
}
