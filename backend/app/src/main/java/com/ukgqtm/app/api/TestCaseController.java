package com.ukgqtm.app.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ukgqtm.app.security.AuthenticatedPrincipal;
import com.ukgqtm.app.security.AuthorizationPolicy;
import com.ukgqtm.app.security.AuthorizationPolicyService;
import com.ukgqtm.app.testcase.TestCaseApplicationService;
import com.ukgqtm.app.testcase.TestCaseApplicationService.AdhocListQuery;
import com.ukgqtm.app.testcase.TestCaseApplicationService.AdhocSelectionContext;
import com.ukgqtm.app.testcase.TestCaseApplicationService.CreateAdhocTestCaseCommand;
import com.ukgqtm.app.testcase.TestCaseApplicationService.CreateManualTestCaseCommand;
import com.ukgqtm.app.testcase.TestCaseApplicationService.ListQuery;
import com.ukgqtm.app.testcase.TestCaseApplicationService.RequirementSelectionContext;
import com.ukgqtm.app.testcase.TestCaseApplicationService.TestCaseGenerationResult;
import com.ukgqtm.app.testcase.TestCaseApplicationService.TestCaseSummary;
import com.ukgqtm.app.testcase.TestCaseApplicationService.UpdateTestCaseCommand;
import com.ukgqtm.app.testcase.TestCaseOperationException;
import com.ukgqtm.identity.api.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class TestCaseController {
    private final TestCaseApplicationService testCases;
    private final AuthorizationPolicyService authorization;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public TestCaseController(
            TestCaseApplicationService testCases,
            AuthorizationPolicyService authorization,
            ObjectMapper objectMapper,
            Validator validator) {
        this.testCases = testCases;
        this.authorization = authorization;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @GetMapping("/test-cases")
    @PreAuthorize("isAuthenticated()")
    public TestCaseListResponse list(
            Authentication authentication,
            @RequestParam("projectId") UUID projectId,
            @RequestParam(name = "projectSuiteAssignmentId", required = false) UUID projectSuiteAssignmentId,
            @RequestParam(name = "testCycleId", required = false) UUID testCycleId,
            @RequestParam(name = "requirementId", required = false) UUID requirementId,
            HttpServletRequest request) {
        require(authentication, AuthorizationPolicy.PROJECT_VIEW, projectId, null, request);
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        return new TestCaseListResponse(testCases.list(
                user, new ListQuery(projectId, projectSuiteAssignmentId, testCycleId, requirementId)));
    }

    @PostMapping(path = "/test-cases", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TestCaseSummary> createManual(
            Authentication authentication,
            @Valid @RequestBody CreateManualTestCaseCommand command,
            HttpServletRequest request) {
        require(authentication, AuthorizationPolicy.TEST_CASE_CREATE, command.projectId(), null, request);
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        TestCaseSummary created =
                testCases.createManual(user, command, request.getHeader(ApiHeaders.CORRELATION_ID));
        return ResponseEntity.created(URI.create("/api/v1/test-cases/" + created.id())).body(created);
    }

    @GetMapping("/test-cases/adhoc")
    @PreAuthorize("isAuthenticated()")
    public TestCaseListResponse listAdhoc(
            Authentication authentication,
            @RequestParam("projectId") UUID projectId,
            @RequestParam("projectSuiteAssignmentId") UUID projectSuiteAssignmentId,
            @RequestParam("testCycleId") UUID testCycleId,
            HttpServletRequest request) {
        require(authentication, AuthorizationPolicy.PROJECT_VIEW, projectId, null, request);
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        return new TestCaseListResponse(testCases.listAdhoc(
                user, new AdhocListQuery(projectId, projectSuiteAssignmentId, testCycleId)));
    }

    @PostMapping(path = "/test-cases/adhoc", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TestCaseSummary> createAdhocManual(
            Authentication authentication, @RequestBody JsonNode payload, HttpServletRequest request) {
        CreateAdhocTestCaseCommand command = readAdhocCommand(payload);
        require(authentication, AuthorizationPolicy.TEST_CASE_CREATE, command.projectId(), null, request);
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        TestCaseSummary created =
                testCases.createAdhocManual(user, command, request.getHeader(ApiHeaders.CORRELATION_ID));
        return ResponseEntity.created(URI.create("/api/v1/test-cases/" + created.id())).body(created);
    }

    @PostMapping(path = "/test-cases:generate", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TestCaseGenerationResult> generate(
            Authentication authentication,
            @Valid @RequestBody RequirementSelectionContext context,
            @RequestHeader(name = ApiHeaders.IDEMPOTENCY_KEY, required = false) String idempotencyKey,
            HttpServletRequest request) {
        require(authentication, AuthorizationPolicy.TEST_CASE_CREATE, context.projectId(), null, request);
        require(authentication, AuthorizationPolicy.GENERATION_JOB_ACCESS, context.projectId(), null, request);
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        TestCaseGenerationResult result = testCases.generateFromRequirement(
                user, context, idempotencyKey, request.getHeader(ApiHeaders.CORRELATION_ID));
        return ResponseEntity.created(URI.create("/api/v1/generation-jobs/" + result.jobId())).body(result);
    }

    @PostMapping(path = "/test-cases:import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TestCaseGenerationResult> importCsv(
            Authentication authentication,
            @RequestPart("csv") MultipartFile csv,
            @RequestParam("projectId") UUID projectId,
            @RequestParam(name = "projectSuiteAssignmentId", required = false) UUID projectSuiteAssignmentId,
            @RequestParam(name = "testCycleId", required = false) UUID testCycleId,
            @RequestParam("requirementId") UUID requirementId,
            @RequestHeader(name = ApiHeaders.IDEMPOTENCY_KEY, required = false) String idempotencyKey,
            HttpServletRequest request) {
        require(authentication, AuthorizationPolicy.TEST_CASE_CREATE, projectId, null, request);
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        TestCaseGenerationResult result = testCases.importCsv(
                user,
                new RequirementSelectionContext(projectId, projectSuiteAssignmentId, testCycleId, requirementId),
                csv,
                idempotencyKey,
                request.getHeader(ApiHeaders.CORRELATION_ID));
        return ResponseEntity.created(URI.create("/api/v1/generation-jobs/" + result.jobId())).body(result);
    }

    @PostMapping(path = "/test-cases/adhoc:import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TestCaseGenerationResult> importAdhocCsv(
            Authentication authentication,
            @RequestPart("csv") MultipartFile csv,
            @RequestParam("projectId") UUID projectId,
            @RequestParam("projectSuiteAssignmentId") UUID projectSuiteAssignmentId,
            @RequestParam("testCycleId") UUID testCycleId,
            @RequestParam(name = "requirementId", required = false) UUID requirementId,
            @RequestParam(name = "requirement_id", required = false) UUID snakeRequirementId,
            @RequestParam(name = "reqId", required = false) String reqId,
            @RequestHeader(name = ApiHeaders.IDEMPOTENCY_KEY, required = false) String idempotencyKey,
            HttpServletRequest request) {
        rejectAdhocRequirement(requirementId == null ? snakeRequirementId : requirementId, reqId);
        require(authentication, AuthorizationPolicy.TEST_CASE_CREATE, projectId, null, request);
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        TestCaseGenerationResult result = testCases.importAdhocCsv(
                user,
                new AdhocSelectionContext(projectId, projectSuiteAssignmentId, testCycleId),
                csv,
                idempotencyKey,
                request.getHeader(ApiHeaders.CORRELATION_ID));
        return ResponseEntity.created(URI.create("/api/v1/generation-jobs/" + result.jobId())).body(result);
    }

    @GetMapping(path = "/test-cases:csv-sample", produces = "text/csv")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> csvSample() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"test-case-upload-sample.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(TestCaseApplicationService.CSV_SAMPLE);
    }

    @GetMapping(path = "/test-cases/adhoc:csv-sample", produces = "text/csv")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> adhocCsvSample() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"adhoc-test-case-upload-sample.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(TestCaseApplicationService.CSV_SAMPLE);
    }

    @PatchMapping(path = "/test-cases/{testCaseId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public TestCaseSummary update(
            Authentication authentication,
            @PathVariable("testCaseId") UUID testCaseId,
            @RequestParam("projectId") UUID projectId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody UpdateTestCaseCommand command,
            HttpServletRequest request) {
        require(authentication, AuthorizationPolicy.TEST_CASE_EDIT, projectId, testCaseId, request);
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        return testCases.update(
                user, projectId, testCaseId, command, ifMatch, request.getHeader(ApiHeaders.CORRELATION_ID));
    }

    @DeleteMapping("/test-cases/{testCaseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable("testCaseId") UUID testCaseId,
            @RequestParam("projectId") UUID projectId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            HttpServletRequest request) {
        require(authentication, AuthorizationPolicy.TEST_CASE_DELETE_DRAFT, projectId, testCaseId, request);
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        testCases.delete(user, projectId, testCaseId, ifMatch, request.getHeader(ApiHeaders.CORRELATION_ID));
        return ResponseEntity.noContent().build();
    }

    private void require(
            Authentication authentication,
            AuthorizationPolicy policy,
            UUID projectId,
            UUID testCaseId,
            HttpServletRequest request) {
        authorization.require(
                authentication,
                policy,
                projectId,
                "TEST_CASE",
                testCaseId == null ? null : testCaseId.toString(),
                request.getHeader(ApiHeaders.CORRELATION_ID));
    }

    private CreateAdhocTestCaseCommand readAdhocCommand(JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            throw new TestCaseOperationException(HttpStatus.BAD_REQUEST, "The request body must be a JSON object.");
        }
        if (payload.hasNonNull("requirementId")
                || payload.hasNonNull("requirement_id")
                || payload.hasNonNull("reqId")) {
            throw new TestCaseOperationException(
                    HttpStatus.BAD_REQUEST, "Ad hoc test cases cannot include a requirement.");
        }
        CreateAdhocTestCaseCommand command;
        try {
            command = objectMapper.convertValue(payload, CreateAdhocTestCaseCommand.class);
        } catch (IllegalArgumentException exception) {
            throw new TestCaseOperationException(HttpStatus.BAD_REQUEST, "The request body failed validation.");
        }
        List<String> violations = validator.validate(command).stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();
        if (!violations.isEmpty()) {
            throw new TestCaseOperationException(
                    HttpStatus.BAD_REQUEST, "The request body failed validation.", violations);
        }
        return command;
    }

    private static void rejectAdhocRequirement(UUID requirementId, String reqId) {
        if (requirementId != null || (reqId != null && !reqId.isBlank())) {
            throw new TestCaseOperationException(
                    HttpStatus.BAD_REQUEST, "Ad hoc test cases cannot include a requirement.");
        }
    }

    public record TestCaseListResponse(List<TestCaseSummary> testCases) {}
}
