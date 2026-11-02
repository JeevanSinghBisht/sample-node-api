package com.platform.workflow.registry.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.platform.common.security.PlatformHmac;
import com.platform.workflow.registry.config.PlatformProperties;
import com.platform.workflow.registry.domain.ExecutionRole;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CursorBridgeClient {

    private final RestClient restClient;
    private final PlatformProperties properties;
    private final ObjectMapper objectMapper;

    public CursorBridgeClient(
            RestClient.Builder restClientBuilder, PlatformProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
                .baseUrl(properties.getBridge().getBaseUrl())
                .build();
    }

    public SpawnAgentResult spawnAgent(SpawnAgentCommand command) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("taskId", command.taskId());
            body.put("executionId", command.executionId());
            body.put("role", command.role().name());
            body.put("repoUrl", command.repoUrl());
            if (command.baseRef() != null) {
                body.put("baseRef", command.baseRef());
            }
            if (command.branchName() != null) {
                body.put("branchName", command.branchName());
            }
            if (command.prUrl() != null) {
                body.put("prUrl", command.prUrl());
            }
            if (command.promptContext() != null && !command.promptContext().isEmpty()) {
                body.set("promptContext", objectMapper.valueToTree(command.promptContext()));
            }

            String json = objectMapper.writeValueAsString(body);
            long ts = Instant.now().getEpochSecond();
            String signature = PlatformHmac.sign(properties.getInternal().getHmacSecret(), ts, json);

            ObjectNode response = restClient
                    .post()
                    .uri("/internal/v1/agents")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(PlatformHmac.TIMESTAMP_HEADER, String.valueOf(ts))
                    .header(PlatformHmac.SIGNATURE_HEADER, signature)
                    .body(json)
                    .retrieve()
                    .body(ObjectNode.class);

            if (response == null) {
                throw new CursorBridgeClientException("Empty response from cursor-bridge");
            }
            return new SpawnAgentResult(
                    textOrNull(response, "bcId"),
                    textOrNull(response, "agentUrl"),
                    textOrNull(response, "runId"),
                    textOrNull(response, "branchName"));
        } catch (CursorBridgeClientException e) {
            throw e;
        } catch (Exception e) {
            throw new CursorBridgeClientException("Failed to spawn agent via cursor-bridge", e);
        }
    }

    private static String textOrNull(ObjectNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    public record SpawnAgentCommand(
            String taskId,
            String executionId,
            ExecutionRole role,
            String repoUrl,
            String baseRef,
            String branchName,
            String prUrl,
            java.util.Map<String, Object> promptContext) {}

    public record SpawnAgentResult(String bcId, String agentUrl, String runId, String branchName) {}
}
