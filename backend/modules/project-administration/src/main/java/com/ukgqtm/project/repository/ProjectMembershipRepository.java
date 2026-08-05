package com.ukgqtm.project.repository;

import com.ukgqtm.project.domain.ProjectMembership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectMembershipRepository extends JpaRepository<ProjectMembership, UUID> {
    List<ProjectMembership> findByProjectIdAndDeletedAtIsNull(UUID projectId);

    List<ProjectMembership> findByTenantIdAndProjectIdAndDeletedAtIsNullOrderByProjectRoleAsc(
            String tenantId, UUID projectId);

    List<ProjectMembership> findByTenantIdAndUserIdAndDeletedAtIsNull(String tenantId, UUID userId);

    Optional<ProjectMembership> findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(
            String tenantId, UUID projectId, UUID id);

    Optional<ProjectMembership> findByTenantIdAndProjectIdAndUserIdAndDeletedAtIsNull(
            String tenantId, UUID projectId, UUID userId);

    boolean existsByProjectIdAndUserIdAndMembershipStatusAndDeletedAtIsNull(UUID projectId, UUID userId, String membershipStatus);

    boolean existsByTenantIdAndProjectIdAndUserIdAndMembershipStatusAndDeletedAtIsNull(
            String tenantId, UUID projectId, UUID userId, String membershipStatus);

    @Query("""
            select pm.projectRole
            from ProjectMembership pm
            where pm.tenantId = :tenantId
              and pm.projectId = :projectId
              and pm.userId = :userId
              and pm.membershipStatus = 'ACTIVE'
              and pm.deletedAt is null
            """)
    Optional<String> findActiveRole(
            @Param("tenantId") String tenantId, @Param("projectId") UUID projectId, @Param("userId") UUID userId);

    long countByTenantIdAndProjectIdAndMembershipStatusAndDeletedAtIsNull(
            String tenantId, UUID projectId, String membershipStatus);

    long countByTenantIdAndProjectIdAndProjectRoleAndMembershipStatusAndDeletedAtIsNull(
            String tenantId, UUID projectId, String projectRole, String membershipStatus);
}
