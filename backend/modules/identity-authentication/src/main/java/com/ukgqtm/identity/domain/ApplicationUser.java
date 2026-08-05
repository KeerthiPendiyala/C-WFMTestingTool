package com.ukgqtm.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "application_user")
public class ApplicationUser {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String normalizedContactEmail;

    @Column(nullable = false)
    private String preProvisioningStatus;

    @Column(nullable = false)
    private String accessStatus;

    private String entraTenantId;
    private String entraObjectId;
    private Instant firstLoginAt;
    private Instant lastLoginAt;
    private String lastClaimEmail;
    private String lastClaimPreferredUsername;
    private String lastClaimName;
    private boolean assignmentScoped;
    private Instant deletedAt;

    @Version
    private int version;

    protected ApplicationUser() {}

    public static ApplicationUser preProvision(String firstName, String lastName, String normalizedContactEmail, UUID actorId) {
        ApplicationUser user = new ApplicationUser();
        user.id = UUID.randomUUID();
        user.firstName = firstName;
        user.lastName = lastName;
        user.normalizedContactEmail = normalizedContactEmail;
        user.preProvisioningStatus = "PRE_PROVISIONED";
        user.accessStatus = "INVITED";
        user.firstLoginAt = null;
        user.lastLoginAt = null;
        return user;
    }

    public static ApplicationUser localUser(
            String firstName,
            String lastName,
            String normalizedContactEmail,
            boolean active,
            boolean assignmentScoped) {
        ApplicationUser user = preProvision(firstName, lastName, normalizedContactEmail, null);
        user.accessStatus = active ? "ACTIVE" : "DISABLED";
        user.assignmentScoped = assignmentScoped;
        return user;
    }

    public UUID id() {
        return id;
    }

    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
    }

    public String normalizedContactEmail() {
        return normalizedContactEmail;
    }

    public String preProvisioningStatus() {
        return preProvisioningStatus;
    }

    public String accessStatus() {
        return accessStatus;
    }

    public String entraTenantId() {
        return entraTenantId;
    }

    public String entraObjectId() {
        return entraObjectId;
    }

    public boolean isDisabled() {
        return "DISABLED".equals(accessStatus);
    }

    public boolean isInvitedForFirstBinding() {
        return "PRE_PROVISIONED".equals(preProvisioningStatus) && "INVITED".equals(accessStatus);
    }

    public boolean isBoundTo(String tenantId, String objectId) {
        return tenantId != null
                && objectId != null
                && tenantId.equals(entraTenantId)
                && objectId.equals(entraObjectId);
    }

    public boolean hasAnyBinding() {
        return entraTenantId != null || entraObjectId != null;
    }

    public boolean isActive() {
        return "ACTIVE".equals(accessStatus);
    }

    public boolean assignmentScoped() {
        return assignmentScoped;
    }

    public void activate() {
        this.accessStatus = "ACTIVE";
    }

    public void bindToEntraIdentity(String tenantId, String objectId, String email, String preferredUsername, String name) {
        Instant now = Instant.now();
        this.entraTenantId = tenantId;
        this.entraObjectId = objectId;
        this.preProvisioningStatus = "BOUND";
        this.accessStatus = "ACTIVE";
        this.firstLoginAt = now;
        recordSuccessfulLogin(email, preferredUsername, name, now);
    }

    public void recordSuccessfulLogin(String email, String preferredUsername, String name) {
        recordSuccessfulLogin(email, preferredUsername, name, Instant.now());
    }

    private void recordSuccessfulLogin(String email, String preferredUsername, String name, Instant now) {
        this.lastLoginAt = now;
        this.lastClaimEmail = normalizeNullable(email);
        this.lastClaimPreferredUsername = normalizeNullable(preferredUsername);
        this.lastClaimName = blankToNull(name);
    }

    private static String normalizeNullable(String value) {
        String cleaned = blankToNull(value);
        return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
