package com.ukgqtm.identity.repository;

import com.ukgqtm.identity.domain.ApprovedTenant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovedTenantRepository extends JpaRepository<ApprovedTenant, String> {
    boolean existsByEntraTenantIdAndActive(String entraTenantId, boolean active);
}
