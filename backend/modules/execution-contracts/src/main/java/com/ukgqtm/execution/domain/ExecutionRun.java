package com.ukgqtm.execution.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "execution_run")
public class ExecutionRun {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private String featureFlagKey;

    @Column(nullable = false)
    private String status;

    private UUID requestedBy;
    private String idempotencyKey;
    private Instant deletedAt;

    @Version
    private int version;

    protected ExecutionRun() {}
}
