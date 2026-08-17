package com.ukgqtm.app.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ukgqtm.app.security.ApplicationUserAuthenticationToken;
import com.ukgqtm.app.security.AuthorizationPolicyService;
import com.ukgqtm.app.security.LocalAdminAuthenticationService;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.identity.api.AuthenticatedUserResolver;
import com.ukgqtm.project.domain.AccessPermission;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class AuthControllerTest {
    @Test
    void authenticatedProfileLoadsCurrentProjectPermissionsFromAuthorizationStore() {
        @SuppressWarnings("unchecked")
        ObjectProvider<AuthenticatedUserResolver> resolvers = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<LocalAdminAuthenticationService> localAdmins = mock(ObjectProvider.class);
        AuthorizationPolicyService authorization = mock(AuthorizationPolicyService.class);
        AuthenticatedUser user = new AuthenticatedUser(
                UUID.randomUUID(),
                "tenant-1",
                "manager-object",
                "Mina",
                "Manager",
                "mina@example.test",
                false);
        UUID projectId = UUID.randomUUID();
        when(authorization.assignedRoleName(user)).thenReturn("Test Manager");
        when(authorization.assignedProjectPermissions(user)).thenReturn(Map.of(
                projectId,
                EnumSet.of(
                        AccessPermission.VIEW,
                        AccessPermission.CREATE,
                        AccessPermission.EDIT,
                        AccessPermission.EXECUTE,
                        AccessPermission.DELETE)));

        var response = new AuthController(resolvers, localAdmins, authorization)
                .me(new ApplicationUserAuthenticationToken(user, null, List.of()))
                .getBody();

        assertThat(response).isNotNull();
        assertThat(response.roleName()).isEqualTo("Test Manager");
        assertThat(response.projectPermissions().get(projectId.toString()))
                .containsExactly("CREATE", "DELETE", "EDIT", "EXECUTE", "VIEW");
    }
}
