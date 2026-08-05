package com.ukgqtm.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ukgqtm.app.config.AuthSecurityProperties;
import com.ukgqtm.audit.repository.AuditEventRepository;
import com.ukgqtm.identity.domain.ApplicationUser;
import com.ukgqtm.identity.domain.LocalUserCredential;
import com.ukgqtm.identity.repository.ApplicationUserRepository;
import com.ukgqtm.identity.repository.GlobalAdministratorAssignmentRepository;
import com.ukgqtm.identity.repository.LocalUserCredentialRepository;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class LocalAdminAuthenticationServiceTest {
    private static final String LOCAL_PASSWORD = UUID.randomUUID().toString();
    private static final String WRONG_PASSWORD = UUID.randomUUID().toString();

    private final AuthSecurityProperties properties = properties();
    private final ApplicationUserRepository users = mock(ApplicationUserRepository.class);
    private final GlobalAdministratorAssignmentRepository administrators =
            mock(GlobalAdministratorAssignmentRepository.class);
    private final LocalUserCredentialRepository credentials = mock(LocalUserCredentialRepository.class);
    private final AuditEventRepository auditEvents = mock(AuditEventRepository.class);
    private final LocalAdminAuthenticationService service = new LocalAdminAuthenticationService(
            properties, users, administrators, credentials, auditEvents, new BCryptPasswordEncoder());

    @Test
    void authenticatesOnlyConfiguredActiveGlobalAdministrator() throws Exception {
        ApplicationUser admin = user("avery.admin@example.test", "ACTIVE");
        when(users.findByNormalizedContactEmailAndDeletedAtIsNull("avery.admin@example.test"))
                .thenReturn(Optional.of(admin));
        when(administrators.existsByUserIdAndDeletedAtIsNull(admin.id())).thenReturn(true);

        var authenticated = service.authenticate("Avery.Admin@Example.Test", LOCAL_PASSWORD, "corr");

        assertThat(authenticated.globalAdministrator()).isTrue();
        assertThat(authenticated.tenantId()).isEqualTo("dev-tenant");
        assertThat(authenticated.objectId()).isEqualTo("local-admin");
        assertThat(authenticated.contactEmail()).isEqualTo("avery.admin@example.test");
    }

    @Test
    void rejectsWrongPassword() {
        assertThatThrownBy(() -> service.authenticate("avery.admin@example.test", WRONG_PASSWORD, "corr"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void authenticatesActiveDatabaseUserWithBcryptCredential() throws Exception {
        var encoder = new BCryptPasswordEncoder();
        String password = "Strong@12345";
        ApplicationUser user = user("new.user@example.test", "ACTIVE");
        when(users.findByNormalizedContactEmailAndDeletedAtIsNull("new.user@example.test"))
                .thenReturn(Optional.of(user));
        when(credentials.findById(user.id()))
                .thenReturn(Optional.of(LocalUserCredential.create(
                        user.id(), "tenant-1", encoder.encode(password))));
        when(administrators.existsByUserIdAndDeletedAtIsNull(user.id())).thenReturn(false);

        var authenticated = service.authenticate("new.user@example.test", password, "corr");

        assertThat(authenticated.globalAdministrator()).isFalse();
        assertThat(authenticated.tenantId()).isEqualTo("tenant-1");
        assertThat(authenticated.objectId()).isEqualTo("local-user-" + user.id());
    }

    @Test
    void rejectsConfiguredUserWithoutGlobalAdministratorAssignment() throws Exception {
        ApplicationUser user = user("avery.admin@example.test", "ACTIVE");
        when(users.findByNormalizedContactEmailAndDeletedAtIsNull("avery.admin@example.test"))
                .thenReturn(Optional.of(user));
        when(administrators.existsByUserIdAndDeletedAtIsNull(user.id())).thenReturn(false);

        assertThatThrownBy(() -> service.authenticate("avery.admin@example.test", LOCAL_PASSWORD, "corr"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void rejectsInvitedAdministratorWithoutActivatingOrBinding() throws Exception {
        ApplicationUser user = user("avery.admin@example.test", "INVITED");
        when(users.findByNormalizedContactEmailAndDeletedAtIsNull("avery.admin@example.test"))
                .thenReturn(Optional.of(user));
        when(administrators.existsByUserIdAndDeletedAtIsNull(user.id())).thenReturn(true);

        assertThatThrownBy(() -> service.authenticate("avery.admin@example.test", LOCAL_PASSWORD, "corr"))
                .isInstanceOf(BadCredentialsException.class);
        assertThat(user.entraTenantId()).isNull();
        assertThat(user.entraObjectId()).isNull();
    }

    private static AuthSecurityProperties properties() {
        AuthSecurityProperties properties = new AuthSecurityProperties();
        properties.setLocalAuthEnabled(true);
        properties.getLocalAdmin().setUsername("avery.admin@example.test");
        properties.getLocalAdmin().setPassword(LOCAL_PASSWORD);
        properties.getLocalAdmin().setTenantId("dev-tenant");
        properties.getLocalAdmin().setObjectId("local-admin");
        return properties;
    }

    private static ApplicationUser user(String email, String accessStatus) throws Exception {
        var constructor = ApplicationUser.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        ApplicationUser user = constructor.newInstance();
        set(user, "id", UUID.randomUUID());
        set(user, "firstName", "Avery");
        set(user, "lastName", "Administrator");
        set(user, "normalizedContactEmail", email);
        set(user, "preProvisioningStatus", "PRE_PROVISIONED");
        set(user, "accessStatus", accessStatus);
        return user;
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field declaredField = target.getClass().getDeclaredField(field);
        declaredField.setAccessible(true);
        declaredField.set(target, value);
    }
}
