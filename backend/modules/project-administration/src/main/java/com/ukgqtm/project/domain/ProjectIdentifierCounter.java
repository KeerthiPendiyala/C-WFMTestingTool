package com.ukgqtm.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@IdClass(ProjectIdentifierCounterId.class)
@Table(name = "project_identifier_counter")
public class ProjectIdentifierCounter {
    @Id
    private UUID projectId;

    @Id
    private String identifierType;

    @Column(nullable = false)
    private int nextValue;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private int version;

    protected ProjectIdentifierCounter() {}
}
