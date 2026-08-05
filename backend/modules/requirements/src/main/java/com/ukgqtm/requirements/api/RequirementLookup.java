package com.ukgqtm.requirements.api;

import java.util.Optional;
import java.util.UUID;

public interface RequirementLookup {
    Optional<RequirementSummary> findRequirement(UUID projectId, UUID requirementId);
}
