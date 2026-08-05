package com.ukgqtm.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ukgqtm.audit.repository.AuditEventRepository;
import com.ukgqtm.identity.api.AuthenticationDeniedException;
import com.ukgqtm.identity.api.EntraTokenClaims;
import com.ukgqtm.identity.domain.ApplicationUser;
import com.ukgqtm.identity.repository.ApplicationUserRepository;
import com.ukgqtm.identity.repository.ApprovedTenantRepository;
import com.ukgqtm.identity.repository.GlobalAdministratorAssignmentRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PreProvisionedEntraUserResolverTest {
    private final ApplicationUserRepository users = mock(ApplicationUserRepository.class);
    private final ApprovedTenantRepository approvedTenants = mock(ApprovedTenantRepository.class);
    private final GlobalAdministratorAssignmentRepository administrators = mock(GlobalAdministratorAssignmentRepository.class);
    private final AuditEventRepository auditEvents = mock(AuditEventRepository.class);
    private final PreProvisionedEntraUserResolver resolver =
            new PreProvisionedEntraUserResolver(users, approvedTenants, administrators, auditEvents);

    @Test
    void bindsSingleInvitedPreProvisionedUserOnFirstLogin() throws Exception {
        ApplicationUser user = invitedUser("avery.admin@example.test");
        when(approvedTenants.existsByEntraTenantIdAndActive("tenant-1", true)).thenReturn(true);
        when(users.findForUpdateByEntraTenantIdAndEntraObjectIdAndDeletedAtIsNull("tenant-1", "object-1"))
                .thenReturn(Optional.empty());
        when(users.findByNormalizedContactEmailInAndDeletedAtIsNull(any())).thenReturn(List.of(user));

        var authenticated = resolver.resolve(claims("tenant-1", "object-1", "avery.admin@example.test"));

        assertThat(authenticated.tenantId()).isEqualTo("tenant-1");
        assertThat(authenticated.objectId()).isEqualTo("object-1");
        assertThat(user.preProvisioningStatus()).isEqualTo("BOUND");
        assertThat(user.accessStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void rejectsAmbiguousFirstLoginContactClaims() throws Exception {
        when(approvedTenants.existsByEntraTenantIdAndActive("tenant-1", true)).thenReturn(true);
        when(users.findForUpdateByEntraTenantIdAndEntraObjectIdAndDeletedAtIsNull("tenant-1", "object-1"))
                .thenReturn(Optional.empty());
        when(users.findByNormalizedContactEmailInAndDeletedAtIsNull(any()))
                .thenReturn(List.of(invitedUser("one@example.test"), invitedUser("two@example.test")));

        assertThatThrownBy(() -> resolver.resolve(claims("tenant-1", "object-1", "one@example.test")))
                .isInstanceOf(AuthenticationDeniedException.class)
                .hasMessageContaining("multiple users");
    }

    @Test
    void rejectsDisabledUser() throws Exception {
        when(approvedTenants.existsByEntraTenantIdAndActive("tenant-1", true)).thenReturn(true);
        when(users.findForUpdateByEntraTenantIdAndEntraObjectIdAndDeletedAtIsNull("tenant-1", "object-1"))
                .thenReturn(Optional.of(disabledUser()));

        assertThatThrownBy(() -> resolver.resolve(claims("tenant-1", "object-1", "disabled@example.test")))
                .isInstanceOf(AuthenticationDeniedException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void resolvesFirstLoginRaceWhenSameIdentityWasAlreadyBound() throws Exception {
        ApplicationUser user = invitedUser("avery.admin@example.test");
        user.bindToEntraIdentity("tenant-1", "object-1", "avery.admin@example.test", "avery.admin@example.test", "Avery");
        when(approvedTenants.existsByEntraTenantIdAndActive("tenant-1", true)).thenReturn(true);
        when(users.findForUpdateByEntraTenantIdAndEntraObjectIdAndDeletedAtIsNull("tenant-1", "object-1"))
                .thenReturn(Optional.empty());
        when(users.findByNormalizedContactEmailInAndDeletedAtIsNull(any())).thenReturn(List.of(user));

        var authenticated = resolver.resolve(claims("tenant-1", "object-1", "avery.admin@example.test"));

        assertThat(authenticated.immutablePrincipalKey()).isEqualTo("tenant-1:object-1");
    }

    private static EntraTokenClaims claims(String tenantId, String objectId, String email) {
        return new EntraTokenClaims(tenantId, objectId, email, email, "Avery Administrator", "corr");
    }

    private static ApplicationUser invitedUser(String email) throws Exception {
        var constructor = ApplicationUser.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        ApplicationUser user = constructor.newInstance();
        set(user, "id", UUID.randomUUID());
        set(user, "firstName", "Avery");
        set(user, "lastName", "Administrator");
        set(user, "normalizedContactEmail", email);
        set(user, "preProvisioningStatus", "PRE_PROVISIONED");
        set(user, "accessStatus", "INVITED");
        return user;
    }

    private static ApplicationUser disabledUser() throws Exception {
        ApplicationUser user = invitedUser("disabled@example.test");
        set(user, "accessStatus", "DISABLED");
        set(user, "entraTenantId", "tenant-1");
        set(user, "entraObjectId", "object-1");
        return user;
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field declaredField = target.getClass().getDeclaredField(field);
        declaredField.setAccessible(true);
        declaredField.set(target, value);
    }
}
