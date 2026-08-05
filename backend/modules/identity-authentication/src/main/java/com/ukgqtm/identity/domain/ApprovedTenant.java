package com.ukgqtm.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "approved_tenant")
public class ApprovedTenant {
    @Id
    private String entraTenantId;

    @Column(nullable = false)
    private String displayName;

    private boolean active;

    @Column(nullable = false)
    private Instant approvedAt;

    @Version
    private int version;

    protected ApprovedTenant() {}
}
