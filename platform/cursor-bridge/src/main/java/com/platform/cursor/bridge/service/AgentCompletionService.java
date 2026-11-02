package com.platform.cursor.bridge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.cursor.bridge.config.BridgeProperties;
import com.platform.cursor.bridge.registry.RegistryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AgentCompletionService {

    private static final Logger log = LoggerFactory.getLogger(AgentCompletionService.class);
    private static final String IDEM_PREFIX = "bridge:completion:";

    private final RegistryClient registryClient;
    private final StringRedisTemplate redis;
    private final BridgeProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AgentCompletionService(
            RegistryClient registryClient,
            StringRedisTemplate redis,
            BridgeProperties properties,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper) {
        this.registryClient = registryClient;
        this.redis = redis;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
    }

    public void handle(AgentCompletionRequest request) {
        String idemKey = IDEM_PREFIX + request.idempotencyKey();
        Boolean first = redis.opsForValue().setIfAbsent(idemKey, "1", java.time.Duration.ofHours(24));
        if (Boolean.FALSE.equals(first)) {
            log.info("Duplicate completion ignored: {}", request.idempotencyKey());
            return;
        }

        String registryStatus = mapRegistryStatus(request.outcome());
        registryClient.patchExecution(
                request.executionId(),
                new RegistryClient.ExecutionPatch(
                        registryStatus,
                        request.bcId(),
                        request.agentUrl(),
                        request.branchName(),
                        request.headSha(),
                        request.outcome(),
                        request.summary(),
                        request.findingsJson()));

        notifyN8n(request);
    }

    private void notifyN8n(AgentCompletionRequest request) {
        String url = properties.getWebhooks().getN8nCompletionUrl();
        if (url == null || url.isBlank()) {
            return;
        }
        try {
            String body = objectMapper.writeValueAsString(request);
            restClient
                    .post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to notify n8n completion webhook: {}", e.getMessage());
        }
    }

    private static String mapRegistryStatus(String outcome) {
        if (outcome == null) {
            return "FAILED";
        }
        return switch (outcome.toLowerCase()) {
            case "success", "approved" -> "SUCCEEDED";
            case "rejected", "failed" -> "FAILED";
            default -> "FAILED";
        };
    }
}
