package com.ukgqtm.requirements.api;

import java.util.UUID;

public record RequirementSummary(
        UUID id,
        UUID projectId,
        String reqId,
        String header,
        UUID projectSuiteAssignmentId,
        UUID testCycleId,
        String status) {}
