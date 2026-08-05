package com.ukgqtm.evidence.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evidence_artifact_link")
public class EvidenceArtifactLink {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private UUID evidenceArtifactId;

    private UUID requirementId;
    private UUID testCaseId;
    private UUID testCaseExecutionId;

    @Column(nullable = false)
    private Instant createdAt;

    protected EvidenceArtifactLink() {}
}
