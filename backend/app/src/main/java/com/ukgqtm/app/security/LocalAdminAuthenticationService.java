package com.ukgqtm.app.security;

import com.ukgqtm.app.config.AuthSecurityProperties;
import com.ukgqtm.audit.domain.AuditEvent;
import com.ukgqtm.audit.repository.AuditEventRepository;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.identity.domain.ApplicationUser;
import com.ukgqtm.identity.repository.ApplicationUserRepository;
import com.ukgqtm.identity.repository.GlobalAdministratorAssignmentRepository;
import com.ukgqtm.identity.repository.LocalUserCredentialRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "app.security.local-auth-enabled", havingValue = "true")
public class LocalAdminAuthenticationService {
    private final AuthSecurityProperties securityProperties;
    private final ApplicationUserRepository users;
    private final GlobalAdministratorAssignmentRepository administrators;
    private final LocalUserCredentialRepository credentials;
    private final AuditEventRepository auditEvents;
    private final PasswordEncoder passwordEncoder;

    public LocalAdminAuthenticationService(
            AuthSecurityProperties securityProperties,
            ApplicationUserRepository users,
            GlobalAdministratorAssignmentRepository administrators,
            LocalUserCredentialRepository credentials,
            AuditEventRepository auditEvents,
            PasswordEncoder passwordEncoder) {
        this.securityProperties = securityProperties;
        this.users = users;
        this.administrators = administrators;
        this.credentials = credentials;
        this.auditEvents = auditEvents;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthenticatedUser authenticate(String username, String password, String correlationId) {
        String normalizedUsername = normalize(username);
        ApplicationUser databaseUser = normalizedUsername == null
                ? null
                : users.findByNormalizedContactEmailAndDeletedAtIsNull(normalizedUsername).orElse(null);
        if (databaseUser != null) {
            var credential = credentials.findById(databaseUser.id()).orElse(null);
            if (credential != null
                    && databaseUser.isActive()
                    && password != null
                    && passwordEncoder.matches(password, credential.passwordHash())) {
                databaseUser.recordSuccessfulLogin(normalizedUsername, normalizedUsername,
                        databaseUser.firstName() + " " + databaseUser.lastName());
                boolean administrator = administrators.existsByUserIdAndDeletedAtIsNull(databaseUser.id());
                auditEvents.save(AuditEvent.authentication(
                        "AUTH_LOCAL_USER_LOGIN_SUCCEEDED",
                        databaseUser.id().toString(),
                        credential.tenantId(),
                        databaseUser.id().toString(),
                        correlationId));
                return new AuthenticatedUser(
                        databaseUser.id(),
                        credential.tenantId(),
                        "local-user-" + databaseUser.id(),
                        databaseUser.firstName(),
                        databaseUser.lastName(),
                        databaseUser.normalizedContactEmail(),
                        administrator);
            }
        }

        AuthSecurityProperties.LocalAdmin localAdmin = securityProperties.getLocalAdmin();
        String configuredUsername = normalize(localAdmin.getUsername());

        if (configuredUsername == null
                || !configuredUsername.equals(normalizedUsername)
                || !passwordMatches(password, localAdmin)) {
            auditFailure(normalizedUsername, correlationId);
            throw new BadCredentialsException("Invalid local Administrator credentials.");
        }

        ApplicationUser user = users.findByNormalizedContactEmailAndDeletedAtIsNull(configuredUsername)
                .filter(ApplicationUser::isActive)
                .filter(candidate -> administrators.existsByUserIdAndDeletedAtIsNull(candidate.id()))
                .orElseThrow(() -> {
                    auditFailure(normalizedUsername, correlationId);
                    return new BadCredentialsException("Invalid local Administrator credentials.");
                });

        auditEvents.save(AuditEvent.authentication(
                "AUTH_LOCAL_ADMIN_LOGIN_SUCCEEDED",
                user.id().toString(),
                localAdmin.getTenantId(),
                user.id().toString(),
                correlationId));
        return new AuthenticatedUser(
                user.id(),
                localAdmin.getTenantId(),
                localAdmin.getObjectId(),
                user.firstName(),
                user.lastName(),
                user.normalizedContactEmail(),
                true);
    }

    @Transactional
    public void observeLogout(AuthenticatedUser user, String correlationId) {
        auditEvents.save(AuditEvent.authentication(
                "AUTH_LOCAL_ADMIN_LOGOUT_OBSERVED",
                user.userId().toString(),
                user.tenantId(),
                user.userId().toString(),
                correlationId));
    }

    public boolean isLocalPrincipal(AuthenticatedUser user) {
        AuthSecurityProperties.LocalAdmin localAdmin = securityProperties.getLocalAdmin();
        return user.objectId().startsWith("local-user-")
                || (localAdmin.getTenantId().equals(user.tenantId()) && localAdmin.getObjectId().equals(user.objectId()));
    }

    private boolean passwordMatches(String submittedPassword, AuthSecurityProperties.LocalAdmin localAdmin) {
        if (submittedPassword == null) {
            return false;
        }
        if (!isBlank(localAdmin.getPasswordHash())) {
            return passwordEncoder.matches(submittedPassword, localAdmin.getPasswordHash());
        }
        if (!isBlank(localAdmin.getPassword())) {
            return MessageDigest.isEqual(
                    submittedPassword.getBytes(StandardCharsets.UTF_8),
                    localAdmin.getPassword().getBytes(StandardCharsets.UTF_8));
        }
        return false;
    }

    private void auditFailure(String normalizedUsername, String correlationId) {
        auditEvents.save(AuditEvent.authentication(
                "AUTH_LOCAL_ADMIN_LOGIN_DENIED",
                normalizedUsername,
                securityProperties.getLocalAdmin().getTenantId(),
                normalizedUsername,
                correlationId));
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
