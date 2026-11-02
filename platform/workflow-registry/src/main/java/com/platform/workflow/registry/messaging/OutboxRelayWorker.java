package com.platform.workflow.registry.messaging;

import com.platform.workflow.registry.domain.OutboxEvent;
import com.platform.workflow.registry.repository.OutboxEventRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxRelayWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayWorker.class);

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;

    public OutboxRelayWorker(
            OutboxEventRepository outboxEventRepository,
            RabbitTemplate rabbitTemplate,
            @Value("${platform.messaging.exchange:platform.events}") String exchangeName) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
    }

    /**
     * Locks one row, publishes to RabbitMQ, marks published — then commits (n8n must run after this returns).
     */
    @Transactional
    public RelayedEvent publishNextOrNull() {
        List<OutboxEvent> batch = outboxEventRepository.findUnpublishedForUpdate(1);
        if (batch.isEmpty()) {
            return null;
        }
        OutboxEvent event = batch.get(0);
        try {
            rabbitTemplate.convertAndSend(exchangeName, event.getRoutingKey(), event.getPayloadJson(), message -> {
                message.getMessageProperties().setHeader("x-platform-version", "1");
                message.getMessageProperties().setContentType("application/json");
                return message;
            });
            event.setPublishedAt(Instant.now());
            return new RelayedEvent(event.getEventType(), event.getPayloadJson());
        } catch (Exception ex) {
            log.warn("Failed to publish outbox event id={}: {}", event.getId(), ex.getMessage());
            return null;
        }
    }

    public record RelayedEvent(String eventType, String payloadJson) {}
}
