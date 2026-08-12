package com.ukgqtm.app.security;

import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.identity.repository.ApplicationUserRepository;
import com.ukgqtm.project.domain.AccessPermission;
import com.ukgqtm.project.repository.UserCycleScopeRepository;
import com.ukgqtm.project.repository.UserProjectPermissionRepository;
import com.ukgqtm.project.repository.UserSuiteScopeRepository;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignmentScopeAuthorizationService {
    private final ApplicationUserRepository users;
    private final UserProjectPermissionRepository projectPermissions;
    private final UserSuiteScopeRepository suiteScopes;
    private final UserCycleScopeRepository cycleScopes;

    public AssignmentScopeAuthorizationService(
            ApplicationUserRepository users,
            UserProjectPermissionRepository projectPermissions,
            UserSuiteScopeRepository suiteScopes,
            UserCycleScopeRepository cycleScopes) {
        this.users = users;
        this.projectPermissions = projectPermissions;
        this.suiteScopes = suiteScopes;
        this.cycleScopes = cycleScopes;
    }

    @Transactional(readOnly = true)
    public boolean canAccess(
            AuthenticatedUser user, UUID projectId, UUID projectSuiteAssignmentId, UUID testCycleId) {
        if (!restricted(user)) {
            return true;
        }
        boolean hasProjectView = projectPermissions
                .findByTenantIdAndUserIdAndProjectId(user.tenantId(), user.userId(), projectId)
                .stream()
                .anyMatch(permission -> AccessPermission.VIEW.name().equals(permission.permissionName()));
        if (hasProjectView) {
            return true;
        }
        return suiteScopes.findAssignmentIds(user.tenantId(), user.userId(), projectId)
                        .contains(projectSuiteAssignmentId)
                && cycleScopes.findCycleIds(user.tenantId(), user.userId(), projectId).contains(testCycleId);
    }

    public void requireAccess(
            AuthenticatedUser user, UUID projectId, UUID projectSuiteAssignmentId, UUID testCycleId) {
        if (!canAccess(user, projectId, projectSuiteAssignmentId, testCycleId)) {
            throw new AccessDeniedException("The requested resource is not available.");
        }
    }

    private boolean restricted(AuthenticatedUser user) {
        return !user.globalAdministrator()
                && users.findById(user.userId()).map(candidate -> candidate.assignmentScoped()).orElse(false);
    }
}
