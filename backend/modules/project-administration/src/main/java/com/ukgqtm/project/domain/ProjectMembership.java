package com.ukgqtm.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "project_membership")
public class ProjectMembership {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String projectRole;

    @Column(nullable = false)
    private String membershipStatus;

    private Instant assignedAt;
    private UUID assignedBy;
    private Instant createdAt;
    private UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;
    private Instant deletedAt;
    private UUID deletedBy;

    @Version
    private int version;

    protected ProjectMembership() {}

    public static ProjectMembership create(
            String tenantId, UUID projectId, UUID userId, ProjectRole projectRole, UUID actorId) {
        Instant now = Instant.now();
        ProjectMembership membership = new ProjectMembership();
        membership.id = UUID.randomUUID();
        membership.tenantId = tenantId;
        membership.projectId = projectId;
        membership.userId = userId;
        membership.projectRole = projectRole.databaseValue();
        membership.membershipStatus = "ACTIVE";
        membership.assignedAt = now;
        membership.assignedBy = actorId;
        membership.createdAt = now;
        membership.createdBy = actorId;
        membership.updatedAt = now;
        membership.updatedBy = actorId;
        return membership;
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

    public UUID userId() {
        return userId;
    }

    public String projectRole() {
        return projectRole;
    }

    public String membershipStatus() {
        return membershipStatus;
    }

    public boolean active() {
        return "ACTIVE".equals(membershipStatus) && deletedAt == null;
    }

    public void changeRole(ProjectRole role, UUID actorId) {
        this.projectRole = role.databaseValue();
        this.membershipStatus = "ACTIVE";
        this.updatedAt = Instant.now();
        this.updatedBy = actorId;
    }

    public void disable(UUID actorId) {
        Instant now = Instant.now();
        this.membershipStatus = "DISABLED";
        this.deletedAt = now;
        this.deletedBy = actorId;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }
}
