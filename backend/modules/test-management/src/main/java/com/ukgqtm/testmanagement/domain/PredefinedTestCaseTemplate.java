package com.ukgqtm.testmanagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "predefined_test_case_template")
public class PredefinedTestCaseTemplate {
    @Id
    private UUID id;

    private String tenantId;
    private UUID suiteId;

    @Column(nullable = false)
    private String templateKey;

    @Column(nullable = false)
    private String header;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String source;

    private boolean active;
    private Instant deletedAt;

    @Version
    private int version;

    protected PredefinedTestCaseTemplate() {}
}
