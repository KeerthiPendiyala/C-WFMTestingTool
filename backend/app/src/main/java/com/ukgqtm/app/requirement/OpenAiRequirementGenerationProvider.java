package com.ukgqtm.app.requirement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ukgqtm.ai.api.RequirementGenerationProvider;
import com.ukgqtm.app.config.OpenAiProperties;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OpenAiRequirementGenerationProvider implements RequirementGenerationProvider {
    private static final String DEVELOPER_PROMPT = """
            Convert the supplied requirement document into discrete, testable software requirements.
            Preserve the source meaning. Do not invent business rules. Return every required JSON field.
            Acceptance criteria must be independently verifiable. Use empty arrays when assumptions or
            dependencies are not stated or safely implied by the source.
            """;
    private static final String TEST_CASE_DEVELOPER_PROMPT = """
            Create concise requirement-linked QA test case candidates from the supplied requirement.
            Use the requirement text as data. Do not invent unrelated business rules, credentials, or
            execution evidence. Return only structured test cases with a header and description.
            The test case header must contain only the test case header details; do not prefix or append
            the ReqID, requirement header, or requirement description.
            """;

    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Autowired
    public OpenAiRequirementGenerationProvider(
            OpenAiProperties properties, ObjectMapper objectMapper, RestClient.Builder builder) {
        this(properties, objectMapper, createClient(properties, builder));
    }

    OpenAiRequirementGenerationProvider(
            OpenAiProperties properties, ObjectMapper objectMapper, RestClient restClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClient;
    }

    private static RestClient createClient(OpenAiProperties properties, RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return builder.baseUrl(properties.getBaseUrl().toString()).requestFactory(requestFactory).build();
    }

    @Override
    public GenerationResult generate(GenerationRequest request) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new RequirementGenerationException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI requirement generation is not configured. Contact the application administrator.");
        }

        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "store", false,
                "input", List.of(
                        message("developer", DEVELOPER_PROMPT),
                        message("user", "Document: " + request.documentName() + "\n\n" + request.documentContent())),
                "text", Map.of("format", Map.of(
                        "type", "json_schema",
                        "name", "generated_requirements",
                        "strict", true,
                        "schema", responseSchema())));

        try {
            JsonNode response = restClient.post()
                    .uri("/responses")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String outputText = extractOutputText(response);
            GeneratedEnvelope envelope = objectMapper.readValue(outputText, GeneratedEnvelope.class);
            return new GenerationResult(properties.getModel(), envelope.requirements());
        } catch (RequirementGenerationException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new RequirementGenerationException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI could not generate requirements right now. Try again shortly.",
                    exception);
        } catch (Exception exception) {
            throw new RequirementGenerationException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI returned an invalid requirement response. Please retry.",
                    exception);
        }
    }

    @Override
    public TestCaseGenerationResult generateTestCases(TestCaseGenerationRequest request) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new RequirementGenerationException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI test case generation is not configured. Contact the application administrator.");
        }

        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "store", false,
                "input", List.of(
                        message("developer", TEST_CASE_DEVELOPER_PROMPT),
                        message(
                                "user",
                                "ReqID: " + request.reqId()
                                        + "\nHeader: " + request.header()
                                        + "\nDescription:\n" + request.description())),
                "text", Map.of("format", Map.of(
                        "type", "json_schema",
                        "name", "generated_test_cases",
                        "strict", true,
                        "schema", testCaseResponseSchema())));

        try {
            JsonNode response = restClient.post()
                    .uri("/responses")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String outputText = extractOutputText(response);
            GeneratedTestCaseEnvelope envelope = objectMapper.readValue(outputText, GeneratedTestCaseEnvelope.class);
            return new TestCaseGenerationResult(properties.getModel(), envelope.testCases());
        } catch (RequirementGenerationException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new RequirementGenerationException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI could not generate test cases right now. Try again shortly.",
                    exception);
        } catch (Exception exception) {
            throw new RequirementGenerationException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI returned an invalid test case response. Please retry.",
                    exception);
        }
    }

    private static Map<String, Object> message(String role, String text) {
        return Map.of("role", role, "content", List.of(Map.of("type", "input_text", "text", text)));
    }

    private static String extractOutputText(JsonNode response) {
        if (response != null) {
            for (JsonNode output : response.path("output")) {
                for (JsonNode content : output.path("content")) {
                    if ("output_text".equals(content.path("type").asText()) && content.hasNonNull("text")) {
                        return content.path("text").asText();
                    }
                    if ("refusal".equals(content.path("type").asText())) {
                        throw new RequirementGenerationException(
                                HttpStatus.UNPROCESSABLE_ENTITY,
                                "OpenAI could not process the supplied document.");
                    }
                }
            }
        }
        throw new RequirementGenerationException(
                HttpStatus.BAD_GATEWAY, "OpenAI returned no generated requirements. Please retry.");
    }

    private static Map<String, Object> responseSchema() {
        Map<String, Object> stringArray = Map.of("type", "array", "items", Map.of("type", "string"));
        Map<String, Object> requirement = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("header", "description", "acceptanceCriteria", "assumptions", "dependencies"),
                "properties", Map.of(
                        "header", Map.of("type", "string"),
                        "description", Map.of("type", "string"),
                        "acceptanceCriteria", stringArray,
                        "assumptions", stringArray,
                        "dependencies", stringArray));
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("requirements"),
                "properties", Map.of("requirements", Map.of("type", "array", "items", requirement)));
    }

    private static Map<String, Object> testCaseResponseSchema() {
        Map<String, Object> testCase = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("header", "description"),
                "properties", Map.of(
                        "header", Map.of("type", "string"),
                        "description", Map.of("type", "string")));
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("testCases"),
                "properties", Map.of("testCases", Map.of("type", "array", "items", testCase)));
    }

    private record GeneratedEnvelope(List<GeneratedRequirement> requirements) {}

    private record GeneratedTestCaseEnvelope(List<GeneratedTestCase> testCases) {}
}
