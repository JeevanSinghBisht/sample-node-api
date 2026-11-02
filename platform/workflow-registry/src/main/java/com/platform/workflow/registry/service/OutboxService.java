package com.platform.workflow.registry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.workflow.registry.domain.OutboxEvent;
import com.platform.workflow.registry.events.PlatformEventEnvelope;
import com.platform.workflow.registry.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;

@Service
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void enqueue(PlatformEventEnvelope envelope, String routingKey) {
        try {
            String payload = objectMapper.writeValueAsString(envelope);
            outboxEventRepository.save(new OutboxEvent()
                    .setEventType(envelope.eventType())
                    .setRoutingKey(routingKey)
                    .setPayloadJson(payload)
                    .setIdempotencyKey(envelope.idempotencyKey()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}
