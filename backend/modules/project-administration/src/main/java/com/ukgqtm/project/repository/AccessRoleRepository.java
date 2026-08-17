package com.ukgqtm.project.repository;

import com.ukgqtm.project.domain.AccessRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessRoleRepository extends JpaRepository<AccessRole, UUID> {
    List<AccessRole> findByTenantIdOrderByNameAsc(String tenantId);

    Optional<AccessRole> findByTenantIdAndId(String tenantId, UUID id);

    Optional<AccessRole> findByTenantIdAndNameIgnoreCase(String tenantId, String name);
}
