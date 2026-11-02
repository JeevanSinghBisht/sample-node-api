package com.platform.cursor.bridge.api;

import com.platform.cursor.bridge.config.BridgeProperties;
import com.platform.cursor.bridge.service.AgentCompletionRequest;
import com.platform.cursor.bridge.service.AgentCompletionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/webhooks/v1")
public class AgentCompletionController {

    public static final String COMPLETION_SECRET_HEADER = "X-Platform-Completion-Secret";

    private final AgentCompletionService completionService;
    private final BridgeProperties properties;

    public AgentCompletionController(AgentCompletionService completionService, BridgeProperties properties) {
        this.completionService = completionService;
        this.properties = properties;
    }

    @PostMapping("/agent-completions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void complete(@Valid @RequestBody AgentCompletionRequest request, HttpServletRequest httpRequest) {
        verifySecret(httpRequest);
        completionService.handle(request);
    }

    private void verifySecret(HttpServletRequest request) {
        String expected = properties.getWebhooks().getCompletionSecret();
        if (expected == null || expected.isBlank()) {
            return;
        }
        String provided = request.getHeader(COMPLETION_SECRET_HEADER);
        if (provided == null || !constantTimeEquals(expected, provided)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid completion secret");
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
