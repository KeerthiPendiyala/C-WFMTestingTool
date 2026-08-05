package com.ukgqtm.app.security;

import com.ukgqtm.audit.domain.AuditEvent;
import com.ukgqtm.audit.repository.AuditEventRepository;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.identity.repository.ApplicationUserRepository;
import com.ukgqtm.project.domain.AccessPermission;
import com.ukgqtm.project.domain.ProjectRole;
import com.ukgqtm.project.repository.ProjectMembershipRepository;
import com.ukgqtm.project.repository.UserProjectPermissionRepository;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepositoryBackedAuthorizationPolicyService implements AuthorizationPolicyService {
    private static final Set<AuthorizationPolicy> ADMIN_POLICIES = EnumSet.allOf(AuthorizationPolicy.class);
    private static final Set<AuthorizationPolicy> MEMBER_POLICIES = EnumSet.of(
            AuthorizationPolicy.PROJECT_VIEW,
            AuthorizationPolicy.REQUIREMENT_CREATE,
            AuthorizationPolicy.REQUIREMENT_DELETE_UNLINKED,
            AuthorizationPolicy.TEST_CASE_CREATE,
            AuthorizationPolicy.TEST_CASE_ASSIGN,
            AuthorizationPolicy.TEST_CASE_DELETE_DRAFT,
            AuthorizationPolicy.TEST_CASE_VIEW_EXPORT,
            AuthorizationPolicy.REPORT_VIEW,
            AuthorizationPolicy.UPLOAD_ACCESS,
            AuthorizationPolicy.GENERATION_JOB_ACCESS,
            AuthorizationPolicy.EXPORT_DOWNLOAD,
            AuthorizationPolicy.AUDIT_VIEW,
            AuthorizationPolicy.EVIDENCE_ACCESS);
    private static final Set<AuthorizationPolicy> TEST_MANAGER_EXTRA_POLICIES = EnumSet.of(
            AuthorizationPolicy.PROJECT_MANAGE_USERS,
            AuthorizationPolicy.PROJECT_MANAGE_SUITES,
            AuthorizationPolicy.PROJECT_MANAGE_CYCLES,
            AuthorizationPolicy.REQUIREMENT_APPROVE,
            AuthorizationPolicy.PREDEFINED_CASE_GENERATE,
            AuthorizationPolicy.PREDEFINED_CASE_DELETE);

    private final ProjectMembershipRepository projectMemberships;
    private final ApplicationUserRepository users;
    private final UserProjectPermissionRepository projectPermissions;
    private final AuditEventRepository auditEvents;

    public RepositoryBackedAuthorizationPolicyService(
            ProjectMembershipRepository projectMemberships,
            ApplicationUserRepository users,
            UserProjectPermissionRepository projectPermissions,
            AuditEventRepository auditEvents) {
        this.projectMemberships = projectMemberships;
        this.users = users;
        this.projectPermissions = projectPermissions;
        this.auditEvents = auditEvents;
    }

    @Override
    public Set<AuthorizationPolicy> globalCapabilities(AuthenticatedUser user) {
        if (user.globalAdministrator()) {
            return EnumSet.copyOf(ADMIN_POLICIES);
        }
        return EnumSet.noneOf(AuthorizationPolicy.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<AuthorizationPolicy> projectCapabilities(AuthenticatedUser user, UUID projectId) {
        if (user.globalAdministrator()) {
            return EnumSet.copyOf(ADMIN_POLICIES);
        }

        Optional<String> role = projectMemberships.findActiveRole(user.tenantId(), projectId, user.userId());
        if (role.isEmpty()) {
            return EnumSet.noneOf(AuthorizationPolicy.class);
        }

        EnumSet<AuthorizationPolicy> policies = EnumSet.copyOf(MEMBER_POLICIES);
        if (ProjectRole.TEST_MANAGER.hasDatabaseValue(role.get())) {
            policies.addAll(TEST_MANAGER_EXTRA_POLICIES);
        }
        if (users.findById(user.userId()).map(candidate -> candidate.assignmentScoped()).orElse(false)) {
            Set<AccessPermission> assigned = projectPermissions
                    .findByTenantIdAndUserIdAndProjectId(user.tenantId(), user.userId(), projectId)
                    .stream()
                    .map(value -> AccessPermission.valueOf(value.permissionName()))
                    .collect(java.util.stream.Collectors.toSet());
            policies.retainAll(policiesFor(assigned));
        }
        return policies;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAllowed(AuthenticatedUser user, AuthorizationPolicy policy, UUID projectId) {
        if (user.globalAdministrator()) {
            return true;
        }
        if (policy == AuthorizationPolicy.PROJECT_CREATE) {
            return false;
        }
        if (projectId == null) {
            return false;
        }
        return projectCapabilities(user, projectId).contains(policy);
    }

    @Override
    public boolean canCreateProject(Authentication authentication) {
        return principal(authentication).map(AuthenticatedUser::globalAdministrator).orElse(false);
    }

    @Override
    public boolean canViewProject(Authentication authentication, UUID projectId) {
        return principal(authentication)
                .map(user -> isAllowed(user, AuthorizationPolicy.PROJECT_VIEW, projectId))
                .orElse(false);
    }

    @Override
    public boolean canManageProjectUsers(Authentication authentication, UUID projectId) {
        return principal(authentication)
                .map(user -> isAllowed(user, AuthorizationPolicy.PROJECT_MANAGE_USERS, projectId))
                .orElse(false);
    }

    @Override
    public boolean canManageSuites(Authentication authentication, UUID projectId) {
        return principal(authentication)
                .map(user -> isAllowed(user, AuthorizationPolicy.PROJECT_MANAGE_SUITES, projectId))
                .orElse(false);
    }

    @Override
    public boolean canManageCycles(Authentication authentication, UUID projectId) {
        return principal(authentication)
                .map(user -> isAllowed(user, AuthorizationPolicy.PROJECT_MANAGE_CYCLES, projectId))
                .orElse(false);
    }

    @Override
    @Transactional
    public void require(
            Authentication authentication,
            AuthorizationPolicy policy,
            UUID projectId,
            String resourceType,
            String resourceId,
            String correlationId) {
        AuthenticatedUser user = principal(authentication)
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
        if (isAllowed(user, policy, projectId)) {
            return;
        }
        auditEvents.save(AuditEvent.authorizationDenied(
                user.userId().toString(), user.tenantId(), projectId, resourceType, resourceId, policy.name(), correlationId));
        throw new AccessDeniedException("The requested resource is not available.");
    }

    private static Optional<AuthenticatedUser> principal(Authentication authentication) {
        if (authentication instanceof ApplicationUserAuthenticationToken token) {
            return Optional.of(token.getPrincipal());
        }
        return Optional.empty();
    }

    private static Set<AuthorizationPolicy> policiesFor(Set<AccessPermission> permissions) {
        EnumSet<AuthorizationPolicy> result = EnumSet.noneOf(AuthorizationPolicy.class);
        if (permissions.contains(AccessPermission.VIEW)) {
            result.addAll(EnumSet.of(
                    AuthorizationPolicy.PROJECT_VIEW,
                    AuthorizationPolicy.TEST_CASE_VIEW_EXPORT,
                    AuthorizationPolicy.REPORT_VIEW,
                    AuthorizationPolicy.EXPORT_DOWNLOAD,
                    AuthorizationPolicy.AUDIT_VIEW,
                    AuthorizationPolicy.EVIDENCE_ACCESS));
        }
        if (permissions.contains(AccessPermission.CREATE)) {
            result.addAll(EnumSet.of(
                    AuthorizationPolicy.REQUIREMENT_CREATE,
                    AuthorizationPolicy.TEST_CASE_CREATE,
                    AuthorizationPolicy.UPLOAD_ACCESS,
                    AuthorizationPolicy.GENERATION_JOB_ACCESS));
        }
        if (permissions.contains(AccessPermission.EDIT)) {
            result.add(AuthorizationPolicy.REQUIREMENT_APPROVE);
        }
        if (permissions.contains(AccessPermission.EXECUTE)) {
            result.add(AuthorizationPolicy.PREDEFINED_CASE_GENERATE);
        }
        if (permissions.contains(AccessPermission.DELETE)) {
            result.addAll(EnumSet.of(
                    AuthorizationPolicy.REQUIREMENT_DELETE_UNLINKED,
                    AuthorizationPolicy.TEST_CASE_DELETE_DRAFT,
                    AuthorizationPolicy.PREDEFINED_CASE_DELETE));
        }
        if (permissions.contains(AccessPermission.MANAGE_ASSIGNMENTS)) {
            result.addAll(EnumSet.of(
                    AuthorizationPolicy.PROJECT_MANAGE_USERS,
                    AuthorizationPolicy.PROJECT_MANAGE_SUITES,
                    AuthorizationPolicy.PROJECT_MANAGE_CYCLES,
                    AuthorizationPolicy.TEST_CASE_ASSIGN));
        }
        return result;
    }
}
