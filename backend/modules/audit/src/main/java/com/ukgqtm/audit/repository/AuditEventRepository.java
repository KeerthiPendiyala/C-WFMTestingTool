package com.ukgqtm.audit.repository;

import com.ukgqtm.audit.domain.AuditEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    List<AuditEvent> findByProjectIdOrderByOccurredAtDesc(UUID projectId);
}
