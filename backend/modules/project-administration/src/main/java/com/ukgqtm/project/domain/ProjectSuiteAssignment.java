package com.ukgqtm.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "project_suite_assignment")
public class ProjectSuiteAssignment {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private UUID suiteId;

    private int displayOrder;
    private boolean active;
    private Instant createdAt;
    private UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;
    private Instant deletedAt;
    private UUID deletedBy;

    @Version
    private int version;

    protected ProjectSuiteAssignment() {}

    public static ProjectSuiteAssignment create(String tenantId, UUID projectId, UUID suiteId, UUID actorId) {
        Instant now = Instant.now();
        ProjectSuiteAssignment assignment = new ProjectSuiteAssignment();
        assignment.id = UUID.randomUUID();
        assignment.tenantId = tenantId;
        assignment.projectId = projectId;
        assignment.suiteId = suiteId;
        assignment.displayOrder = 0;
        assignment.active = true;
        assignment.createdAt = now;
        assignment.createdBy = actorId;
        assignment.updatedAt = now;
        assignment.updatedBy = actorId;
        return assignment;
    }

    public UUID id() {
        return id;
    }

    public String tenantId() {
        return tenantId;
    }

    public UUID projectId() {
        return projectId;
    }

    public UUID suiteId() {
        return suiteId;
    }

    public boolean active() {
        return active && deletedAt == null;
    }

    public int version() {
        return version;
    }

    public void unassign(UUID actorId) {
        Instant now = Instant.now();
        this.active = false;
        this.deletedAt = now;
        this.deletedBy = actorId;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }
}
