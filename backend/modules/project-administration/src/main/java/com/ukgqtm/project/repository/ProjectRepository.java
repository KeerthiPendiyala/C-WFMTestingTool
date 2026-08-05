package com.ukgqtm.project.repository;

import com.ukgqtm.project.domain.Project;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByTenantIdAndActiveTrueAndDeletedAtIsNullOrderByNameAsc(String tenantId);

    Optional<Project> findByTenantIdAndIdAndActiveTrueAndDeletedAtIsNull(String tenantId, UUID id);

    boolean existsByTenantIdAndProjectKeyAndDeletedAtIsNull(String tenantId, String projectKey);

    @Query("""
            select count(p) > 0
            from Project p
            where p.tenantId = :tenantId
              and lower(p.name) = lower(:name)
              and p.deletedAt is null
            """)
    boolean existsActiveName(@Param("tenantId") String tenantId, @Param("name") String name);

    @Query("""
            select distinct p
            from Project p
            join ProjectMembership pm on pm.projectId = p.id
            where p.tenantId = :tenantId
              and p.active = true
              and p.deletedAt is null
              and pm.userId = :userId
              and pm.tenantId = :tenantId
              and pm.membershipStatus = 'ACTIVE'
              and pm.deletedAt is null
            order by p.name asc
            """)
    List<Project> findAssignedActiveProjects(@Param("tenantId") String tenantId, @Param("userId") UUID userId);
}
