package com.ukgqtm.project.repository;

import com.ukgqtm.project.domain.UserCycleScope;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserCycleScopeRepository extends JpaRepository<UserCycleScope, UUID> {
    @Query("select s.testCycleId from UserCycleScope s where s.tenantId=:tenantId and s.userId=:userId and s.projectId=:projectId")
    List<UUID> findCycleIds(@Param("tenantId") String tenantId, @Param("userId") UUID userId, @Param("projectId") UUID projectId);
}
