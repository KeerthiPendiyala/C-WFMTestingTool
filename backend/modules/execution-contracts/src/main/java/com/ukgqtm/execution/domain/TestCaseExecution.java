package com.ukgqtm.execution.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "test_case_execution")
public class TestCaseExecution {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private UUID executionRunId;

    @Column(nullable = false)
    private UUID testCaseId;

    @Column(nullable = false)
    private String featureFlagKey;

    @Column(nullable = false)
    private String status;

    private Instant startedAt;
    private Instant completedAt;

    @Version
    private int version;

    protected TestCaseExecution() {}
}
