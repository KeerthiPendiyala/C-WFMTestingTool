package com.ukgqtm.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_cycle_scope")
public class UserCycleScope {
    @Id private UUID id;
    @Column(nullable = false) private String tenantId;
    @Column(nullable = false) private UUID userId;
    @Column(nullable = false) private UUID projectId;
    @Column(nullable = false) private UUID testCycleId;
    @Column(nullable = false) private Instant createdAt;
    private UUID createdBy;

    protected UserCycleScope() {}

    public static UserCycleScope create(
            String tenantId, UUID userId, UUID projectId, UUID testCycleId, UUID actorId) {
        UserCycleScope value = new UserCycleScope();
        value.id = UUID.randomUUID();
        value.tenantId = tenantId;
        value.userId = userId;
        value.projectId = projectId;
        value.testCycleId = testCycleId;
        value.createdAt = Instant.now();
        value.createdBy = actorId;
        return value;
    }
}
