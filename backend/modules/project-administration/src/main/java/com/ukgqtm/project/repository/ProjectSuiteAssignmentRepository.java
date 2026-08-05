package com.ukgqtm.project.repository;

import com.ukgqtm.project.domain.ProjectSuiteAssignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectSuiteAssignmentRepository extends JpaRepository<ProjectSuiteAssignment, UUID> {
    List<ProjectSuiteAssignment> findByProjectIdAndDeletedAtIsNull(UUID projectId);

    List<ProjectSuiteAssignment> findByTenantIdAndProjectIdAndDeletedAtIsNullOrderByIdAsc(String tenantId, UUID projectId);

    Optional<ProjectSuiteAssignment> findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(
            String tenantId, UUID projectId, UUID id);

    Optional<ProjectSuiteAssignment> findByTenantIdAndProjectIdAndSuiteIdAndDeletedAtIsNull(
            String tenantId, UUID projectId, UUID suiteId);

    long countByTenantIdAndSuiteIdAndDeletedAtIsNull(String tenantId, UUID suiteId);

    long countByTenantIdAndProjectIdAndActiveTrueAndDeletedAtIsNull(String tenantId, UUID projectId);
}
