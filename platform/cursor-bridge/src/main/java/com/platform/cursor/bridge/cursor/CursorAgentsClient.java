package com.platform.cursor.bridge.cursor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.platform.cursor.bridge.config.BridgeProperties;
import com.platform.cursor.bridge.domain.AgentRole;
import com.platform.cursor.bridge.service.SpawnAgentCommand;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CursorAgentsClient {

    private static final Logger log = LoggerFactory.getLogger(CursorAgentsClient.class);

    private final RestClient restClient;
    private final BridgeProperties properties;
    private final ObjectMapper objectMapper;

    public CursorAgentsClient(
            RestClient.Builder restClientBuilder, BridgeProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
                .baseUrl(properties.getCursor().getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, authorizationHeader())
                .build();
    }

    public CursorAgentCreated createAgent(SpawnAgentCommand command, String promptText) {
        if (properties.getCursor().isMockEnabled()) {
            return mockCreate(command);
        }

        ObjectNode body = buildCreateBody(command, promptText);
        JsonNode response = restClient
                .post()
                .uri("/v1/agents")
                .body(body.toString())
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.has("agent")) {
            throw new CursorApiException("Unexpected Cursor API response");
        }

        JsonNode agent = response.get("agent");
        JsonNode run = response.has("run") ? response.get("run") : null;
        String bcId = agent.path("id").asText(null);
        String agentUrl = agent.path("url").asText(null);
        String runId = run != null ? run.path("id").asText(null) : null;
        String branch = extractBranch(agent);
        if (bcId == null || agentUrl == null) {
            throw new CursorApiException("Cursor API response missing agent id or url");
        }
        return new CursorAgentCreated(bcId, agentUrl, runId, branch);
    }

    private ObjectNode buildCreateBody(SpawnAgentCommand command, String promptText) {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode prompt = root.putObject("prompt");
        prompt.put("text", promptText);
        root.put("name", truncateName(command.taskId() + " " + command.role()));

        ObjectNode model = root.putObject("model");
        model.put("id", command.modelId() != null ? command.modelId() : properties.getDefaultModelId());

        ArrayNode repos = root.putArray("repos");
        ObjectNode repo = repos.addObject();
        repo.put("url", command.repoUrl());
        if (command.prUrl() != null && !command.prUrl().isBlank()) {
            repo.put("prUrl", command.prUrl());
        } else {
            repo.put("startingRef", command.baseRef() != null ? command.baseRef() : "master");
        }

        if (command.environmentId() != null && !command.environmentId().isBlank()) {
            ObjectNode env = root.putObject("env");
            env.put("type", "cloud");
            env.put("name", command.environmentId());
            root.remove("repos");
        }

        root.put("autoCreatePR", command.role() == AgentRole.DEVELOPER);
        root.put("workOnCurrentBranch", false);

        ObjectNode envVars = root.putObject("envVars");
        envVars.put("PLATFORM_TASK_ID", command.taskId());
        envVars.put("PLATFORM_EXECUTION_ID", command.executionId());
        envVars.put("PLATFORM_ROLE", command.role().name());
        if (command.branchName() != null) {
            envVars.put("PLATFORM_BRANCH_HINT", command.branchName());
        }

        return root;
    }

    private String extractBranch(JsonNode agent) {
        JsonNode branches = agent.path("branches");
        if (branches.isArray() && !branches.isEmpty()) {
            return branches.get(0).path("name").asText(null);
        }
        return null;
    }

    private CursorAgentCreated mockCreate(SpawnAgentCommand command) {
        String bcId = "bc-mock-" + command.executionId().substring(0, 8);
        log.info("Mock Cursor agent created bcId={} role={}", bcId, command.role());
        return new CursorAgentCreated(
                bcId,
                "https://cursor.com/agents/" + bcId,
                "run-mock-1",
                command.branchName());
    }

    private String authorizationHeader() {
        String apiKey = properties.getCursor().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return "Basic " + Base64.getEncoder().encodeToString(":".getBytes(StandardCharsets.UTF_8));
        }
        String token = apiKey + ":";
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    private static String truncateName(String value) {
        return value.length() <= 100 ? value : value.substring(0, 100);
    }
}
