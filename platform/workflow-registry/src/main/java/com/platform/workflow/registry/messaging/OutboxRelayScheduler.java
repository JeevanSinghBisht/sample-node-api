package com.platform.workflow.registry.messaging;

import com.platform.workflow.registry.domain.OutboxEvent;
import com.platform.workflow.registry.repository.OutboxEventRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxRelayScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayScheduler.class);

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final N8nWebhookNotifier n8nWebhookNotifier;
    private final String exchangeName;
    private final int batchSize;

    public OutboxRelayScheduler(
            OutboxEventRepository outboxEventRepository,
            RabbitTemplate rabbitTemplate,
            N8nWebhookNotifier n8nWebhookNotifier,
            @Value("${platform.messaging.exchange:platform.events}") String exchangeName,
            @Value("${platform.outbox.batch-size:50}") int batchSize) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.n8nWebhookNotifier = n8nWebhookNotifier;
        this.exchangeName = exchangeName;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${platform.outbox.poll-interval-ms:2000}")
    @Transactional
    public void relay() {
        List<OutboxEvent> batch = outboxEventRepository.findUnpublishedForUpdate(batchSize);
        for (OutboxEvent event : batch) {
            try {
                rabbitTemplate.convertAndSend(exchangeName, event.getRoutingKey(), event.getPayloadJson(), message -> {
                    message.getMessageProperties().setHeader("x-platform-version", "1");
                    message.getMessageProperties().setContentType("application/json");
                    return message;
                });
                if (event.getEventType().equals("platform.task.state.changed")) {
                    n8nWebhookNotifier.notifyTaskStateChanged(event.getPayloadJson());
                }
                event.setPublishedAt(Instant.now());
            } catch (Exception ex) {
                log.warn("Failed to publish outbox event id={}: {}", event.getId(), ex.getMessage());
            }
        }
    }
}
