package com.ukgqtm.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_project_permission")
public class UserProjectPermission {
    @Id private UUID id;
    @Column(nullable = false) private String tenantId;
    @Column(nullable = false) private UUID userId;
    @Column(nullable = false) private UUID projectId;
    @Column(nullable = false) private String permissionName;
    @Column(nullable = false) private Instant createdAt;
    private UUID createdBy;

    protected UserProjectPermission() {}

    public static UserProjectPermission create(
            String tenantId, UUID userId, UUID projectId, AccessPermission permission, UUID actorId) {
        UserProjectPermission value = new UserProjectPermission();
        value.id = UUID.randomUUID();
        value.tenantId = tenantId;
        value.userId = userId;
        value.projectId = projectId;
        value.permissionName = permission.name();
        value.createdAt = Instant.now();
        value.createdBy = actorId;
        return value;
    }

    public String permissionName() { return permissionName; }
}
