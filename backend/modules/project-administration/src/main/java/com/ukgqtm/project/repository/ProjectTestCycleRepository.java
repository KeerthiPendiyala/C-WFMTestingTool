package com.ukgqtm.project.repository;

import com.ukgqtm.project.domain.ProjectTestCycle;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectTestCycleRepository extends JpaRepository<ProjectTestCycle, UUID> {
    List<ProjectTestCycle> findByProjectIdAndDeletedAtIsNull(UUID projectId);

    List<ProjectTestCycle> findByTenantIdAndProjectIdAndDeletedAtIsNullOrderByStartDateAscNameAsc(String tenantId, UUID projectId);

    Optional<ProjectTestCycle> findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(
            String tenantId, UUID projectId, UUID id);

    @Query("""
            select count(c) > 0
            from ProjectTestCycle c
            where c.tenantId = :tenantId
              and c.projectId = :projectId
              and lower(c.name) = lower(:name)
              and c.deletedAt is null
            """)
    boolean existsActiveName(
            @Param("tenantId") String tenantId, @Param("projectId") UUID projectId, @Param("name") String name);

    @Query("""
            select count(c) > 0
            from ProjectTestCycle c
            where c.tenantId = :tenantId
              and c.projectId = :projectId
              and lower(c.name) = lower(:name)
              and c.id <> :excludedId
              and c.deletedAt is null
            """)
    boolean existsActiveNameExcluding(
            @Param("tenantId") String tenantId,
            @Param("projectId") UUID projectId,
            @Param("name") String name,
            @Param("excludedId") UUID excludedId);

    long countByTenantIdAndProjectIdAndActiveTrueAndDeletedAtIsNull(String tenantId, UUID projectId);
}
