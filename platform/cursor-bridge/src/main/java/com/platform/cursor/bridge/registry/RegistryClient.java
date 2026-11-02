package com.platform.cursor.bridge.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.platform.common.security.PlatformHmac;
import com.platform.cursor.bridge.config.BridgeProperties;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RegistryClient {

    private final RestClient restClient;
    private final BridgeProperties properties;
    private final ObjectMapper objectMapper;

    public RegistryClient(RestClient.Builder restClientBuilder, BridgeProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
                .baseUrl(properties.getRegistry().getBaseUrl())
                .build();
    }

    public void patchExecution(String executionId, ExecutionPatch patch) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            if (patch.status() != null) {
                body.put("status", patch.status());
            }
            if (patch.bcId() != null) {
                body.put("bcId", patch.bcId());
            }
            if (patch.agentUrl() != null) {
                body.put("agentUrl", patch.agentUrl());
            }
            if (patch.branchName() != null) {
                body.put("branchName", patch.branchName());
            }
            if (patch.headSha() != null) {
                body.put("headSha", patch.headSha());
            }
            if (patch.outcome() != null) {
                body.put("outcome", patch.outcome());
            }
            if (patch.summary() != null) {
                body.put("summary", patch.summary());
            }
            if (patch.findingsJson() != null) {
                body.put("findingsJson", patch.findingsJson());
            }

            String json = objectMapper.writeValueAsString(body);
            long ts = Instant.now().getEpochSecond();
            String signature = PlatformHmac.sign(properties.getRegistry().getHmacSecret(), ts, json);

            restClient
                    .patch()
                    .uri("/internal/v1/executions/{executionId}", executionId)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(PlatformHmac.TIMESTAMP_HEADER, String.valueOf(ts))
                    .header(PlatformHmac.SIGNATURE_HEADER, signature)
                    .body(json)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new RegistryClientException("Failed to update execution " + executionId, e);
        }
    }

    public record ExecutionPatch(
            String status,
            String bcId,
            String agentUrl,
            String branchName,
            String headSha,
            String outcome,
            String summary,
            String findingsJson) {}
}
