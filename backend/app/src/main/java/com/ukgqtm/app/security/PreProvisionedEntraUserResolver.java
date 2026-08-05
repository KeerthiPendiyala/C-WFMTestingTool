package com.ukgqtm.app.security;

import com.ukgqtm.audit.domain.AuditEvent;
import com.ukgqtm.audit.repository.AuditEventRepository;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.identity.api.AuthenticatedUserResolver;
import com.ukgqtm.identity.api.AuthenticationDeniedException;
import com.ukgqtm.identity.api.EntraTokenClaims;
import com.ukgqtm.identity.domain.ApplicationUser;
import com.ukgqtm.identity.repository.ApplicationUserRepository;
import com.ukgqtm.identity.repository.ApprovedTenantRepository;
import com.ukgqtm.identity.repository.GlobalAdministratorAssignmentRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "app.security.oauth2-resource-server-enabled", havingValue = "true")
public class PreProvisionedEntraUserResolver implements AuthenticatedUserResolver {
    private final ApplicationUserRepository users;
    private final ApprovedTenantRepository approvedTenants;
    private final GlobalAdministratorAssignmentRepository administrators;
    private final AuditEventRepository auditEvents;

    public PreProvisionedEntraUserResolver(
            ApplicationUserRepository users,
            ApprovedTenantRepository approvedTenants,
            GlobalAdministratorAssignmentRepository administrators,
            AuditEventRepository auditEvents) {
        this.users = users;
        this.approvedTenants = approvedTenants;
        this.administrators = administrators;
        this.auditEvents = auditEvents;
    }

    @Override
    @Transactional
    public AuthenticatedUser resolve(EntraTokenClaims claims) {
        requireClaim(claims.tenantId(), "tid");
        requireClaim(claims.objectId(), "oid");

        if (!approvedTenants.existsByEntraTenantIdAndActive(claims.tenantId(), true)) {
            deny("AUTH_LOGIN_DENIED_UNAPPROVED_TENANT", claims, "Tenant is not approved.");
        }

        var existing = users.findForUpdateByEntraTenantIdAndEntraObjectIdAndDeletedAtIsNull(
                claims.tenantId(), claims.objectId());
        if (existing.isPresent()) {
            ApplicationUser user = existing.get();
            if (user.isDisabled()) {
                deny("AUTH_LOGIN_DENIED_DISABLED_USER", claims, "User is disabled.");
            }
            user.recordSuccessfulLogin(claims.email(), claims.preferredUsername(), claims.name());
            return toAuthenticatedUser(user);
        }

        Set<String> candidateEmails = candidateEmails(claims);
        if (candidateEmails.isEmpty()) {
            deny("AUTH_LINK_FAILED_UNPROVISIONED_USER", claims, "Token does not contain a linkable contact claim.");
        }
        List<ApplicationUser> candidates = users.findByNormalizedContactEmailInAndDeletedAtIsNull(candidateEmails);
        if (candidates.isEmpty()) {
            deny("AUTH_LINK_FAILED_UNPROVISIONED_USER", claims, "User is not pre-provisioned.");
        }
        if (candidates.size() > 1) {
            deny("AUTH_LINK_FAILED_AMBIGUOUS_USER", claims, "Token contact claims match multiple users.");
        }

        ApplicationUser candidate = candidates.getFirst();
        if (candidate.isDisabled()) {
            deny("AUTH_LOGIN_DENIED_DISABLED_USER", claims, "User is disabled.");
        }
        if (candidate.hasAnyBinding()) {
            if (candidate.isBoundTo(claims.tenantId(), claims.objectId())) {
                candidate.recordSuccessfulLogin(claims.email(), claims.preferredUsername(), claims.name());
                return toAuthenticatedUser(candidate);
            }
            deny("AUTH_LINK_FAILED_ALREADY_LINKED", claims, "Pre-provisioned user is already bound to another identity.");
        }
        if (!candidate.isInvitedForFirstBinding()) {
            deny("AUTH_LINK_FAILED_NOT_INVITED", claims, "Pre-provisioned user is not in invited state.");
        }

        candidate.bindToEntraIdentity(
                claims.tenantId(), claims.objectId(), claims.email(), claims.preferredUsername(), claims.name());
        auditEvents.save(AuditEvent.authentication(
                "AUTH_LINK_SUCCEEDED",
                candidate.id().toString(),
                claims.tenantId(),
                candidate.id().toString(),
                claims.correlationId()));
        return toAuthenticatedUser(candidate);
    }

    @Override
    @Transactional
    public void observeLogout(AuthenticatedUser user, String correlationId) {
        auditEvents.save(AuditEvent.authentication(
                "AUTH_LOGOUT_OBSERVED",
                user.userId().toString(),
                user.tenantId(),
                user.userId().toString(),
                correlationId));
    }

    private AuthenticatedUser toAuthenticatedUser(ApplicationUser user) {
        return new AuthenticatedUser(
                user.id(),
                user.entraTenantId(),
                user.entraObjectId(),
                user.firstName(),
                user.lastName(),
                user.normalizedContactEmail(),
                administrators.existsByUserIdAndDeletedAtIsNull(user.id()));
    }

    private void deny(String action, EntraTokenClaims claims, String message) {
        auditEvents.save(AuditEvent.authentication(
                action, claims.objectId(), claims.tenantId(), claims.email(), claims.correlationId()));
        throw new AuthenticationDeniedException(message);
    }

    private static Set<String> candidateEmails(EntraTokenClaims claims) {
        Set<String> values = new LinkedHashSet<>();
        addNormalized(values, claims.email());
        addNormalized(values, claims.preferredUsername());
        return values;
    }

    private static void addNormalized(Set<String> values, String value) {
        if (value != null && !value.isBlank()) {
            values.add(value.trim().toLowerCase(Locale.ROOT));
        }
    }

    private static void requireClaim(String value, String claimName) {
        if (value == null || value.isBlank()) {
            throw new AuthenticationDeniedException("Missing required token claim: " + claimName);
        }
    }
}
