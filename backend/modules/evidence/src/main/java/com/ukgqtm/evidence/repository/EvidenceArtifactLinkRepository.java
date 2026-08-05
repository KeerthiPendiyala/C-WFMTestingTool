package com.ukgqtm.evidence.repository;

import com.ukgqtm.evidence.domain.EvidenceArtifactLink;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceArtifactLinkRepository extends JpaRepository<EvidenceArtifactLink, UUID> {
    List<EvidenceArtifactLink> findByEvidenceArtifactId(UUID evidenceArtifactId);
}
