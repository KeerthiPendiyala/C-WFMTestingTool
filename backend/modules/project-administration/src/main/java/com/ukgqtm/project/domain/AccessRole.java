package com.ukgqtm.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "access_role")
public class AccessRole {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private boolean administratorRole;

    private Instant updatedAt;
    private UUID updatedBy;

    @Version
    private int version;

    protected AccessRole() {}

    public static AccessRole create(
            String tenantId, String name, String description, boolean administratorRole, UUID actorId) {
        AccessRole role = new AccessRole();
        role.id = UUID.randomUUID();
        role.tenantId = tenantId;
        role.name = name;
        role.description = description;
        role.administratorRole = administratorRole;
        role.updatedAt = Instant.now();
        role.updatedBy = actorId;
        return role;
    }

    public void update(String name, String description, UUID actorId) {
        this.name = name;
        this.description = description;
        this.updatedAt = Instant.now();
        this.updatedBy = actorId;
    }

    public UUID id() {
        return id;
    }

    public String tenantId() {
        return tenantId;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public boolean administratorRole() {
        return administratorRole;
    }

    public int version() {
        return version;
    }
}
