package com.ukgqtm.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_event")
public class AuditEvent {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String eventType;

    private String actorId;
    private String tenantId;
    private UUID projectId;
    private String resourceType;
    private String resourceId;

    @Column(nullable = false)
    private String action;

    private String correlationId;

    @Column(nullable = false)
    private Instant occurredAt;

    @Version
    private int version;

    protected AuditEvent() {}

    public static AuditEvent authentication(
            String action, String actorId, String tenantId, String resourceId, String correlationId) {
        AuditEvent event = new AuditEvent();
        event.id = UUID.randomUUID();
        event.eventType = "AUTHENTICATION";
        event.actorId = actorId;
        event.tenantId = tenantId;
        event.resourceType = "APPLICATION_USER";
        event.resourceId = resourceId;
        event.action = action;
        event.correlationId = correlationId;
        event.occurredAt = Instant.now();
        return event;
    }

    public static AuditEvent authorizationDenied(
            String actorId,
            String tenantId,
            UUID projectId,
            String resourceType,
            String resourceId,
            String policy,
            String correlationId) {
        AuditEvent event = new AuditEvent();
        event.id = UUID.randomUUID();
        event.eventType = "AUTHORIZATION";
        event.actorId = actorId;
        event.tenantId = tenantId;
        event.projectId = projectId;
        event.resourceType = resourceType;
        event.resourceId = resourceId;
        event.action = "DENIED_" + policy;
        event.correlationId = correlationId;
        event.occurredAt = Instant.now();
        return event;
    }

    public static AuditEvent project(
            String action,
            String actorId,
            String tenantId,
            UUID projectId,
            String resourceType,
            String resourceId,
            String correlationId) {
        AuditEvent event = new AuditEvent();
        event.id = UUID.randomUUID();
        event.eventType = "PROJECT";
        event.actorId = actorId;
        event.tenantId = tenantId;
        event.projectId = projectId;
        event.resourceType = resourceType;
        event.resourceId = resourceId;
        event.action = action;
        event.correlationId = correlationId;
        event.occurredAt = Instant.now();
        return event;
    }
}
