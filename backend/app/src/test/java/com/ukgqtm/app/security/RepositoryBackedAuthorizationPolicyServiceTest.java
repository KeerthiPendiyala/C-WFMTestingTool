package com.ukgqtm.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ukgqtm.audit.repository.AuditEventRepository;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.identity.repository.ApplicationUserRepository;
import com.ukgqtm.project.domain.ProjectRole;
import com.ukgqtm.project.repository.ProjectMembershipRepository;
import com.ukgqtm.project.repository.UserProjectPermissionRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class RepositoryBackedAuthorizationPolicyServiceTest {
    private final ProjectMembershipRepository memberships = mock(ProjectMembershipRepository.class);
    private final ApplicationUserRepository users = mock(ApplicationUserRepository.class);
    private final UserProjectPermissionRepository projectPermissions = mock(UserProjectPermissionRepository.class);
    private final AuditEventRepository auditEvents = mock(AuditEventRepository.class);
    private final RepositoryBackedAuthorizationPolicyService policies =
            new RepositoryBackedAuthorizationPolicyService(memberships, users, projectPermissions, auditEvents);

    private final UUID projectId = UUID.randomUUID();

    @Test
    void administratorReceivesEveryPolicyIncludingProjectCreateAndManagerInheritance() {
        AuthenticatedUser admin = user(true);

        assertThat(policies.globalCapabilities(admin)).contains(AuthorizationPolicy.PROJECT_CREATE);
        assertThat(policies.isAllowed(admin, AuthorizationPolicy.PROJECT_MANAGE_USERS, projectId)).isTrue();
        assertThat(policies.isAllowed(admin, AuthorizationPolicy.PREDEFINED_CASE_GENERATE, projectId)).isTrue();
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
