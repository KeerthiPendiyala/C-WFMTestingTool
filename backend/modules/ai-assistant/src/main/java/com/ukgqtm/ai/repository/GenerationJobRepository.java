package com.ukgqtm.ai.repository;

import com.ukgqtm.ai.domain.GenerationJob;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenerationJobRepository extends JpaRepository<GenerationJob, UUID> {
    Optional<GenerationJob> findByIdempotencyKey(String idempotencyKey);

    List<GenerationJob> findByProjectIdAndStatus(UUID projectId, String status);
}
