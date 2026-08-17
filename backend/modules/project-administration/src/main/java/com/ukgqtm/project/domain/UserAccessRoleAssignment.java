package com.ukgqtm.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_access_role_assignment")
public class UserAccessRoleAssignment {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID roleId;

    private Instant updatedAt;
    private UUID updatedBy;

    protected UserAccessRoleAssignment() {}

    public static UserAccessRoleAssignment assign(String tenantId, UUID userId, UUID roleId, UUID actorId) {
        UserAccessRoleAssignment assignment = new UserAccessRoleAssignment();
        assignment.id = UUID.randomUUID();
        assignment.tenantId = tenantId;
        assignment.userId = userId;
        assignment.roleId = roleId;
        assignment.updatedAt = Instant.now();
        assignment.updatedBy = actorId;
        return assignment;
    }

    public void changeRole(UUID roleId, UUID actorId) {
        this.roleId = roleId;
        this.updatedAt = Instant.now();
        this.updatedBy = actorId;
    }

    public String tenantId() {
        return tenantId;
    }

    public UUID userId() {
        return userId;
    }

    public UUID roleId() {
        return roleId;
    }
}
