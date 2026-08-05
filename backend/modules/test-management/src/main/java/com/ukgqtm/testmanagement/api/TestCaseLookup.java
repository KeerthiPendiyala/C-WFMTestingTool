package com.ukgqtm.testmanagement.api;

import java.util.List;
import java.util.UUID;

public interface TestCaseLookup {
    long countActiveTestCasesForRequirement(UUID requirementId);

    List<TestCaseSummary> findProjectTestCases(UUID projectId);
}
