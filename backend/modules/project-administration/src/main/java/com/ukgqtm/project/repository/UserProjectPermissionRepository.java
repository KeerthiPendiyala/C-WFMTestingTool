package com.ukgqtm.project.repository;

import com.ukgqtm.project.domain.UserProjectPermission;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProjectPermissionRepository extends JpaRepository<UserProjectPermission, UUID> {
    List<UserProjectPermission> findByTenantIdAndUserIdAndProjectId(String tenantId, UUID userId, UUID projectId);
}
