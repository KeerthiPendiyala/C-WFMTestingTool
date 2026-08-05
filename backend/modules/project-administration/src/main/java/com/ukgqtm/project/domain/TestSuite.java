package com.ukgqtm.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "test_suite")
public class TestSuite {
    @Id
    private UUID id;

    private String tenantId;

    @Column(nullable = false)
    private String suiteKey;

    @Column(nullable = false)
    private String name;

    private String description;
    private boolean active;
    private Instant createdAt;
    private UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;
    private Instant deletedAt;
    private UUID deletedBy;

    @Version
    private int version;

    protected TestSuite() {}

    public static TestSuite create(String tenantId, String suiteKey, String name, String description, UUID actorId) {
        Instant now = Instant.now();
        TestSuite suite = new TestSuite();
        suite.id = UUID.randomUUID();
        suite.tenantId = tenantId;
        suite.suiteKey = suiteKey;
        suite.name = name;
        suite.description = description;
        suite.active = true;
        suite.createdAt = now;
        suite.createdBy = actorId;
        suite.updatedAt = now;
        suite.updatedBy = actorId;
        return suite;
    }

    public UUID id() {
        return id;
    }

    public String tenantId() {
        return tenantId;
    }

    public String suiteKey() {
        return suiteKey;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public boolean active() {
        return active && deletedAt == null;
    }

    public int version() {
        return version;
    }

    public void update(String suiteKey, String name, String description, UUID actorId) {
        this.suiteKey = suiteKey;
        this.name = name;
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
