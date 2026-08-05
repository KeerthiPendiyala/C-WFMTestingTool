package com.ukgqtm.requirements.repository;

import com.ukgqtm.requirements.domain.UploadedDocument;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadedDocumentRepository extends JpaRepository<UploadedDocument, UUID> {
    List<UploadedDocument> findByProjectIdAndDeletedAtIsNull(UUID projectId);
}
