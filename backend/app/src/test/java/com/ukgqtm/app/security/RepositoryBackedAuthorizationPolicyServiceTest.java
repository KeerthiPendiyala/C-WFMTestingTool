package com.ukgqtm.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ukgqtm.audit.repository.AuditEventRepository;
import com.ukgqtm.app.role.RoleApplicationService;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.identity.domain.ApplicationUser;
import com.ukgqtm.identity.repository.ApplicationUserRepository;
import com.ukgqtm.project.domain.AccessPermission;
import com.ukgqtm.project.domain.AccessRole;
import com.ukgqtm.project.domain.ProjectMembership;
import com.ukgqtm.project.domain.ProjectRole;
import com.ukgqtm.project.domain.UserProjectPermission;
import com.ukgqtm.project.repository.ProjectMembershipRepository;
import com.ukgqtm.project.repository.UserProjectPermissionRepository;
import java.util.Optional;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class RepositoryBackedAuthorizationPolicyServiceTest {
    private final ProjectMembershipRepository memberships = mock(ProjectMembershipRepository.class);
    private final ApplicationUserRepository users = mock(ApplicationUserRepository.class);
    private final UserProjectPermissionRepository projectPermissions = mock(UserProjectPermissionRepository.class);
    private final AuditEventRepository auditEvents = mock(AuditEventRepository.class);
    private final RoleApplicationService roles = mock(RoleApplicationService.class);
    private final RepositoryBackedAuthorizationPolicyService policies =
            new RepositoryBackedAuthorizationPolicyService(memberships, users, projectPermissions, auditEvents, roles);

    private final UUID projectId = UUID.randomUUID();

    @Test
    void administratorReceivesEveryPolicyIncludingProjectCreateAndManagerInheritance() {
        AuthenticatedUser admin = user(true);

        assertThat(policies.globalCapabilities(admin)).contains(AuthorizationPolicy.PROJECT_CREATE);
        assertThat(policies.isAllowed(admin, AuthorizationPolicy.PROJECT_MANAGE_USERS, projectId)).isTrue();
        assertThat(policies.isAllowed(admin, AuthorizationPolicy.PREDEFINED_CASE_GENERATE, projectId)).isTrue();
        assertThat(policies.isAllowed(admin, AuthorizationPolicy.SUITE_DELETE, projectId)).isTrue();
    }

    @Test
    void testManagerReceivesAssignedProjectManagerPoliciesButCannotCreateProjects() {
        AuthenticatedUser manager = user(false);
        when(memberships.findActiveRole(manager.tenantId(), projectId, manager.userId()))
                .thenReturn(Optional.of(ProjectRole.TEST_MANAGER.databaseValue()));

        assertThat(policies.isAllowed(manager, AuthorizationPolicy.PROJECT_CREATE, null)).isFalse();
        assertThat(policies.isAllowed(manager, AuthorizationPolicy.PROJECT_MANAGE_USERS, projectId)).isTrue();
        assertThat(policies.isAllowed(manager, AuthorizationPolicy.PROJECT_MANAGE_SUITES, projectId)).isTrue();
        assertThat(policies.isAllowed(manager, AuthorizationPolicy.REQUIREMENT_APPROVE, projectId)).isTrue();
    }

    @Test
    void testLeadAndAnalystCannotManageSuitesCyclesOrApproveRequirements() {
        AuthenticatedUser lead = user(false);
        when(memberships.findActiveRole(lead.tenantId(), projectId, lead.userId()))
                .thenReturn(Optional.of(ProjectRole.TEST_LEAD.databaseValue()));

        assertThat(policies.isAllowed(lead, AuthorizationPolicy.REQUIREMENT_CREATE, projectId)).isTrue();
        assertThat(policies.isAllowed(lead, AuthorizationPolicy.TEST_CASE_VIEW_EXPORT, projectId)).isTrue();
        assertThat(policies.isAllowed(lead, AuthorizationPolicy.PROJECT_MANAGE_SUITES, projectId)).isFalse();
        assertThat(policies.isAllowed(lead, AuthorizationPolicy.PROJECT_MANAGE_CYCLES, projectId)).isFalse();
        assertThat(policies.isAllowed(lead, AuthorizationPolicy.REQUIREMENT_APPROVE, projectId)).isFalse();

        AuthenticatedUser analyst = user(false);
        when(memberships.findActiveRole(analyst.tenantId(), projectId, analyst.userId()))
                .thenReturn(Optional.of(ProjectRole.TEST_ANALYST.databaseValue()));

        assertThat(policies.isAllowed(analyst, AuthorizationPolicy.TEST_CASE_CREATE, projectId)).isTrue();
        assertThat(policies.isAllowed(analyst, AuthorizationPolicy.EXPORT_DOWNLOAD, projectId)).isTrue();
        assertThat(policies.isAllowed(analyst, AuthorizationPolicy.PREDEFINED_CASE_GENERATE, projectId)).isFalse();
    }

    @Test
    void assignmentScopedManagerUsesCurrentProjectPermissionsInsteadOfRoleDefaults() {
        ApplicationUser databaseUser = ApplicationUser.localUser(
                "Mina", "Manager", "mina@example.test", true, true);
        AuthenticatedUser manager = new AuthenticatedUser(
                databaseUser.id(),
                "tenant-1",
                "manager-object",
                "Mina",
                "Manager",
                "mina@example.test",
                false);
        ProjectMembership membership = ProjectMembership.create(
                manager.tenantId(), projectId, manager.userId(), ProjectRole.TEST_MANAGER, UUID.randomUUID());
        List<UserProjectPermission> assigned = List.of(
                UserProjectPermission.create(manager.tenantId(), manager.userId(), projectId, AccessPermission.VIEW, UUID.randomUUID()),
                UserProjectPermission.create(manager.tenantId(), manager.userId(), projectId, AccessPermission.CREATE, UUID.randomUUID()),
                UserProjectPermission.create(manager.tenantId(), manager.userId(), projectId, AccessPermission.EDIT, UUID.randomUUID()),
                UserProjectPermission.create(manager.tenantId(), manager.userId(), projectId, AccessPermission.EXECUTE, UUID.randomUUID()),
                UserProjectPermission.create(manager.tenantId(), manager.userId(), projectId, AccessPermission.DELETE, UUID.randomUUID()));

        when(memberships.findActiveRole(manager.tenantId(), projectId, manager.userId()))
                .thenReturn(Optional.of(ProjectRole.TEST_MANAGER.databaseValue()));
        when(memberships.findByTenantIdAndUserIdAndDeletedAtIsNull(manager.tenantId(), manager.userId()))
                .thenReturn(List.of(membership));
        when(users.findById(manager.userId())).thenReturn(Optional.of(databaseUser));
        when(projectPermissions.findByTenantIdAndUserIdAndProjectId(
                        manager.tenantId(), manager.userId(), projectId))
                .thenReturn(assigned);

        assertThat(policies.projectPermissions(manager, projectId))
                .containsExactlyInAnyOrder(
                        AccessPermission.VIEW,
                        AccessPermission.CREATE,
                        AccessPermission.EDIT,
                        AccessPermission.EXECUTE,
                        AccessPermission.DELETE)
                .doesNotContain(AccessPermission.MANAGE_ASSIGNMENTS);
        assertThat(policies.isAllowed(manager, AuthorizationPolicy.SUITE_CREATE, projectId)).isTrue();
        assertThat(policies.isAllowed(manager, AuthorizationPolicy.SUITE_EDIT, projectId)).isTrue();
        assertThat(policies.isAllowed(manager, AuthorizationPolicy.SUITE_DELETE, projectId)).isTrue();
        assertThat(policies.isAllowed(manager, AuthorizationPolicy.PREDEFINED_CASE_GENERATE, projectId)).isTrue();
        assertThat(policies.isAllowed(manager, AuthorizationPolicy.SUITE_MANAGE_ASSIGNMENTS, projectId)).isFalse();
        assertThat(policies.isAllowed(manager, AuthorizationPolicy.REQUIREMENT_APPROVE, projectId)).isFalse();
        assertThat(policies.assignedProjectPermissions(manager)).containsKey(projectId);
        assertThat(policies.isAllowed(manager, AuthorizationPolicy.SUITE_CREATE, UUID.randomUUID())).isFalse();
    }

    @Test
    void assignmentScopedManagerCanApproveOnlyWithExplicitApprovalPermission() {
        ApplicationUser databaseUser = ApplicationUser.localUser(
                "Mina", "Manager", "mina@example.test", true, true);
        AuthenticatedUser manager = new AuthenticatedUser(
                databaseUser.id(),
                "tenant-1",
                "manager-object",
                "Mina",
                "Manager",
                "mina@example.test",
                false);
        when(memberships.findActiveRole(manager.tenantId(), projectId, manager.userId()))
                .thenReturn(Optional.of(ProjectRole.TEST_MANAGER.databaseValue()));
        when(users.findById(manager.userId())).thenReturn(Optional.of(databaseUser));
        when(projectPermissions.findByTenantIdAndUserIdAndProjectId(
                        manager.tenantId(), manager.userId(), projectId))
                .thenReturn(List.of(
                        UserProjectPermission.create(
                                manager.tenantId(),
                                manager.userId(),
                                projectId,
                                AccessPermission.VIEW,
                                UUID.randomUUID()),
                        UserProjectPermission.create(
                                manager.tenantId(),
                                manager.userId(),
                                projectId,
                                AccessPermission.APPROVE_REQUIREMENTS,
                                UUID.randomUUID())));

        assertThat(policies.isAllowed(manager, AuthorizationPolicy.REQUIREMENT_APPROVE, projectId)).isTrue();
    }

    @Test
    void rolePermissionChangesImmediatelyChangeBackendAuthorization() {
        AuthenticatedUser tester = user(false);
        AccessRole role = AccessRole.create(tester.tenantId(), "Tester", "Testing role", false, UUID.randomUUID());
        when(memberships.findActiveRole(tester.tenantId(), projectId, tester.userId()))
                .thenReturn(Optional.of(ProjectRole.TEST_ANALYST.databaseValue()));
        when(roles.roleForUser(tester.tenantId(), tester.userId())).thenReturn(Optional.of(role));
        when(roles.permissionsForRole(role.id())).thenReturn(
                EnumSet.of(AccessPermission.VIEW),
                EnumSet.of(AccessPermission.VIEW, AccessPermission.EDIT));

        assertThat(policies.isAllowed(tester, AuthorizationPolicy.REQUIREMENT_EDIT, projectId)).isFalse();
        assertThat(policies.isAllowed(tester, AuthorizationPolicy.REQUIREMENT_EDIT, projectId)).isTrue();
    }

    @Test
    void administratorWithoutCreatePermissionCannotCreateProjects() {
        AuthenticatedUser administrator = user(true);
        AccessRole role = AccessRole.create(
                administrator.tenantId(), "Admin", "Restricted administrator", true, UUID.randomUUID());
        when(roles.roleForUser(administrator.tenantId(), administrator.userId())).thenReturn(Optional.of(role));
        when(roles.permissionsForRole(role.id())).thenReturn(EnumSet.of(AccessPermission.VIEW));
        var authentication = new ApplicationUserAuthenticationToken(administrator, null, List.of());

        assertThat(policies.globalCapabilities(administrator))
                .contains(AuthorizationPolicy.USER_ACCESS_MANAGE)
                .doesNotContain(AuthorizationPolicy.PROJECT_CREATE);
        assertThat(policies.canCreateProject(authentication)).isFalse();
        assertThat(policies.isAllowed(administrator, AuthorizationPolicy.PROJECT_CREATE, null)).isFalse();
    }

    @Test
    void crossProjectIdentifierIsDeniedAndAudited() {
        AuthenticatedUser analyst = user(false);
        var authentication = new ApplicationUserAuthenticationToken(analyst, null, java.util.List.of());
        when(memberships.findActiveRole(analyst.tenantId(), projectId, analyst.userId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> policies.require(
                        authentication,
                        AuthorizationPolicy.EXPORT_DOWNLOAD,
                        projectId,
                        "EXPORT",
                        "export-1",
                        "corr-1"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not available");
        verify(auditEvents).save(any());
    }

    private static AuthenticatedUser user(boolean administrator) {
        return new AuthenticatedUser(
                UUID.randomUUID(),
                "tenant-1",
                UUID.randomUUID().toString(),
                "Avery",
                "Tester",
                "avery@example.test",
                administrator);
    }
}
