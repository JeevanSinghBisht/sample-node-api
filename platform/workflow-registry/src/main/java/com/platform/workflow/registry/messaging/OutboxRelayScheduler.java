package com.platform.workflow.registry.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxRelayScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayScheduler.class);

    private final OutboxRelayWorker relayWorker;
    private final N8nWebhookNotifier n8nWebhookNotifier;
    private final int batchSize;

    public OutboxRelayScheduler(
            OutboxRelayWorker relayWorker,
            N8nWebhookNotifier n8nWebhookNotifier,
            @Value("${platform.outbox.batch-size:50}") int batchSize) {
        this.relayWorker = relayWorker;
        this.n8nWebhookNotifier = n8nWebhookNotifier;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${platform.outbox.poll-interval-ms:2000}")
    public void relay() {
        for (int i = 0; i < batchSize; i++) {
            OutboxRelayWorker.RelayedEvent relayed = relayWorker.publishNextOrNull();
            if (relayed == null) {
                break;
            }
            notifyN8nAfterCommit(relayed);
        }
    }

    private void notifyN8nAfterCommit(OutboxRelayWorker.RelayedEvent relayed) {
        if (!"platform.task.state.changed".equals(relayed.eventType())) {
            return;
        }
        try {
            n8nWebhookNotifier.notifyTaskStateChanged(relayed.payloadJson());
        } catch (Exception ex) {
            log.warn("Failed to notify n8n after outbox relay: {}", ex.getMessage());
        }
    }
}
