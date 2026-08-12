package com.ukgqtm.app.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ukgqtm.app.user.UserAccessApplicationService.UpdateUserCommand;
import com.ukgqtm.app.user.UserAccessApplicationService.UserRole;
import com.ukgqtm.app.user.UserAccessApplicationService.UserStatus;
import com.ukgqtm.audit.domain.AuditEvent;
import com.ukgqtm.audit.repository.AuditEventRepository;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.identity.domain.ApplicationUser;
import com.ukgqtm.identity.domain.LocalUserCredential;
import com.ukgqtm.identity.repository.ApplicationUserRepository;
import com.ukgqtm.identity.repository.GlobalAdministratorAssignmentRepository;
import com.ukgqtm.identity.repository.LocalUserCredentialRepository;
import com.ukgqtm.project.domain.AccessPermission;
import com.ukgqtm.project.domain.Project;
import com.ukgqtm.project.domain.ProjectMembership;
import com.ukgqtm.project.domain.ProjectRole;
import com.ukgqtm.project.domain.UserProjectPermission;
import com.ukgqtm.project.repository.ProjectMembershipRepository;
import com.ukgqtm.project.repository.ProjectRepository;
import com.ukgqtm.project.repository.ProjectSuiteAssignmentRepository;
import com.ukgqtm.project.repository.ProjectTestCycleRepository;
import com.ukgqtm.project.repository.UserCycleScopeRepository;
import com.ukgqtm.project.repository.UserProjectPermissionRepository;
import com.ukgqtm.project.repository.UserSuiteScopeRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserAccessApplicationServiceTest {
    @Mock private ApplicationUserRepository users;
    @Mock private LocalUserCredentialRepository credentials;
    @Mock private GlobalAdministratorAssignmentRepository administrators;
    @Mock private ProjectRepository projects;
    @Mock private ProjectMembershipRepository memberships;
    @Mock private ProjectSuiteAssignmentRepository suiteAssignments;
    @Mock private ProjectTestCycleRepository cycles;
    @Mock private UserProjectPermissionRepository permissions;
    @Mock private UserSuiteScopeRepository suiteScopes;
    @Mock private UserCycleScopeRepository cycleScopes;
    @Mock private AuditEventRepository auditEvents;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserAccessApplicationService service;

    @Test
    void updatesProfileStatusRoleAndProjectAccessInOneTransaction() {
        AuthenticatedUser actor = new AuthenticatedUser(
                UUID.randomUUID(),
                "tenant-1",
                "admin-object",
                "Avery",
                "Administrator",
                "avery@example.test",
                true);
        UUID projectId = UUID.randomUUID();
        ApplicationUser target = ApplicationUser.localUser(
                "Alex", "Analyst", "alex@example.test", true, true);
        ProjectMembership membership = ProjectMembership.create(
                actor.tenantId(), projectId, target.id(), ProjectRole.TEST_ANALYST, actor.userId());
        UserProjectPermission viewPermission = UserProjectPermission.create(
                actor.tenantId(), target.id(), projectId, AccessPermission.VIEW, actor.userId());
        UserProjectPermission editPermission = UserProjectPermission.create(
                actor.tenantId(), target.id(), projectId, AccessPermission.EDIT, actor.userId());
        UserProjectPermission approvePermission = UserProjectPermission.create(
                actor.tenantId(),
                target.id(),
                projectId,
                AccessPermission.APPROVE_REQUIREMENTS,
                actor.userId());

        when(users.findByIdAndDeletedAtIsNull(target.id())).thenReturn(Optional.of(target));
        when(users.findByNormalizedContactEmailAndDeletedAtIsNull("alexa@example.test"))
                .thenReturn(Optional.empty());
        when(projects.findByTenantIdAndIdAndActiveTrueAndDeletedAtIsNull(actor.tenantId(), projectId))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(Project.class)));
        when(memberships.findByTenantIdAndUserIdAndDeletedAtIsNull(actor.tenantId(), target.id()))
                .thenReturn(List.of(membership));
        when(administrators.findByUserIdAndDeletedAtIsNull(target.id())).thenReturn(Optional.empty());
        when(permissions.findByTenantIdAndUserId(actor.tenantId(), target.id()))
                .thenReturn(
                        List.of(viewPermission),
                        List.of(viewPermission, editPermission, approvePermission));
        when(suiteScopes.findByTenantIdAndUserId(actor.tenantId(), target.id())).thenReturn(List.of());
        when(cycleScopes.findByTenantIdAndUserId(actor.tenantId(), target.id())).thenReturn(List.of());

        var updated = service.updateUser(
                actor,
                target.id(),
                new UpdateUserCommand(
                        "Alexa",
                        "Lead",
                        "alexa@example.test",
                        UserRole.TEST_MANAGER,
                        UserStatus.INACTIVE,
                        List.of(projectId),
                        Set.of(
                                AccessPermission.VIEW,
                                AccessPermission.EDIT,
                                AccessPermission.APPROVE_REQUIREMENTS),
                        "",
                        ""),
                "correlation-1");

        assertThat(updated.firstName()).isEqualTo("Alexa");
        assertThat(updated.lastName()).isEqualTo("Lead");
        assertThat(updated.email()).isEqualTo("alexa@example.test");
        assertThat(updated.role()).isEqualTo("TEST_MANAGER");
        assertThat(updated.status()).isEqualTo("DISABLED");
        assertThat(updated.projectIds()).containsExactly(projectId);
        assertThat(updated.permissions())
                .containsExactly(
                        AccessPermission.VIEW,
                        AccessPermission.EDIT,
                        AccessPermission.APPROVE_REQUIREMENTS);
        assertThat(membership.projectRole()).isEqualTo(ProjectRole.TEST_MANAGER.databaseValue());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<UserProjectPermission>> savedPermissions =
                ArgumentCaptor.forClass(Iterable.class);
        verify(permissions).saveAll(savedPermissions.capture());
        assertThat(savedPermissions.getValue())
                .extracting(UserProjectPermission::permissionName)
                .containsExactlyInAnyOrder(
                        AccessPermission.VIEW.name(),
                        AccessPermission.EDIT.name(),
                        AccessPermission.APPROVE_REQUIREMENTS.name());
        verify(auditEvents).save(any(AuditEvent.class));
        verifyNoInteractions(credentials, passwordEncoder);
    }

    @Test
    void resetsLocalPasswordOnlyWhenBothOptionalFieldsAreValid() {
        AuthenticatedUser actor = new AuthenticatedUser(
                UUID.randomUUID(),
                "tenant-1",
                "admin-object",
                "Avery",
                "Administrator",
                "avery@example.test",
                true);
        UUID projectId = UUID.randomUUID();
        ApplicationUser target = ApplicationUser.localUser(
                "Alex", "Analyst", "alex@example.test", true, true);
        ProjectMembership membership = ProjectMembership.create(
                actor.tenantId(), projectId, target.id(), ProjectRole.TEST_ANALYST, actor.userId());
        UserProjectPermission viewPermission = UserProjectPermission.create(
                actor.tenantId(), target.id(), projectId, AccessPermission.VIEW, actor.userId());
        LocalUserCredential credential = LocalUserCredential.create(target.id(), actor.tenantId(), "$2a$old-hash");

        when(users.findByIdAndDeletedAtIsNull(target.id())).thenReturn(Optional.of(target));
        when(users.findByNormalizedContactEmailAndDeletedAtIsNull("alex@example.test"))
                .thenReturn(Optional.of(target));
        when(projects.findByTenantIdAndIdAndActiveTrueAndDeletedAtIsNull(actor.tenantId(), projectId))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(Project.class)));
        when(memberships.findByTenantIdAndUserIdAndDeletedAtIsNull(actor.tenantId(), target.id()))
                .thenReturn(List.of(membership));
        when(administrators.findByUserIdAndDeletedAtIsNull(target.id())).thenReturn(Optional.empty());
        when(permissions.findByTenantIdAndUserId(actor.tenantId(), target.id()))
                .thenReturn(List.of(viewPermission));
        when(suiteScopes.findByTenantIdAndUserId(actor.tenantId(), target.id())).thenReturn(List.of());
        when(cycleScopes.findByTenantIdAndUserId(actor.tenantId(), target.id())).thenReturn(List.of());
        when(credentials.findById(target.id())).thenReturn(Optional.of(credential));
        when(passwordEncoder.encode("Updated1!Password")).thenReturn("$2a$new-hash");

        service.updateUser(
                actor,
                target.id(),
                new UpdateUserCommand(
                        "Alex",
                        "Analyst",
                        "alex@example.test",
                        UserRole.TEST_ANALYST,
                        UserStatus.ACTIVE,
                        List.of(projectId),
                        Set.of(AccessPermission.VIEW),
                        "Updated1!Password",
                        "Updated1!Password"),
                "correlation-2");

        assertThat(credential.passwordHash()).isEqualTo("$2a$new-hash");
        verify(passwordEncoder).encode("Updated1!Password");
    }

    @Test
    void rejectsMismatchedOptionalResetPasswordsWithoutChangingCredential() {
        AuthenticatedUser actor = new AuthenticatedUser(
                UUID.randomUUID(),
                "tenant-1",
                "admin-object",
                "Avery",
                "Administrator",
                "avery@example.test",
                true);
        UUID projectId = UUID.randomUUID();
        ApplicationUser target = ApplicationUser.localUser(
                "Alex", "Analyst", "alex@example.test", true, true);
        ProjectMembership membership = ProjectMembership.create(
                actor.tenantId(), projectId, target.id(), ProjectRole.TEST_ANALYST, actor.userId());
        UserProjectPermission viewPermission = UserProjectPermission.create(
                actor.tenantId(), target.id(), projectId, AccessPermission.VIEW, actor.userId());

        when(users.findByIdAndDeletedAtIsNull(target.id())).thenReturn(Optional.of(target));
        when(users.findByNormalizedContactEmailAndDeletedAtIsNull("alex@example.test"))
                .thenReturn(Optional.of(target));
        when(projects.findByTenantIdAndIdAndActiveTrueAndDeletedAtIsNull(actor.tenantId(), projectId))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(Project.class)));
        when(memberships.findByTenantIdAndUserIdAndDeletedAtIsNull(actor.tenantId(), target.id()))
                .thenReturn(List.of(membership));
        when(administrators.findByUserIdAndDeletedAtIsNull(target.id())).thenReturn(Optional.empty());
        when(permissions.findByTenantIdAndUserId(actor.tenantId(), target.id()))
                .thenReturn(List.of(viewPermission));
        when(suiteScopes.findByTenantIdAndUserId(actor.tenantId(), target.id())).thenReturn(List.of());
        when(cycleScopes.findByTenantIdAndUserId(actor.tenantId(), target.id())).thenReturn(List.of());

        assertThatThrownBy(() -> service.updateUser(
                        actor,
                        target.id(),
                        new UpdateUserCommand(
                                "Alex",
                                "Analyst",
                                "alex@example.test",
                                UserRole.TEST_ANALYST,
                                UserStatus.ACTIVE,
                                List.of(projectId),
                                Set.of(AccessPermission.VIEW),
                                "Updated1!Password",
                                "Different1!Password"),
                        "correlation-3"))
                .isInstanceOf(com.ukgqtm.app.api.ApiConflictException.class)
                .hasMessage("Password and confirmation must match.");
        verifyNoInteractions(credentials, passwordEncoder);
    }

    @Test
    void rejectsApprovalPermissionForNonManagerRole() {
        AuthenticatedUser actor = new AuthenticatedUser(
                UUID.randomUUID(),
                "tenant-1",
                "admin-object",
                "Avery",
                "Administrator",
                "avery@example.test",
                true);
        UUID projectId = UUID.randomUUID();
        ApplicationUser target = ApplicationUser.localUser(
                "Alex", "Analyst", "alex@example.test", true, true);

        when(users.findByIdAndDeletedAtIsNull(target.id())).thenReturn(Optional.of(target));
        when(users.findByNormalizedContactEmailAndDeletedAtIsNull("alex@example.test"))
                .thenReturn(Optional.of(target));
        when(projects.findByTenantIdAndIdAndActiveTrueAndDeletedAtIsNull(actor.tenantId(), projectId))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(Project.class)));

        assertThatThrownBy(() -> service.updateUser(
                        actor,
                        target.id(),
                        new UpdateUserCommand(
                                "Alex",
                                "Analyst",
                                "alex@example.test",
                                UserRole.TEST_ANALYST,
                                UserStatus.ACTIVE,
                                List.of(projectId),
                                Set.of(AccessPermission.VIEW, AccessPermission.APPROVE_REQUIREMENTS),
                                "",
                                ""),
                        "correlation-4"))
                .isInstanceOf(com.ukgqtm.app.api.ApiConflictException.class)
                .hasMessage("Approve Requirements permission requires the Test Manager role.");
    }
}
