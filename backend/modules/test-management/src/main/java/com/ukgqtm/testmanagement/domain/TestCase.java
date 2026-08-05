package com.ukgqtm.testmanagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "test_case")
public class TestCase {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private UUID projectId;

    private UUID requirementId;

    @Column(nullable = false)
    private int testCaseSequence;

    @Column(nullable = false)
    private String testCaseId;

    @Column(nullable = false)
    private UUID projectSuiteAssignmentId;

    @Column(nullable = false)
    private UUID testCycleId;

    private UUID assigneeMembershipId;

    @Column(nullable = false)
    private String header;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Instant createdDate;

    private LocalDate dueDate;

    @Column(nullable = false)
    private String sourceType;

    private UUID predefinedTemplateId;
    private UUID generationJobId;
    private Instant deletedAt;

    @Version
    private int version;

    protected TestCase() {}
}
