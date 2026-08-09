package com.ukgqtm.requirements.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "requirement")
public class Requirement {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private UUID projectSuiteAssignmentId;

    @Column(nullable = false)
    private UUID testCycleId;

    @Column(nullable = false)
    private int reqSequence;

    @Column(nullable = false)
    private String reqId;

    @Column(nullable = false)
    private String header;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String acceptanceCriteria;

    @Column(nullable = false)
    private String assumptions;

    @Column(nullable = false)
    private String dependencies;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String sourceType;

    private UUID sourceDocumentId;
    private UUID generationJobId;
    private Instant approvedAt;
    private UUID approvedBy;

    @Column(nullable = false)
    private Instant createdDate;

    private Instant deletedAt;

    @Version
    private int version;

    protected Requirement() {}

    public static Requirement createManual(
            String tenantId,
            UUID projectId,
            UUID projectSuiteAssignmentId,
            UUID testCycleId,
            int reqSequence,
            String header,
            String description) {
        Requirement requirement = new Requirement();
        requirement.id = UUID.randomUUID();
        requirement.tenantId = tenantId;
        requirement.projectId = projectId;
        requirement.projectSuiteAssignmentId = projectSuiteAssignmentId;
        requirement.testCycleId = testCycleId;
        requirement.reqSequence = reqSequence;
        requirement.reqId = "REQ-%03d".formatted(reqSequence);
        requirement.header = header;
        requirement.description = description;
        requirement.acceptanceCriteria = "";
        requirement.assumptions = "";
        requirement.dependencies = "";
        requirement.status = "Draft";
        requirement.sourceType = "MANUAL";
        requirement.createdDate = Instant.now();
        return requirement;
    }

    public static Requirement createGenerated(
            String tenantId,
            UUID projectId,
            UUID projectSuiteAssignmentId,
            UUID testCycleId,
            int reqSequence,
            String header,
            String description,
            String acceptanceCriteria,
            String assumptions,
            String dependencies,
            UUID sourceDocumentId,
            UUID generationJobId) {
        Requirement requirement = createManual(
                tenantId, projectId, projectSuiteAssignmentId, testCycleId, reqSequence, header, description);
        requirement.sourceType = "AI";
        requirement.acceptanceCriteria = acceptanceCriteria;
        requirement.assumptions = assumptions;
        requirement.dependencies = dependencies;
        requirement.sourceDocumentId = sourceDocumentId;
        requirement.generationJobId = generationJobId;
        return requirement;
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

    public UUID projectSuiteAssignmentId() {
        return projectSuiteAssignmentId;
    }

    public UUID testCycleId() {
        return testCycleId;
    }

    public String reqId() {
        return reqId;
    }

    public String header() {
        return header;
    }

    public String description() {
        return description;
    }

    public String acceptanceCriteria() {
        return acceptanceCriteria;
    }

    public String assumptions() {
        return assumptions;
    }

    public String dependencies() {
        return dependencies;
    }

    public String status() {
        return status;
    }

    public String sourceType() {
        return sourceType;
    }

    public Instant approvedAt() {
        return approvedAt;
    }

    public UUID approvedBy() {
        return approvedBy;
    }

    public Instant createdDate() {
        return createdDate;
    }

    public int version() {
        return version;
    }

    public void approve(UUID actorId) {
        if (!"Draft".equals(status)) {
            throw new IllegalStateException("Only Draft requirements can be approved.");
        }
        status = "Approved";
        approvedAt = Instant.now();
        approvedBy = actorId;
    }

    public void updateDetails(
            String header,
            String description,
            String acceptanceCriteria,
            String assumptions,
            String dependencies) {
        this.header = header;
        this.description = description;
        this.acceptanceCriteria = acceptanceCriteria;
        this.assumptions = assumptions;
        this.dependencies = dependencies;
    }

    public void softDelete() {
        deletedAt = Instant.now();
    }
}
