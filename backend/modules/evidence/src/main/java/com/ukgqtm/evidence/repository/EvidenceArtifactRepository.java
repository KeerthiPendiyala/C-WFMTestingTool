package com.ukgqtm.evidence.repository;

import com.ukgqtm.evidence.domain.EvidenceArtifact;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceArtifactRepository extends JpaRepository<EvidenceArtifact, UUID> {
    List<EvidenceArtifact> findByProjectIdAndDeletedAtIsNull(UUID projectId);
}
