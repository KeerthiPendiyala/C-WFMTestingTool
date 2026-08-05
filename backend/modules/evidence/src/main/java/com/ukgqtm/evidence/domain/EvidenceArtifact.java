package com.ukgqtm.evidence.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evidence_artifact")
public class EvidenceArtifact {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private String featureFlagKey;

    @Column(nullable = false)
    private String evidenceType;

    @Column(nullable = false)
    private String storageProvider;

    @Column(nullable = false)
    private String objectReference;

    private String contentHash;
    private Instant deletedAt;

    @Version
    private int version;

    protected EvidenceArtifact() {}
}
