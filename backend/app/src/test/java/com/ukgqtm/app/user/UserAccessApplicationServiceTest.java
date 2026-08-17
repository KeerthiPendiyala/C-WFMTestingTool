package com.ukgqtm.app.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ukgqtm.app.role.RoleApplicationService;
import com.ukgqtm.app.user.UserAccessApplicationService.UpdateUserCommand;
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
import com.ukgqtm.project.domain.AccessRole;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    @Mock private RoleApplicationService roles;
    @Mock private AuditEventRepository auditEvents;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserAccessApplicationService service;

    @Test
    void updatesUserRoleWithoutWritingIndividualPermissionOverrides() {
        AuthenticatedUser actor = administrator();
        UUID projectId = UUID.randomUUID();
        ApplicationUser target = ApplicationUser.localUser("Alex", "Analyst", "alex@example.test", true, true);
        ProjectMembership membership = ProjectMembership.create(
                actor.tenantId(), projectId, target.id(), ProjectRole.TEST_ANALYST, actor.userId());
        AccessRole managerRole = AccessRole.create(
                actor.tenantId(), "Test Manager", "Manager role", false, actor.userId());
        UserProjectPermission legacyPermission = UserProjectPermission.create(
                actor.tenantId(), target.id(), projectId, AccessPermission.VIEW, actor.userId());

        when(roles.requireRole(actor.tenantId(), managerRole.id())).thenReturn(managerRole);
        when(roles.roleForUser(actor.tenantId(), target.id())).thenReturn(Optional.of(managerRole));
        when(roles.permissionsForRole(managerRole.id()))
                .thenReturn(EnumSet.of(AccessPermission.VIEW, AccessPermission.EDIT, AccessPermission.APPROVE_REQUIREMENTS));
        when(users.findByIdAndDeletedAtIsNull(target.id())).thenReturn(Optional.of(target));
        when(users.findByNormalizedContactEmailAndDeletedAtIsNull("alexa@example.test")).thenReturn(Optional.empty());
        when(projects.findByTenantIdAndIdAndActiveTrueAndDeletedAtIsNull(actor.tenantId(), projectId))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(Project.class)));
        when(memberships.findByTenantIdAndUserIdAndDeletedAtIsNull(actor.tenantId(), target.id()))
                .thenReturn(List.of(membership));
        when(administrators.findByUserIdAndDeletedAtIsNull(target.id())).thenReturn(Optional.empty());
        when(permissions.findByTenantIdAndUserId(actor.tenantId(), target.id()))
                .thenReturn(List.of(legacyPermission));
        when(suiteScopes.findByTenantIdAndUserId(actor.tenantId(), target.id())).thenReturn(List.of());
        when(cycleScopes.findByTenantIdAndUserId(actor.tenantId(), target.id())).thenReturn(List.of());

        var updated = service.updateUser(
                actor,
                target.id(),
                new UpdateUserCommand(
                        "Alexa",
                        "Lead",
                        "alexa@example.test",
                        managerRole.id(),
                        UserStatus.INACTIVE,
                        List.of(projectId),
                        "",
                        ""),
                "correlation-1");

        assertThat(updated.roleId()).isEqualTo(managerRole.id());
        assertThat(updated.roleName()).isEqualTo("Test Manager");
        assertThat(updated.permissions()).containsExactly(
                AccessPermission.VIEW, AccessPermission.EDIT, AccessPermission.APPROVE_REQUIREMENTS);
        assertThat(membership.projectRole()).isEqualTo(ProjectRole.TEST_MANAGER.databaseValue());
        verify(roles).assignRole(actor.tenantId(), target.id(), managerRole.id(), actor.userId());
        verify(permissions).deleteAllInBatch(List.of(legacyPermission));
        verify(permissions, never()).saveAll(any());
        verify(auditEvents).save(any(AuditEvent.class));
        verifyNoInteractions(credentials, passwordEncoder);
    }

    @Test
    void changingRolePermissionsChangesTheUserSummaryWithoutUpdatingTheUser() {
        AuthenticatedUser actor = administrator();
        ApplicationUser target = ApplicationUser.localUser("Alex", "Analyst", "alex@example.test", true, true);
        AccessRole viewerRole = AccessRole.create(actor.tenantId(), "Viewer", "Read only", false, actor.userId());
        when(users.findByDeletedAtIsNullOrderByLastNameAscFirstNameAsc()).thenReturn(List.of(target));
        when(memberships.findByTenantIdAndUserIdAndDeletedAtIsNull(actor.tenantId(), target.id()))
                .thenReturn(List.of());
        when(roles.roleForUser(actor.tenantId(), target.id())).thenReturn(Optional.of(viewerRole));
        when(roles.permissionsForRole(viewerRole.id()))
                .thenReturn(EnumSet.of(AccessPermission.VIEW), EnumSet.of(AccessPermission.VIEW, AccessPermission.EDIT));

        assertThat(service.listUsers(actor).getFirst().permissions()).containsExactly(AccessPermission.VIEW);
        assertThat(service.listUsers(actor).getFirst().permissions())
                .containsExactly(AccessPermission.VIEW, AccessPermission.EDIT);
        verify(roles, never()).assignRole(any(), any(), any(), any());
    }

    @Test
    void rejectsMismatchedOptionalResetPasswordsWithoutChangingCredential() {
        AuthenticatedUser actor = administrator();
        UUID projectId = UUID.randomUUID();
        ApplicationUser target = ApplicationUser.localUser("Alex", "Analyst", "alex@example.test", true, true);
        AccessRole viewerRole = AccessRole.create(actor.tenantId(), "Viewer", "Read only", false, actor.userId());

        when(roles.requireRole(actor.tenantId(), viewerRole.id())).thenReturn(viewerRole);
        when(users.findByIdAndDeletedAtIsNull(target.id())).thenReturn(Optional.of(target));
        when(users.findByNormalizedContactEmailAndDeletedAtIsNull("alex@example.test")).thenReturn(Optional.of(target));
        when(projects.findByTenantIdAndIdAndActiveTrueAndDeletedAtIsNull(actor.tenantId(), projectId))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(Project.class)));
        when(memberships.findByTenantIdAndUserIdAndDeletedAtIsNull(actor.tenantId(), target.id()))
                .thenReturn(List.of());
        when(administrators.findByUserIdAndDeletedAtIsNull(target.id())).thenReturn(Optional.empty());
        when(permissions.findByTenantIdAndUserId(actor.tenantId(), target.id())).thenReturn(List.of());
        when(suiteScopes.findByTenantIdAndUserId(actor.tenantId(), target.id())).thenReturn(List.of());
        when(cycleScopes.findByTenantIdAndUserId(actor.tenantId(), target.id())).thenReturn(List.of());

        assertThatThrownBy(() -> service.updateUser(
                        actor,
                        target.id(),
                        new UpdateUserCommand(
                                "Alex",
                                "Analyst",
                                "alex@example.test",
                                viewerRole.id(),
                                UserStatus.ACTIVE,
                                List.of(projectId),
                                "Updated1!Password",
                                "Different1!Password"),
                        "correlation-3"))
                .isInstanceOf(com.ukgqtm.app.api.ApiConflictException.class)
                .hasMessage("Password and confirmation must match.");
        verifyNoInteractions(credentials, passwordEncoder);
    }

    private static AuthenticatedUser administrator() {
        return new AuthenticatedUser(
                UUID.randomUUID(),
                "tenant-1",
                "admin-object",
                "Avery",
                "Administrator",
                "avery@example.test",
                true);
    }
}
