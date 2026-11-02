package com.platform.cursor.bridge.prompt;

import com.platform.cursor.bridge.domain.AgentRole;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component
public class PromptRenderer {

    public String render(AgentRole role, Map<String, Object> context) {
        String template = loadTemplate(role);
        if (context == null || context.isEmpty()) {
            return template;
        }
        String rendered = template;
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";
            rendered = rendered.replace(placeholder, value);
        }
        return rendered;
    }

    private String loadTemplate(AgentRole role) {
        String path = switch (role) {
            case DEVELOPER -> "prompts/developer.md";
            case REVIEWER -> "prompts/reviewer.md";
            case TESTER -> "prompts/tester.md";
        };
        try {
            return StreamUtils.copyToString(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Missing prompt template: " + path, e);
        }
    }
}
