package com.ukgqtm.testmanagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "predefined_test_case_template")
public class PredefinedTestCaseTemplate {
    @Id
    private UUID id;

    private String tenantId;
    private UUID suiteId;

    @Column(nullable = false)
    private String templateKey;

    @Column(nullable = false)
    private String header;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String source;

    private boolean active;
    private Instant createdAt;
    private UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;
    private Instant deletedAt;
    private UUID deletedBy;

    @Version
    private int version;

    protected PredefinedTestCaseTemplate() {}

    public static PredefinedTestCaseTemplate create(
            String tenantId, UUID suiteId, String templateKey, String header, String description, UUID actorId) {
        Instant now = Instant.now();
        PredefinedTestCaseTemplate template = new PredefinedTestCaseTemplate();
        template.id = UUID.randomUUID();
        template.tenantId = tenantId;
        template.suiteId = suiteId;
        template.templateKey = templateKey;
        template.header = header;
        template.description = description;
        template.source = "MANUAL";
        template.active = true;
        template.createdAt = now;
        template.createdBy = actorId;
        template.updatedAt = now;
        template.updatedBy = actorId;
        return template;
    }

    public UUID id() {
        return id;
    }

    public String tenantId() {
        return tenantId;
    }

    public UUID suiteId() {
        return suiteId;
    }

    public String templateKey() {
        return templateKey;
    }

    public String header() {
        return header;
    }

    public String description() {
        return description;
    }

    public String source() {
        return source;
    }

    public boolean active() {
        return active && deletedAt == null;
    }

    public int version() {
        return version;
    }

    public void update(String templateKey, String header, String description, UUID actorId) {
        this.templateKey = templateKey;
        this.header = header;
        this.description = description;
        this.updatedAt = Instant.now();
        this.updatedBy = actorId;
    }

    public void softDelete(UUID actorId) {
        Instant now = Instant.now();
        this.active = false;
        this.deletedAt = now;
        this.deletedBy = actorId;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }
}
