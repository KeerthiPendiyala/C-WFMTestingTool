package com.ukgqtm.requirements.repository;

import com.ukgqtm.requirements.domain.Requirement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequirementRepository extends JpaRepository<Requirement, UUID> {
    List<Requirement> findByProjectIdAndDeletedAtIsNull(UUID projectId);

    List<Requirement> findByTenantIdAndProjectIdAndDeletedAtIsNullOrderByCreatedDateDesc(
            String tenantId, UUID projectId);

    Optional<Requirement> findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(
            String tenantId, UUID projectId, UUID id);

    Optional<Requirement> findByProjectIdAndReqIdAndDeletedAtIsNull(UUID projectId, String reqId);

    long countByProjectIdAndProjectSuiteAssignmentIdAndDeletedAtIsNull(UUID projectId, UUID projectSuiteAssignmentId);

    long countByProjectIdAndTestCycleIdAndDeletedAtIsNull(UUID projectId, UUID testCycleId);
}
