package com.platform.workflow.registry.messaging;

import com.platform.workflow.registry.config.PlatformProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class N8nWebhookNotifier {

    private static final Logger log = LoggerFactory.getLogger(N8nWebhookNotifier.class);

    private final RestClient restClient;
    private final PlatformProperties properties;

    public N8nWebhookNotifier(RestClient.Builder restClientBuilder, PlatformProperties properties) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    public void notifyTaskStateChanged(String payloadJson) {
        String url = properties.getN8n().getTaskStateWebhookUrl();
        if (url == null || url.isBlank()) {
            return;
        }
        try {
            restClient
                    .post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(payloadJson)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Failed to notify n8n task-state webhook: {}", ex.getMessage());
        }
    }
}
