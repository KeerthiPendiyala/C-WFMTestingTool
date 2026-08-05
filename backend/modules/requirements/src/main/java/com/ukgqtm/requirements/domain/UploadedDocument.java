package com.ukgqtm.requirements.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "uploaded_document")
public class UploadedDocument {
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
    private String originalFilename;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long byteSize;

    @Column(nullable = false)
    private String documentStatus;

    @Column(nullable = false)
    private String sourceType;

    private String storageProvider;
    private String objectReference;
    private String contentHash;
    private UUID uploadedBy;
    private Instant deletedAt;

    @Version
    private int version;

    protected UploadedDocument() {}

    public static UploadedDocument create(
            String tenantId,
            UUID projectId,
            UUID projectSuiteAssignmentId,
            UUID testCycleId,
            String originalFilename,
            String contentType,
            long byteSize,
            String sourceType,
            String contentHash,
            UUID uploadedBy) {
        UploadedDocument document = new UploadedDocument();
        document.id = UUID.randomUUID();
        document.tenantId = tenantId;
        document.projectId = projectId;
        document.projectSuiteAssignmentId = projectSuiteAssignmentId;
        document.testCycleId = testCycleId;
        document.originalFilename = originalFilename;
        document.contentType = contentType;
        document.byteSize = byteSize;
        document.documentStatus = "PROCESSED";
        document.sourceType = sourceType;
        document.contentHash = contentHash;
        document.uploadedBy = uploadedBy;
        return document;
    }

    public UUID id() {
        return id;
    }
}
