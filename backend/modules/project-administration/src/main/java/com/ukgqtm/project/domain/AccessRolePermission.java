package com.ukgqtm.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "access_role_permission")
public class AccessRolePermission {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID roleId;

    @Column(nullable = false)
    private String permissionName;

    protected AccessRolePermission() {}

    public static AccessRolePermission create(UUID roleId, AccessPermission permission) {
        AccessRolePermission value = new AccessRolePermission();
        value.id = UUID.randomUUID();
        value.roleId = roleId;
        value.permissionName = permission.name();
        return value;
    }

    public UUID roleId() {
        return roleId;
    }

    public String permissionName() {
        return permissionName;
    }
}
