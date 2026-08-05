package com.ukgqtm.project.api;

import java.util.UUID;

public interface ProjectScopeLookup {
    boolean isActiveProjectMember(UUID projectId, UUID userId);

    boolean isSuiteAssignedToProject(UUID projectId, UUID projectSuiteAssignmentId);

    boolean isCycleInProject(UUID projectId, UUID testCycleId);
}
