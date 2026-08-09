package com.ukgqtm.testmanagement.repository;

import com.ukgqtm.testmanagement.domain.TestCase;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseRepository extends JpaRepository<TestCase, UUID> {
    List<TestCase> findByProjectIdAndDeletedAtIsNull(UUID projectId);

    List<TestCase> findByTenantIdAndProjectIdAndDeletedAtIsNullOrderByCreatedDateDesc(
            String tenantId, UUID projectId);

    List<TestCase> findByGenerationJobIdAndDeletedAtIsNullOrderByCreatedDateDesc(UUID generationJobId);

    Optional<TestCase> findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(String tenantId, UUID projectId, UUID id);

    Optional<TestCase> findByProjectIdAndTestCaseIdAndDeletedAtIsNull(UUID projectId, String testCaseId);

    long countByRequirementIdAndDeletedAtIsNull(UUID requirementId);

    long countByProjectIdAndProjectSuiteAssignmentIdAndDeletedAtIsNull(UUID projectId, UUID projectSuiteAssignmentId);

    long countByProjectIdAndTestCycleIdAndDeletedAtIsNull(UUID projectId, UUID testCycleId);
}
