package com.ukgqtm.project.repository;

import com.ukgqtm.project.domain.AccessRolePermission;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessRolePermissionRepository extends JpaRepository<AccessRolePermission, UUID> {
    List<AccessRolePermission> findByRoleId(UUID roleId);
}
