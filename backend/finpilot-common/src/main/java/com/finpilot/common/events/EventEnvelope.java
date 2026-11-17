package com.finpilot.common.events;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        UUID correlationId,
        T payload
) {
    public static <T> EventEnvelope<T> of(String eventType, UUID correlationId, T payload) {
        return new EventEnvelope<>(UUID.randomUUID(), eventType, Instant.now(), correlationId, payload);
    }

    public static EventEnvelope<Map<String, Object>> fromMap(String eventType, Map<String, Object> payload) {
        return of(eventType, UUID.randomUUID(), payload);
    }
}