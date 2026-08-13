package com.ukgqtm.ai.api;

import java.util.List;

public interface RequirementGenerationProvider {
    GenerationResult generate(GenerationRequest request);

    TestCaseGenerationResult generateTestCases(TestCaseGenerationRequest request);

    record GenerationRequest(String documentName, String documentContent) {}

    record GenerationResult(String model, List<GeneratedRequirement> requirements) {}

    record GeneratedRequirement(
            String header,
            String description,
            List<String> acceptanceCriteria,
            List<String> assumptions,
            List<String> dependencies) {}

    record TestCaseGenerationRequest(
            String reqId, String header, String description, String acceptanceCriteria, String dependencies) {}

    record TestCaseGenerationResult(String model, List<GeneratedTestCase> testCases) {}

    record GeneratedTestCase(String header, String description) {}
}
