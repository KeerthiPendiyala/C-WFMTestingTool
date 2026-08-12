package com.ukgqtm.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.identity.domain.ApplicationUser;
import com.ukgqtm.identity.repository.ApplicationUserRepository;
import com.ukgqtm.project.domain.AccessPermission;
import com.ukgqtm.project.domain.UserProjectPermission;
import com.ukgqtm.project.repository.UserCycleScopeRepository;
import com.ukgqtm.project.repository.UserProjectPermissionRepository;
import com.ukgqtm.project.repository.UserSuiteScopeRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssignmentScopeAuthorizationServiceTest {
    private final ApplicationUserRepository users = mock(ApplicationUserRepository.class);
    private final UserProjectPermissionRepository projectPermissions =
            mock(UserProjectPermissionRepository.class);
    private final UserSuiteScopeRepository suiteScopes = mock(UserSuiteScopeRepository.class);
    private final UserCycleScopeRepository cycleScopes = mock(UserCycleScopeRepository.class);
    private final AssignmentScopeAuthorizationService authorization =
            new AssignmentScopeAuthorizationService(users, projectPermissions, suiteScopes, cycleScopes);

    @Test
    void projectViewPermissionAllowsEverySuiteAndCycleInTheAssignedProject() {
        ApplicationUser databaseUser = ApplicationUser.localUser(
                "Test", "Manager", "manager@example.test", true, true);
        AuthenticatedUser manager = authenticated(databaseUser);
        UUID projectId = UUID.randomUUID();
        UUID unrelatedSuiteAssignmentId = UUID.randomUUID();
        UUID unrelatedCycleId = UUID.randomUUID();

        when(users.findById(manager.userId())).thenReturn(Optional.of(databaseUser));
        when(projectPermissions.findByTenantIdAndUserIdAndProjectId(
                        manager.tenantId(), manager.userId(), projectId))
                .thenReturn(List.of(UserProjectPermission.create(
                        manager.tenantId(), manager.userId(), projectId, AccessPermission.VIEW, UUID.randomUUID())));

        assertThat(authorization.canAccess(
                        manager, projectId, unrelatedSuiteAssignmentId, unrelatedCycleId))
                .isTrue();
        verify(suiteScopes, never()).findAssignmentIds(manager.tenantId(), manager.userId(), projectId);
        verify(cycleScopes, never()).findCycleIds(manager.tenantId(), manager.userId(), projectId);
    }

    @Test
    void suiteAndCycleScopesRemainTheFallbackWithoutProjectViewPermission() {
        ApplicationUser databaseUser = ApplicationUser.localUser(
                "Test", "Analyst", "analyst@example.test", true, true);
        AuthenticatedUser analyst = authenticated(databaseUser);
        UUID projectId = UUID.randomUUID();
        UUID suiteAssignmentId = UUID.randomUUID();
        UUID cycleId = UUID.randomUUID();

        when(users.findById(analyst.userId())).thenReturn(Optional.of(databaseUser));
        when(projectPermissions.findByTenantIdAndUserIdAndProjectId(
                        analyst.tenantId(), analyst.userId(), projectId))
                .thenReturn(List.of());
        when(suiteScopes.findAssignmentIds(analyst.tenantId(), analyst.userId(), projectId))
                .thenReturn(List.of(suiteAssignmentId));
        when(cycleScopes.findCycleIds(analyst.tenantId(), analyst.userId(), projectId))
                .thenReturn(List.of(cycleId));

        assertThat(authorization.canAccess(analyst, projectId, suiteAssignmentId, cycleId)).isTrue();
        assertThat(authorization.canAccess(analyst, projectId, suiteAssignmentId, UUID.randomUUID())).isFalse();
    }

    private static AuthenticatedUser authenticated(ApplicationUser user) {
        return new AuthenticatedUser(
                user.id(),
                "tenant-1",
                UUID.randomUUID().toString(),
                user.firstName(),
                user.lastName(),
                user.normalizedContactEmail(),
                false);
    }
}
