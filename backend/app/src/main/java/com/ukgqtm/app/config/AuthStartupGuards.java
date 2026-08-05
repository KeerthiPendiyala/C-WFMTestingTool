package com.ukgqtm.app.config;

import com.ukgqtm.audit.domain.AuditEvent;
import com.ukgqtm.audit.repository.AuditEventRepository;
import com.ukgqtm.identity.domain.GlobalAdministratorAssignment;
import com.ukgqtm.identity.domain.ApplicationUser;
import com.ukgqtm.identity.domain.LocalUserCredential;
import com.ukgqtm.identity.repository.ApplicationUserRepository;
import com.ukgqtm.identity.repository.GlobalAdministratorAssignmentRepository;
import com.ukgqtm.identity.repository.LocalUserCredentialRepository;
import java.util.Locale;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuthStartupGuards implements ApplicationRunner {
    private final AuthSecurityProperties securityProperties;
    private final ApplicationUserRepository users;
    private final GlobalAdministratorAssignmentRepository administrators;
    private final AuditEventRepository auditEvents;
    private final LocalUserCredentialRepository credentials;
    private final PasswordEncoder passwordEncoder;

    public AuthStartupGuards(
            AuthSecurityProperties securityProperties,
            ApplicationUserRepository users,
            GlobalAdministratorAssignmentRepository administrators,
            AuditEventRepository auditEvents,
            LocalUserCredentialRepository credentials,
            PasswordEncoder passwordEncoder) {
        this.securityProperties = securityProperties;
        this.users = users;
        this.administrators = administrators;
        this.auditEvents = auditEvents;
        this.credentials = credentials;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        bootstrapFirstAdministrator();
        ensureDevelopmentAdministrator();
    }

    private void bootstrapFirstAdministrator() {
        AuthSecurityProperties.BootstrapAdmin bootstrap = securityProperties.getBootstrapAdmin();
        if (!bootstrap.isEnabled()) {
            return;
        }
        if (isBlank(bootstrap.getNormalizedEmail())) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_EMAIL is required when bootstrap admin is enabled.");
        }
        if (administrators.existsByDeletedAtIsNull()) {
            throw new IllegalStateException(
                    "Bootstrap admin must be disabled after the first active Administrator assignment exists.");
        }

        String normalizedEmail = bootstrap.getNormalizedEmail().trim().toLowerCase(Locale.ROOT);
        var user = users.findByNormalizedContactEmailAndDeletedAtIsNull(normalizedEmail)
                .orElseThrow(() -> new IllegalStateException("Bootstrap admin user must be pre-provisioned."));
        if (user.isDisabled()) {
            throw new IllegalStateException("Bootstrap admin user is disabled.");
        }
        administrators.save(GlobalAdministratorAssignment.bootstrap(user.id()));
        auditEvents.save(AuditEvent.authentication(
                "AUTH_BOOTSTRAP_ADMIN_ASSIGNED", user.id().toString(), user.entraTenantId(), user.id().toString(), null));
    }

    private void ensureDevelopmentAdministrator() {
        AuthSecurityProperties.DevelopmentAdmin development = securityProperties.getDevelopmentAdmin();
        if (!development.isEnabled()) {
            return;
        }
        if (securityProperties.isProduction()) {
            throw new IllegalStateException("Development Administrator is blocked in production.");
        }
        if (!securityProperties.isLocalAuthEnabled()) {
            throw new IllegalStateException("LOCAL_AUTH_ENABLED must be true for the Development Administrator.");
        }
        if (isBlank(development.getEmail())) {
            throw new IllegalStateException("DEV_ADMIN_EMAIL is required when Development Administrator is enabled.");
        }
        if (isBlank(development.getPassword()) && isBlank(development.getPasswordHash())) {
            throw new IllegalStateException(
                    "DEV_ADMIN_PASSWORD or DEV_ADMIN_PASSWORD_HASH is required when Development Administrator is enabled.");
        }
        String email = development.getEmail().trim().toLowerCase(Locale.ROOT);
        ApplicationUser user = users.findByNormalizedContactEmailAndDeletedAtIsNull(email)
                .orElseGet(() -> users.save(ApplicationUser.localUser(
                        development.getFirstName().trim(),
                        development.getLastName().trim(),
                        email,
                        true,
                        false)));
        if (!user.isActive()) {
            user.activate();
        }
        if (!administrators.existsByUserIdAndDeletedAtIsNull(user.id())) {
            administrators.save(GlobalAdministratorAssignment.bootstrap(user.id()));
        }
        if (!credentials.existsById(user.id())) {
            String passwordHash = isBlank(development.getPasswordHash())
                    ? passwordEncoder.encode(development.getPassword())
                    : development.getPasswordHash().trim();
            credentials.save(LocalUserCredential.create(user.id(), development.getTenantId(), passwordHash));
        }
        auditEvents.save(AuditEvent.authentication(
                "AUTH_DEVELOPMENT_ADMIN_READY",
                user.id().toString(),
                development.getTenantId(),
                user.id().toString(),
                null));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
