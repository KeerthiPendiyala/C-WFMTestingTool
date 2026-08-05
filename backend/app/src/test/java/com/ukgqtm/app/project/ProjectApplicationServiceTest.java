package com.ukgqtm.app.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ukgqtm.app.api.ApiConflictException;
import com.ukgqtm.audit.repository.AuditEventRepository;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.identity.api.EntraTokenClaims;
import com.ukgqtm.identity.domain.ApplicationUser;
import com.ukgqtm.identity.repository.ApplicationUserRepository;
import com.ukgqtm.project.domain.Project;
import com.ukgqtm.project.domain.ProjectMembership;
import com.ukgqtm.project.domain.ProjectRole;
import com.ukgqtm.project.repository.ProjectMembershipRepository;
import com.ukgqtm.project.repository.ProjectRepository;
import com.ukgqtm.project.repository.ProjectSuiteAssignmentRepository;
import com.ukgqtm.project.repository.ProjectTestCycleRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProjectApplicationServiceTest {
    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final ProjectMembershipRepository memberships = mock(ProjectMembershipRepository.class);
    private final ProjectSuiteAssignmentRepository suiteAssignments = mock(ProjectSuiteAssignmentRepository.class);
    private final ProjectTestCycleRepository cycles = mock(ProjectTestCycleRepository.class);
    private final ApplicationUserRepository users = mock(ApplicationUserRepository.class);
    private final AuditEventRepository auditEvents = mock(AuditEventRepository.class);
    private final ProjectApplicationService service =
            new ProjectApplicationService(projects, memberships, suiteAssignments, cycles, users, auditEvents);

    private final AuthenticatedUser admin = user(true);
    private final UUID projectId = UUID.randomUUID();
    private Project project;

    @BeforeEach
    void setUp() {
        project = Project.create(admin.tenantId(), "ABC", "Australian Broadcasting Corporation", "Timekeeping", admin.userId());
        when(projects.findByTenantIdAndIdAndActiveTrueAndDeletedAtIsNull(admin.tenantId(), projectId))
                .thenReturn(Optional.of(project));
        when(projects.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberships.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createProjectValidatesUniqueNameAndCodeAndAuditsCreation() {
        var created = service.createProject(
                admin,
                new ProjectApplicationService.CreateProjectCommand("new code", "New Project", "Description"),
                "corr-1");

        assertThat(created.projectKey()).isEqualTo("NEW_CODE");
        verify(projects).save(any(Project.class));
        verify(auditEvents).save(any());
    }

    @Test
    void createProjectRejectsDuplicateCodeBeforeSave() {
        when(projects.existsByTenantIdAndProjectKeyAndDeletedAtIsNull(admin.tenantId(), "ABC")).thenReturn(true);

        assertThatThrownBy(() -> service.createProject(
                        admin,
                        new ProjectApplicationService.CreateProjectCommand("ABC", "Another", null),
                        "corr-1"))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("code");
        verify(projects, never()).save(any(Project.class));
    }

    @Test
    void addProjectMembershipPreProvisionsInvitedUserForFirstLoginLinkage() {
        var command = new ProjectApplicationService.AddProjectMemberCommand(
                "Mina", "Manager", "Mina.Manager@Example.Test", ProjectRole.TEST_MANAGER);
        when(users.findByNormalizedContactEmailAndDeletedAtIsNull("mina.manager@example.test"))
                .thenReturn(Optional.empty());

        var membership = service.addProjectMembership(admin, projectId, command, "corr-1");

        assertThat(membership.email()).isEqualTo("mina.manager@example.test");
        assertThat(membership.invitationStatus()).isEqualTo("INVITED");
        verify(users).save(any(ApplicationUser.class));
        verify(auditEvents, times(2)).save(any());
    }

    @Test
    void addProjectMembershipDoesNotOverwriteExistingEntraBinding() {
        ApplicationUser existing = ApplicationUser.preProvision(
                "Existing", "User", "existing@example.test", admin.userId());
        existing.bindToEntraIdentity("tenant-1", "object-1", "existing@example.test", "existing@example.test", "Existing User");
        when(users.findByNormalizedContactEmailAndDeletedAtIsNull("existing@example.test"))
                .thenReturn(Optional.of(existing));

        service.addProjectMembership(
                admin,
                projectId,
                new ProjectApplicationService.AddProjectMemberCommand(
                        "Changed", "Name", "existing@example.test", ProjectRole.TEST_LEAD),
                "corr-1");

        assertThat(existing.isBoundTo("tenant-1", "object-1")).isTrue();
        assertThat(existing.firstName()).isEqualTo("Existing");
    }

    @Test
    void newlyProvisionedUserCanStillBindOnFirstLogin() {
        ApplicationUser provisioned = ApplicationUser.preProvision(
                "Mina", "Manager", "mina.manager@example.test", admin.userId());

        provisioned.bindToEntraIdentity(
                "tenant-1",
                "object-1",
                "mina.manager@example.test",
                "mina.manager@example.test",
                "Mina Manager");

        assertThat(provisioned.preProvisioningStatus()).isEqualTo("BOUND");
        assertThat(provisioned.accessStatus()).isEqualTo("ACTIVE");
        assertThat(new EntraTokenClaims("tenant-1", "object-1", "mina.manager@example.test", null, "Mina", "corr")
                        .tenantId())
                .isEqualTo("tenant-1");
    }

    @Test
    void duplicateActiveProjectMembershipIsRejected() {
        ApplicationUser existing = ApplicationUser.preProvision(
                "Mina", "Manager", "mina.manager@example.test", admin.userId());
        when(users.findByNormalizedContactEmailAndDeletedAtIsNull("mina.manager@example.test"))
                .thenReturn(Optional.of(existing));
        when(memberships.findByTenantIdAndProjectIdAndUserIdAndDeletedAtIsNull(
                        admin.tenantId(), projectId, existing.id()))
                .thenReturn(Optional.of(ProjectMembership.create(
                        admin.tenantId(), projectId, existing.id(), ProjectRole.TEST_MANAGER, admin.userId())));

        assertThatThrownBy(() -> service.addProjectMembership(
                        admin,
                        projectId,
                        new ProjectApplicationService.AddProjectMemberCommand(
                                "Mina", "Manager", "mina.manager@example.test", ProjectRole.TEST_MANAGER),
                        "corr-1"))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("already assigned");
    }

    @Test
    void testManagerCannotRemoveLastEffectiveProjectManagerWithoutAdministratorOverride() {
        AuthenticatedUser manager = user(false);
        UUID membershipId = UUID.randomUUID();
        ProjectMembership membership = ProjectMembership.create(
                manager.tenantId(), projectId, manager.userId(), ProjectRole.TEST_MANAGER, manager.userId());
        when(projects.findByTenantIdAndIdAndActiveTrueAndDeletedAtIsNull(manager.tenantId(), projectId))
                .thenReturn(Optional.of(project));
        when(memberships.findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(manager.tenantId(), projectId, membershipId))
                .thenReturn(Optional.of(membership));
        when(memberships.countByTenantIdAndProjectIdAndProjectRoleAndMembershipStatusAndDeletedAtIsNull(
                        manager.tenantId(), projectId, ProjectRole.TEST_MANAGER.databaseValue(), "ACTIVE"))
                .thenReturn(1L);

        assertThatThrownBy(() -> service.disableProjectMembership(manager, projectId, membershipId, false, "corr-1"))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("last effective project manager");
    }

    @Test
    void administratorCanDeliberatelyOverrideLastManagerRemoval() {
        UUID membershipId = UUID.randomUUID();
        ProjectMembership membership = ProjectMembership.create(
                admin.tenantId(), projectId, admin.userId(), ProjectRole.TEST_MANAGER, admin.userId());
        when(memberships.findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(admin.tenantId(), projectId, membershipId))
                .thenReturn(Optional.of(membership));
        when(memberships.countByTenantIdAndProjectIdAndProjectRoleAndMembershipStatusAndDeletedAtIsNull(
                        admin.tenantId(), projectId, ProjectRole.TEST_MANAGER.databaseValue(), "ACTIVE"))
                .thenReturn(1L);

        service.disableProjectMembership(admin, projectId, membershipId, true, "corr-1");

        assertThat(membership.active()).isFalse();
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
