package com.ukgqtm.execution.repository;

import com.ukgqtm.execution.domain.TestCaseExecution;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseExecutionRepository extends JpaRepository<TestCaseExecution, UUID> {
    List<TestCaseExecution> findByExecutionRunId(UUID executionRunId);
}
