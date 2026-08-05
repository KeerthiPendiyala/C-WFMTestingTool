package com.ukgqtm.identity.api;

import java.util.Optional;
import java.util.UUID;

public interface UserLookup {
    Optional<UserSummary> findActiveUser(UUID userId);
}
