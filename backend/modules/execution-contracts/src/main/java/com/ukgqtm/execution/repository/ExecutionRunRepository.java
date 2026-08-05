package com.ukgqtm.execution.repository;

import com.ukgqtm.execution.domain.ExecutionRun;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionRunRepository extends JpaRepository<ExecutionRun, UUID> {
    Optional<ExecutionRun> findByIdempotencyKey(String idempotencyKey);

    List<ExecutionRun> findByProjectIdAndDeletedAtIsNull(UUID projectId);
}
