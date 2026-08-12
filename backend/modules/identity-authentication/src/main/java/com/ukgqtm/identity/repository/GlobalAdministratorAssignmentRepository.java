package com.ukgqtm.identity.repository;

import com.ukgqtm.identity.domain.GlobalAdministratorAssignment;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlobalAdministratorAssignmentRepository extends JpaRepository<GlobalAdministratorAssignment, UUID> {
    boolean existsByUserIdAndDeletedAtIsNull(UUID userId);

    Optional<GlobalAdministratorAssignment> findByUserIdAndDeletedAtIsNull(UUID userId);

    boolean existsByDeletedAtIsNull();
}
