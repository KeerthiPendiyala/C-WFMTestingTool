package com.ukgqtm.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "project")
public class Project {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String projectKey;

    @Column(nullable = false)
    private String name;

    private String description;
    private boolean active;
    private Instant createdAt;
    private UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;
    private Instant deletedAt;

    @Version
    private int version;

    protected Project() {}

    public static Project create(String tenantId, String projectKey, String name, String description, UUID actorId) {
        Instant now = Instant.now();
        Project project = new Project();
        project.id = UUID.randomUUID();
        project.tenantId = tenantId;
        project.projectKey = projectKey;
        project.name = name;
        project.description = description;
        project.active = true;
        project.createdAt = now;
        project.createdBy = actorId;
        project.updatedAt = now;
        project.updatedBy = actorId;
        return project;
    }

    public UUID id() {
        return id;
    }

    public String tenantId() {
        return tenantId;
    }

    public String projectKey() {
        return projectKey;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public boolean active() {
        return active;
    }
}
