package com.ukgqtm.project.repository;

import com.ukgqtm.project.domain.UserSuiteScope;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSuiteScopeRepository extends JpaRepository<UserSuiteScope, UUID> {
    List<UserSuiteScope> findByTenantIdAndUserId(String tenantId, UUID userId);

    @Query("select s.projectSuiteAssignmentId from UserSuiteScope s where s.tenantId=:tenantId and s.userId=:userId and s.projectId=:projectId")
    List<UUID> findAssignmentIds(@Param("tenantId") String tenantId, @Param("userId") UUID userId, @Param("projectId") UUID projectId);
}
