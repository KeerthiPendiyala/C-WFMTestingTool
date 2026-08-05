package com.ukgqtm.identity.api;

import java.util.UUID;

public record UserSummary(UUID id, String normalizedContactEmail, String accessStatus) {}
