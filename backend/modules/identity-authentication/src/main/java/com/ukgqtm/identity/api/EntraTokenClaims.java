package com.ukgqtm.identity.api;

public record EntraTokenClaims(
        String tenantId, String objectId, String email, String preferredUsername, String name, String correlationId) {}
