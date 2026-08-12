package com.ukgqtm.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "local_user_credential")
public class LocalUserCredential {
    @Id
    private UUID userId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected LocalUserCredential() {}

    public static LocalUserCredential create(UUID userId, String tenantId, String passwordHash) {
        LocalUserCredential credential = new LocalUserCredential();
        credential.userId = userId;
        credential.tenantId = tenantId;
        credential.passwordHash = passwordHash;
        credential.createdAt = Instant.now();
        credential.updatedAt = credential.createdAt;
        return credential;
    }

    public UUID userId() {
        return userId;
    }

    public String tenantId() {
        return tenantId;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public void resetPassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.updatedAt = Instant.now();
    }
}
