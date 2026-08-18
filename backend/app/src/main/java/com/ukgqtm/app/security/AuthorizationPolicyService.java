package com.ukgqtm.app.security;

import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.project.domain.AccessPermission;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AuthorizationPolicyService {
    Set<AuthorizationPolicy> globalCapabilities(AuthenticatedUser user);

    Set<AccessPermission> effectivePermissions(AuthenticatedUser user);

    String assignedRoleName(AuthenticatedUser user);

    Set<AuthorizationPolicy> projectCapabilities(AuthenticatedUser user, UUID projectId);

    Set<AccessPermission> projectPermissions(AuthenticatedUser user, UUID projectId);

    Map<UUID, Set<AccessPermission>> assignedProjectPermissions(AuthenticatedUser user);

    boolean isAllowed(AuthenticatedUser user, AuthorizationPolicy policy, UUID projectId);

    boolean canCreateProject(Authentication authentication);

    boolean canViewProject(Authentication authentication, UUID projectId);

    boolean canManageProjectUsers(Authentication authentication, UUID projectId);

    boolean canManageSuites(Authentication authentication, UUID projectId);

    boolean canManageCycles(Authentication authentication, UUID projectId);

    void require(
            Authentication authentication,
            AuthorizationPolicy policy,
            UUID projectId,
            String resourceType,
            String resourceId,
            String correlationId);

    void requireGlobalOrAnyProjectPermission(
            Authentication authentication,
            AccessPermission permission,
            String resourceType,
            String resourceId,
            String correlationId);
}
