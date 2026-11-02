package com.platform.workflow.registry.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PlatformEventEnvelope(
        String eventId,
        String eventType,
        Instant occurredAt,
        String correlationId,
        String idempotencyKey,
        Map<String, Object> payload) {

    public static PlatformEventEnvelope create(
            String eventType,
            String correlationId,
            String idempotencyKey,
            Map<String, Object> payload) {
        return new PlatformEventEnvelope(
                UUID.randomUUID().toString(),
                eventType,
                Instant.now(),
                correlationId,
                idempotencyKey,
                payload);
    }
}
