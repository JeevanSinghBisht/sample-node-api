package com.platform.workflow.registry.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.workflow.registry.github.GitHubWebhookService;
import com.platform.workflow.registry.github.GitHubWebhookVerifier;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/webhooks/v1/github")
public class GitHubWebhookController {

    private final GitHubWebhookVerifier verifier;
    private final GitHubWebhookService webhookService;
    private final ObjectMapper objectMapper;

    public GitHubWebhookController(
            GitHubWebhookVerifier verifier, GitHubWebhookService webhookService, ObjectMapper objectMapper) {
        this.verifier = verifier;
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void receive(
            @RequestBody byte[] rawBody,
            @RequestHeader(value = GitHubWebhookVerifier.SIGNATURE_HEADER, required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", required = false) String event,
            HttpServletRequest request)
            throws IOException {
        String payload = new String(rawBody, StandardCharsets.UTF_8);
        if (!verifier.verify(payload, signature)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid GitHub signature");
        }
        if (event == null || event.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing X-GitHub-Event");
        }
        if ("ping".equals(event)) {
            return;
        }
        JsonNode json = objectMapper.readTree(payload);
        webhookService.handle(event, json);
    }
}
