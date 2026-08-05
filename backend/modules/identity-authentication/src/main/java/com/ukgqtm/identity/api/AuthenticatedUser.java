package com.ukgqtm.identity.api;

import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        String tenantId,
        String objectId,
        String firstName,
        String lastName,
        String contactEmail,
        boolean globalAdministrator) {
    public String immutablePrincipalKey() {
        return tenantId + ":" + objectId;
    }
}
