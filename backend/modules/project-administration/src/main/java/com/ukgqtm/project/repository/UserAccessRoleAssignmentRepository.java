package com.ukgqtm.project.repository;

import com.ukgqtm.project.domain.UserAccessRoleAssignment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccessRoleAssignmentRepository extends JpaRepository<UserAccessRoleAssignment, UUID> {
    Optional<UserAccessRoleAssignment> findByTenantIdAndUserId(String tenantId, UUID userId);
}
