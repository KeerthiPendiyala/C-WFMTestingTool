package com.ukgqtm.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_suite_scope")
public class UserSuiteScope {
    @Id private UUID id;
    @Column(nullable = false) private String tenantId;
    @Column(nullable = false) private UUID userId;
    @Column(nullable = false) private UUID projectId;
    @Column(nullable = false) private UUID projectSuiteAssignmentId;
    @Column(nullable = false) private Instant createdAt;
    private UUID createdBy;

    protected UserSuiteScope() {}

    public static UserSuiteScope create(
            String tenantId, UUID userId, UUID projectId, UUID assignmentId, UUID actorId) {
        UserSuiteScope value = new UserSuiteScope();
        value.id = UUID.randomUUID();
        value.tenantId = tenantId;
        value.userId = userId;
        value.projectId = projectId;
        value.projectSuiteAssignmentId = assignmentId;
        value.createdAt = Instant.now();
        value.createdBy = actorId;
        return value;
    }
}
