package com.ukgqtm.testmanagement.api;

import java.time.LocalDate;
import java.util.UUID;

public record TestCaseSummary(
        UUID id,
        UUID projectId,
        String testCaseId,
        String reqId,
        String header,
        String status,
        LocalDate dueDate) {}
