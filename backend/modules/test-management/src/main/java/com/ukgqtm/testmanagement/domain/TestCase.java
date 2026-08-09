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

    public static TestCase createRequirementLinked(
            String tenantId,
            UUID projectId,
            UUID requirementId,
            int testCaseSequence,
            UUID projectSuiteAssignmentId,
            UUID testCycleId,
            UUID assigneeMembershipId,
            String header,
            String description,
            LocalDate dueDate,
            String sourceType,
            UUID generationJobId) {
        TestCase testCase = new TestCase();
        testCase.id = UUID.randomUUID();
        testCase.tenantId = tenantId;
        testCase.projectId = projectId;
        testCase.requirementId = requirementId;
        testCase.testCaseSequence = testCaseSequence;
        testCase.testCaseId = "TC-%03d".formatted(testCaseSequence);
        testCase.projectSuiteAssignmentId = projectSuiteAssignmentId;
        testCase.testCycleId = testCycleId;
        testCase.assigneeMembershipId = assigneeMembershipId;
        testCase.header = header;
        testCase.description = description;
        testCase.status = "Draft";
        testCase.createdDate = Instant.now();
        testCase.dueDate = dueDate;
        testCase.sourceType = sourceType;
        testCase.generationJobId = generationJobId;
        return testCase;
    }

    public static TestCase createAdhoc(
            String tenantId,
            UUID projectId,
            int testCaseSequence,
            UUID projectSuiteAssignmentId,
            UUID testCycleId,
            UUID assigneeMembershipId,
            String header,
            String description,
            LocalDate dueDate,
            String sourceType,
            UUID generationJobId) {
        TestCase testCase = new TestCase();
        testCase.id = UUID.randomUUID();
        testCase.tenantId = tenantId;
        testCase.projectId = projectId;
        testCase.requirementId = null;
        testCase.testCaseSequence = testCaseSequence;
        testCase.testCaseId = "TC-%03d".formatted(testCaseSequence);
        testCase.projectSuiteAssignmentId = projectSuiteAssignmentId;
        testCase.testCycleId = testCycleId;
        testCase.assigneeMembershipId = assigneeMembershipId;
        testCase.header = header;
        testCase.description = description;
        testCase.status = "Draft";
        testCase.createdDate = Instant.now();
        testCase.dueDate = dueDate;
        testCase.sourceType = sourceType;
        testCase.generationJobId = generationJobId;
        return testCase;
    }

    public UUID id() {
        return id;
    }

    public String tenantId() {
        return tenantId;
    }

    public UUID projectId() {
        return projectId;
    }

    public UUID requirementId() {
        return requirementId;
    }

    public String testCaseId() {
        return testCaseId;
    }

    public UUID projectSuiteAssignmentId() {
        return projectSuiteAssignmentId;
    }

    public UUID testCycleId() {
        return testCycleId;
    }

    public UUID assigneeMembershipId() {
        return assigneeMembershipId;
    }

    public String header() {
        return header;
    }

    public String description() {
        return description;
    }

    public String status() {
        return status;
    }

    public Instant createdDate() {
        return createdDate;
    }

    public LocalDate dueDate() {
        return dueDate;
    }

    public String sourceType() {
        return sourceType;
    }

    public int version() {
        return version;
    }

    public void update(
            String header,
            String description,
            UUID assigneeMembershipId,
            LocalDate dueDate,
            String status) {
        this.header = header;
        this.description = description;
        this.assigneeMembershipId = assigneeMembershipId;
        this.dueDate = dueDate;
        this.status = status;
    }

    public void softDelete() {
        if (!"Draft".equals(status)) {
            throw new IllegalStateException("Test case can be deleted only while Draft.");
        }
        deletedAt = Instant.now();
    }
}
