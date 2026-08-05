package com.ukgqtm.identity.repository;

import com.ukgqtm.identity.domain.ApplicationUser;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ApplicationUserRepository extends JpaRepository<ApplicationUser, UUID> {
    Optional<ApplicationUser> findByNormalizedContactEmailAndDeletedAtIsNull(String normalizedContactEmail);

    List<ApplicationUser> findByDeletedAtIsNullOrderByLastNameAscFirstNameAsc();

    List<ApplicationUser> findByIdInAndDeletedAtIsNull(Collection<UUID> ids);

    Optional<ApplicationUser> findByEntraTenantIdAndEntraObjectIdAndDeletedAtIsNull(String entraTenantId, String entraObjectId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ApplicationUser> findByNormalizedContactEmailInAndDeletedAtIsNull(Collection<String> normalizedContactEmails);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ApplicationUser> findForUpdateByEntraTenantIdAndEntraObjectIdAndDeletedAtIsNull(
            String entraTenantId, String entraObjectId);
}
