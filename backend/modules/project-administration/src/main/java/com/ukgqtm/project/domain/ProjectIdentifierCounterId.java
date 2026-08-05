package com.ukgqtm.project.domain;

import java.io.Serializable;
import java.util.UUID;

public class ProjectIdentifierCounterId implements Serializable {
    private UUID projectId;
    private String identifierType;

    protected ProjectIdentifierCounterId() {}

    public ProjectIdentifierCounterId(UUID projectId, String identifierType) {
        this.projectId = projectId;
        this.identifierType = identifierType;
    }
}
