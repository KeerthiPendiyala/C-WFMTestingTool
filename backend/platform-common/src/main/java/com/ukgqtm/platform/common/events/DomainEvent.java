package com.ukgqtm.platform.common.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DomainEvent(
        UUID eventId,
        String eventType,
        String aggregateId,
        String tenantId,
        String projectId,
        String actorId,
        Instant occurredAt,
        String correlationId,
        String causationId,
        int schemaVersion,
        String idempotencyKey,
        Map<String, Object> payload) {
}

