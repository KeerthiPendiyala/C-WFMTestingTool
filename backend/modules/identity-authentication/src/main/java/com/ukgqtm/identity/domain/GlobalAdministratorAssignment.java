package com.ukgqtm.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "global_administrator_assignment")
public class GlobalAdministratorAssignment {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private Instant assignedAt;

    private UUID assignedBy;
    private Instant deletedAt;
    private UUID deletedBy;

    @Version
    private int version;

    protected GlobalAdministratorAssignment() {}

    public static GlobalAdministratorAssignment bootstrap(UUID userId) {
        GlobalAdministratorAssignment assignment = new GlobalAdministratorAssignment();
        assignment.id = UUID.randomUUID();
        assignment.userId = userId;
        assignment.assignedAt = Instant.now();
        return assignment;
    }

    public static GlobalAdministratorAssignment assign(UUID userId, UUID actorId) {
        GlobalAdministratorAssignment assignment = bootstrap(userId);
        assignment.assignedBy = actorId;
        return assignment;
    }

    public void revoke(UUID actorId) {
        this.deletedAt = Instant.now();
        this.deletedBy = actorId;
    }
}
