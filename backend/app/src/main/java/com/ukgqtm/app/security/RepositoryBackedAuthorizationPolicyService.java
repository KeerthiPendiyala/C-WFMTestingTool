package com.ukgqtm.app.security;

import com.ukgqtm.audit.domain.AuditEvent;
import com.ukgqtm.audit.repository.AuditEventRepository;
import com.ukgqtm.app.role.RoleApplicationService;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.identity.repository.ApplicationUserRepository;
import com.ukgqtm.project.domain.AccessPermission;
import com.ukgqtm.project.domain.ProjectRole;
import com.ukgqtm.project.repository.ProjectMembershipRepository;
import com.ukgqtm.project.repository.UserProjectPermissionRepository;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepositoryBackedAuthorizationPolicyService implements AuthorizationPolicyService {
    private static final Set<AuthorizationPolicy> MEMBER_POLICIES = EnumSet.of(
            AuthorizationPolicy.PROJECT_VIEW,
            AuthorizationPolicy.REQUIREMENT_CREATE,
            AuthorizationPolicy.REQUIREMENT_EDIT,
            AuthorizationPolicy.REQUIREMENT_DELETE_UNLINKED,
            AuthorizationPolicy.TEST_CASE_CREATE,
            AuthorizationPolicy.TEST_CASE_EDIT,
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
            AuthorizationPolicy.SUITE_CREATE,
            AuthorizationPolicy.SUITE_EDIT,
            AuthorizationPolicy.SUITE_DELETE,
            AuthorizationPolicy.SUITE_MANAGE_ASSIGNMENTS,
            AuthorizationPolicy.CYCLE_CREATE,
            AuthorizationPolicy.CYCLE_EDIT,
            AuthorizationPolicy.CYCLE_DELETE,
            AuthorizationPolicy.REQUIREMENT_APPROVE,
            AuthorizationPolicy.PREDEFINED_CASE_GENERATE,
            AuthorizationPolicy.PREDEFINED_CASE_DELETE);

    private final ProjectMembershipRepository projectMemberships;
    private final ApplicationUserRepository users;
    private final UserProjectPermissionRepository projectPermissions;
    private final AuditEventRepository auditEvents;
    private final RoleApplicationService roles;

    public RepositoryBackedAuthorizationPolicyService(
            ProjectMembershipRepository projectMemberships,
            ApplicationUserRepository users,
            UserProjectPermissionRepository projectPermissions,
            AuditEventRepository auditEvents,
            RoleApplicationService roles) {
        this.projectMemberships = projectMemberships;
        this.users = users;
        this.projectPermissions = projectPermissions;
        this.auditEvents = auditEvents;
        this.roles = roles;
    }

    @Override
    public Set<AuthorizationPolicy> globalCapabilities(AuthenticatedUser user) {
        if (user.globalAdministrator()) {
            EnumSet<AuthorizationPolicy> policies = EnumSet.noneOf(AuthorizationPolicy.class);
            Set<AccessPermission> permissions = effectivePermissions(user);
            policies.add(AuthorizationPolicy.USER_ACCESS_MANAGE);
            policies.addAll(policiesFor(permissions));
            if (permissions.contains(AccessPermission.CREATE)) {
                policies.add(AuthorizationPolicy.PROJECT_CREATE);
            }
            return policies;
        }
        return EnumSet.noneOf(AuthorizationPolicy.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<AccessPermission> effectivePermissions(AuthenticatedUser user) {
        return roles.roleForUser(user.tenantId(), user.userId())
                .map(role -> roles.permissionsForRole(role.id()))
                .orElseGet(() -> user.globalAdministrator()
                        ? EnumSet.allOf(AccessPermission.class)
                        : EnumSet.noneOf(AccessPermission.class));
    }

    @Override
    @Transactional(readOnly = true)
    public String assignedRoleName(AuthenticatedUser user) {
        return roles.roleForUser(user.tenantId(), user.userId())
                .map(role -> role.name())
                .orElse(user.globalAdministrator() ? "Admin" : "Project Member");
    }

    @Override
    @Transactional(readOnly = true)
    public Set<AuthorizationPolicy> projectCapabilities(AuthenticatedUser user, UUID projectId) {
        if (user.globalAdministrator()) {
            return policiesFor(effectivePermissions(user));
        }

        Optional<String> role = projectMemberships.findActiveRole(user.tenantId(), projectId, user.userId());
        if (role.isEmpty()) {
            return EnumSet.noneOf(AuthorizationPolicy.class);
        }

        if (roles.roleForUser(user.tenantId(), user.userId()).isPresent()) {
            return policiesFor(effectivePermissions(user));
        }
        if (users.findById(user.userId()).map(candidate -> candidate.assignmentScoped()).orElse(false)) {
            return policiesFor(loadAssignedPermissions(user, projectId));
        }

        EnumSet<AuthorizationPolicy> policies = EnumSet.copyOf(MEMBER_POLICIES);
        if (ProjectRole.TEST_MANAGER.hasDatabaseValue(role.get())) {
            policies.addAll(TEST_MANAGER_EXTRA_POLICIES);
        }
        return policies;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<AccessPermission> projectPermissions(AuthenticatedUser user, UUID projectId) {
        if (user.globalAdministrator()) {
            return effectivePermissions(user);
        }
        Optional<String> role = projectMemberships.findActiveRole(user.tenantId(), projectId, user.userId());
        if (role.isEmpty()) {
            return EnumSet.noneOf(AccessPermission.class);
        }
        if (roles.roleForUser(user.tenantId(), user.userId()).isPresent()) {
            return effectivePermissions(user);
        }
        if (users.findById(user.userId()).map(candidate -> candidate.assignmentScoped()).orElse(false)) {
            return loadAssignedPermissions(user, projectId);
        }
        return legacyPermissions(role.get());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Set<AccessPermission>> assignedProjectPermissions(AuthenticatedUser user) {
        if (user.globalAdministrator()) {
            return Map.of();
        }
        Map<UUID, Set<AccessPermission>> result = new LinkedHashMap<>();
        projectMemberships.findByTenantIdAndUserIdAndDeletedAtIsNull(user.tenantId(), user.userId()).stream()
                .filter(membership -> membership.active())
                .forEach(membership -> result.put(
                        membership.projectId(), projectPermissions(user, membership.projectId())));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAllowed(AuthenticatedUser user, AuthorizationPolicy policy, UUID projectId) {
        if (policy == AuthorizationPolicy.USER_ACCESS_MANAGE) {
            return user.globalAdministrator();
        }
        if (policy == AuthorizationPolicy.PROJECT_CREATE) {
            return user.globalAdministrator() && effectivePermissions(user).contains(AccessPermission.CREATE);
        }
        if (projectId == null) {
            return false;
        }
        return projectCapabilities(user, projectId).contains(policy);
    }

    @Override
    public boolean canCreateProject(Authentication authentication) {
        return principal(authentication)
                .map(user -> isAllowed(user, AuthorizationPolicy.PROJECT_CREATE, null))
                .orElse(false);
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

    @Override
    @Transactional
    public void requireGlobalOrAnyProjectPermission(
            Authentication authentication,
            AccessPermission permission,
            String resourceType,
            String resourceId,
            String correlationId) {
        AuthenticatedUser user = principal(authentication)
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
        boolean allowed = user.globalAdministrator()
                ? effectivePermissions(user).contains(permission)
                : assignedProjectPermissions(user).values().stream()
                        .anyMatch(permissions -> permissions.contains(permission));
        if (allowed) {
            return;
        }
        auditEvents.save(AuditEvent.authorizationDenied(
                user.userId().toString(),
                user.tenantId(),
                null,
                resourceType,
                resourceId,
                "ANY_PROJECT_" + permission.name(),
                correlationId));
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
                    AuthorizationPolicy.SUITE_CREATE,
                    AuthorizationPolicy.CYCLE_CREATE,
                    AuthorizationPolicy.REQUIREMENT_CREATE,
                    AuthorizationPolicy.TEST_CASE_CREATE,
                    AuthorizationPolicy.UPLOAD_ACCESS,
                    AuthorizationPolicy.GENERATION_JOB_ACCESS));
        }
        if (permissions.contains(AccessPermission.EDIT)) {
            result.addAll(EnumSet.of(
                    AuthorizationPolicy.SUITE_EDIT,
                    AuthorizationPolicy.CYCLE_EDIT,
                    AuthorizationPolicy.REQUIREMENT_EDIT,
                    AuthorizationPolicy.TEST_CASE_EDIT));
        }
        if (permissions.contains(AccessPermission.APPROVE_REQUIREMENTS)) {
            result.add(AuthorizationPolicy.REQUIREMENT_APPROVE);
        }
        if (permissions.contains(AccessPermission.EXECUTE)) {
            result.add(AuthorizationPolicy.PREDEFINED_CASE_GENERATE);
        }
        if (permissions.contains(AccessPermission.DELETE)) {
            result.addAll(EnumSet.of(
                    AuthorizationPolicy.SUITE_DELETE,
                    AuthorizationPolicy.CYCLE_DELETE,
                    AuthorizationPolicy.REQUIREMENT_DELETE_UNLINKED,
                    AuthorizationPolicy.TEST_CASE_DELETE_DRAFT,
                    AuthorizationPolicy.PREDEFINED_CASE_DELETE));
        }
        if (permissions.contains(AccessPermission.MANAGE_ASSIGNMENTS)) {
            result.addAll(EnumSet.of(
                    AuthorizationPolicy.PROJECT_MANAGE_USERS,
                    AuthorizationPolicy.PROJECT_MANAGE_SUITES,
                    AuthorizationPolicy.PROJECT_MANAGE_CYCLES,
                    AuthorizationPolicy.SUITE_MANAGE_ASSIGNMENTS,
                    AuthorizationPolicy.TEST_CASE_ASSIGN));
        }
        return result;
    }

    private Set<AccessPermission> loadAssignedPermissions(AuthenticatedUser user, UUID projectId) {
        return projectPermissions
                .findByTenantIdAndUserIdAndProjectId(user.tenantId(), user.userId(), projectId)
                .stream()
                .map(value -> AccessPermission.valueOf(value.permissionName()))
                .collect(java.util.stream.Collectors.toCollection(() -> EnumSet.noneOf(AccessPermission.class)));
    }

    private static Set<AccessPermission> legacyPermissions(String role) {
        if (ProjectRole.TEST_MANAGER.hasDatabaseValue(role)) {
            return EnumSet.allOf(AccessPermission.class);
        }
        return EnumSet.of(
                AccessPermission.VIEW,
                AccessPermission.CREATE,
                AccessPermission.EDIT,
                AccessPermission.DELETE);
    }
}
