package com.ukgqtm.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "generation_job")
public class GenerationJob {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String tenantId;

    private UUID projectId;

    @Column(nullable = false)
    private String jobType;

    @Column(nullable = false)
    private String sourceType;

    @Column(nullable = false)
    private String status;

    private String idempotencyKey;
    private String providerKind;
    private String modelName;
    private String promptHash;
    private UUID sourceDocumentId;
    private UUID requestedBy;
    private String lastError;
    private Instant deletedAt;

    @Version
    private int version;

    protected GenerationJob() {}

    public static GenerationJob succeeded(
            String tenantId,
            UUID projectId,
            String modelName,
            UUID sourceDocumentId,
            UUID requestedBy) {
        GenerationJob job = new GenerationJob();
        job.id = UUID.randomUUID();
        job.tenantId = tenantId;
        job.projectId = projectId;
        job.jobType = "REQUIREMENT_EXTRACTION";
        job.sourceType = "AI";
        job.status = "SUCCEEDED";
        job.providerKind = "OPENAI";
        job.modelName = modelName;
        job.sourceDocumentId = sourceDocumentId;
        job.requestedBy = requestedBy;
        return job;
    }

    public static GenerationJob succeededTestCaseGeneration(
            String tenantId,
            UUID projectId,
            String sourceType,
            String modelName,
            String idempotencyKey,
            UUID requestedBy) {
        GenerationJob job = new GenerationJob();
        job.id = UUID.randomUUID();
        job.tenantId = tenantId;
        job.projectId = projectId;
        job.jobType = "TEST_CASE_GENERATION";
        job.sourceType = sourceType;
        job.status = "SUCCEEDED";
        job.idempotencyKey = idempotencyKey;
        job.providerKind = "AI".equals(sourceType) ? "OPENAI" : null;
        job.modelName = modelName;
        job.requestedBy = requestedBy;
        return job;
    }

    public UUID id() {
        return id;
    }

    public String tenantId() {
        return tenantId;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public UUID projectId() {
        return projectId;
    }
}
