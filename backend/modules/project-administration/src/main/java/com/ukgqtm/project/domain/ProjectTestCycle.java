package com.ukgqtm.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "project_test_cycle")
public class ProjectTestCycle {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private String name;

    private LocalDate startDate;
    private LocalDate endDate;
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

    protected ProjectTestCycle() {}

    public static ProjectTestCycle create(
            String tenantId,
            UUID projectId,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            String description,
            UUID actorId) {
        Instant now = Instant.now();
        ProjectTestCycle cycle = new ProjectTestCycle();
        cycle.id = UUID.randomUUID();
        cycle.tenantId = tenantId;
        cycle.projectId = projectId;
        cycle.name = name;
        cycle.startDate = startDate;
        cycle.endDate = endDate;
        cycle.description = description;
        cycle.active = true;
        cycle.createdAt = now;
        cycle.createdBy = actorId;
        cycle.updatedAt = now;
        cycle.updatedBy = actorId;
        return cycle;
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

    public String name() {
        return name;
    }

    public LocalDate startDate() {
        return startDate;
    }

    public LocalDate endDate() {
        return endDate;
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

    public void update(String name, LocalDate startDate, LocalDate endDate, String description, UUID actorId) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
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
