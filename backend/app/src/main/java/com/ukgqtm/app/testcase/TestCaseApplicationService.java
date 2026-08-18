package com.ukgqtm.app.testcase;

import com.ukgqtm.ai.api.RequirementGenerationProvider;
import com.ukgqtm.ai.api.RequirementGenerationProvider.GeneratedTestCase;
import com.ukgqtm.ai.api.RequirementGenerationProvider.TestCaseGenerationRequest;
import com.ukgqtm.app.api.ApiConflictException;
import com.ukgqtm.app.security.AssignmentScopeAuthorizationService;
import com.ukgqtm.audit.domain.AuditEvent;
import com.ukgqtm.audit.repository.AuditEventRepository;
import com.ukgqtm.ai.domain.GenerationJob;
import com.ukgqtm.ai.repository.GenerationJobRepository;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.identity.domain.ApplicationUser;
import com.ukgqtm.identity.repository.ApplicationUserRepository;
import com.ukgqtm.project.domain.Project;
import com.ukgqtm.project.domain.ProjectMembership;
import com.ukgqtm.project.domain.ProjectSuiteAssignment;
import com.ukgqtm.project.domain.ProjectTestCycle;
import com.ukgqtm.project.domain.TestSuite;
import com.ukgqtm.project.repository.ProjectIdentifierCounterRepository;
import com.ukgqtm.project.repository.ProjectMembershipRepository;
import com.ukgqtm.project.repository.ProjectRepository;
import com.ukgqtm.project.repository.ProjectSuiteAssignmentRepository;
import com.ukgqtm.project.repository.ProjectTestCycleRepository;
import com.ukgqtm.project.repository.TestSuiteRepository;
import com.ukgqtm.requirements.domain.Requirement;
import com.ukgqtm.requirements.repository.RequirementRepository;
import com.ukgqtm.testmanagement.domain.TestCase;
import com.ukgqtm.testmanagement.repository.TestCaseRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TestCaseApplicationService {
    public static final String CSV_SAMPLE = "Test Case Header,Description\r\n"
            + "Validate employee clock-in,Confirm an active employee can clock in successfully.\r\n";
    private static final List<String> STATUSES =
            List.of("Draft", "Inprogress", "Defect", "Resolved", "Not applicable", "Retest");

    private final ProjectRepository projects;
    private final ProjectSuiteAssignmentRepository suiteAssignments;
    private final TestSuiteRepository suites;
    private final ProjectTestCycleRepository cycles;
    private final RequirementRepository requirements;
    private final TestCaseRepository testCases;
    private final ProjectIdentifierCounterRepository counters;
    private final ProjectMembershipRepository memberships;
    private final ApplicationUserRepository users;
    private final GenerationJobRepository generationJobs;
    private final RequirementGenerationProvider aiProvider;
    private final AuditEventRepository auditEvents;
    private final AssignmentScopeAuthorizationService assignmentScope;

    public TestCaseApplicationService(
            ProjectRepository projects,
            ProjectSuiteAssignmentRepository suiteAssignments,
            TestSuiteRepository suites,
            ProjectTestCycleRepository cycles,
            RequirementRepository requirements,
            TestCaseRepository testCases,
            ProjectIdentifierCounterRepository counters,
            ProjectMembershipRepository memberships,
            ApplicationUserRepository users,
            GenerationJobRepository generationJobs,
            RequirementGenerationProvider aiProvider,
            AuditEventRepository auditEvents,
            AssignmentScopeAuthorizationService assignmentScope) {
        this.projects = projects;
        this.suiteAssignments = suiteAssignments;
        this.suites = suites;
        this.cycles = cycles;
        this.requirements = requirements;
        this.testCases = testCases;
        this.counters = counters;
        this.memberships = memberships;
        this.users = users;
        this.generationJobs = generationJobs;
        this.aiProvider = aiProvider;
        this.auditEvents = auditEvents;
        this.assignmentScope = assignmentScope;
    }

    @Transactional(readOnly = true)
    public List<TestCaseSummary> list(AuthenticatedUser user, ListQuery query) {
        requireProject(user, query.projectId());
        return testCases.findByTenantIdAndProjectIdAndDeletedAtIsNullOrderByCreatedDateDesc(
                        user.tenantId(), query.projectId())
                .stream()
                .filter(testCase -> query.projectSuiteAssignmentId() == null
                        || query.projectSuiteAssignmentId().equals(testCase.projectSuiteAssignmentId()))
                .filter(testCase -> query.testCycleId() == null
                        || query.testCycleId().equals(testCase.testCycleId()))
                .filter(testCase -> query.requirementId() == null
                        || query.requirementId().equals(testCase.requirementId()))
                .filter(testCase -> assignmentScope.canAccess(
                        user, testCase.projectId(), testCase.projectSuiteAssignmentId(), testCase.testCycleId()))
                .map(testCase -> summarize(user, testCase))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TestCaseSummary> listAdhoc(AuthenticatedUser user, AdhocListQuery query) {
        requireAdhocScope(user, query.context());
        return testCases.findByTenantIdAndProjectIdAndDeletedAtIsNullOrderByCreatedDateDesc(
                        user.tenantId(), query.projectId())
                .stream()
                .filter(testCase -> testCase.requirementId() == null)
                .filter(testCase -> query.projectSuiteAssignmentId().equals(testCase.projectSuiteAssignmentId()))
                .filter(testCase -> query.testCycleId().equals(testCase.testCycleId()))
                .filter(testCase -> assignmentScope.canAccess(
                        user, testCase.projectId(), testCase.projectSuiteAssignmentId(), testCase.testCycleId()))
                .map(testCase -> summarize(user, testCase))
                .toList();
    }

    @Transactional
    public TestCaseSummary createManual(
            AuthenticatedUser actor, CreateManualTestCaseCommand command, String correlationId) {
        Requirement requirement = requireRequirementForSelection(actor, command.context());
        TestCase testCase = saveOne(
                actor,
                requirement,
                command.header().trim(),
                command.description().trim(),
                null,
                "MANUAL",
                null);
        audit(actor, requirement.projectId(), "TEST_CASE_CREATED_MANUAL", testCase.id(), correlationId);
        return summarize(actor, testCase);
    }

    @Transactional
    public TestCaseSummary createAdhocManual(
            AuthenticatedUser actor, CreateAdhocTestCaseCommand command, String correlationId) {
        AdhocSelectionContext context = command.context();
        requireAdhocScope(actor, context);
        GeneratedTestCase validated = validateManualCandidate(command.header(), command.description());
        TestCase testCase = saveAdhocOne(
                actor,
                context,
                validated.header().trim(),
                validated.description().trim(),
                null,
                "MANUAL_ADHOC",
                null);
        audit(actor, context.projectId(), "TEST_CASE_CREATED_MANUAL_ADHOC", testCase.id(), correlationId);
        return summarize(actor, testCase);
    }

    @Transactional
    public TestCaseGenerationResult generateFromRequirement(
            AuthenticatedUser actor,
            RequirementSelectionContext context,
            String idempotencyKey,
            String correlationId) {
        String key = requireIdempotencyKey(idempotencyKey);
        Requirement requirement = requireRequirementForSelection(actor, context);
        Optional<GenerationJob> existingJob = generationJobs.findByIdempotencyKey(key);
        if (existingJob.isPresent()) {
            return summarizeJob(actor, existingJob.get(), context.projectId());
        }
        var generated = aiProvider.generateTestCases(new TestCaseGenerationRequest(
                requirement.reqId(),
                requirement.header(),
                requirement.description(),
                requirement.acceptanceCriteria(),
                requirement.dependencies()));
        List<GeneratedTestCase> validated = validateGenerated(generated.testCases());
        GenerationJob job = generationJobs.save(GenerationJob.succeededTestCaseGeneration(
                actor.tenantId(), context.projectId(), "AI", generated.model(), key, actor.userId()));
        List<TestCaseSummary> saved = new ArrayList<>();
        for (GeneratedTestCase item : validated) {
            TestCase testCase = saveOne(
                    actor,
                    requirement,
                    item.header().trim(),
                    item.description().trim(),
                    null,
                    "AI",
                    job.id());
            audit(actor, context.projectId(), "TEST_CASE_CREATED_AI", testCase.id(), correlationId);
            saved.add(summarize(actor, testCase));
        }
        return new TestCaseGenerationResult(job.id(), saved.size(), saved);
    }

    @Transactional
    public TestCaseGenerationResult importCsv(
            AuthenticatedUser actor,
            RequirementSelectionContext context,
            MultipartFile csv,
            String idempotencyKey,
            String correlationId) {
        String key = requireIdempotencyKey(idempotencyKey);
        Requirement requirement = requireRequirementForSelection(actor, context);
        Optional<GenerationJob> existingJob = generationJobs.findByIdempotencyKey(key);
        if (existingJob.isPresent()) {
            return summarizeJob(actor, existingJob.get(), context.projectId());
        }
        List<GeneratedTestCase> rows = parseCsv(csv);
        GenerationJob job = generationJobs.save(GenerationJob.succeededTestCaseGeneration(
                actor.tenantId(), context.projectId(), "UPLOAD", null, key, actor.userId()));
        List<TestCaseSummary> saved = new ArrayList<>();
        for (GeneratedTestCase item : rows) {
            TestCase testCase = saveOne(
                    actor,
                    requirement,
                    item.header().trim(),
                    item.description().trim(),
                    null,
                    "CSV",
                    job.id());
            audit(actor, context.projectId(), "TEST_CASE_CREATED_CSV", testCase.id(), correlationId);
            saved.add(summarize(actor, testCase));
        }
        return new TestCaseGenerationResult(job.id(), saved.size(), saved);
    }

    @Transactional
    public TestCaseGenerationResult importAdhocCsv(
            AuthenticatedUser actor,
            AdhocSelectionContext context,
            MultipartFile csv,
            String idempotencyKey,
            String correlationId) {
        String key = requireIdempotencyKey(idempotencyKey);
        requireAdhocScope(actor, context);
        Optional<GenerationJob> existingJob = generationJobs.findByIdempotencyKey(key);
        if (existingJob.isPresent()) {
            return summarizeJob(actor, existingJob.get(), context.projectId());
        }
        List<GeneratedTestCase> rows = parseCsv(csv);
        GenerationJob job = generationJobs.save(GenerationJob.succeededTestCaseGeneration(
                actor.tenantId(), context.projectId(), "UPLOAD", null, key, actor.userId()));
        List<TestCaseSummary> saved = new ArrayList<>();
        for (GeneratedTestCase item : rows) {
            TestCase testCase = saveAdhocOne(
                    actor,
                    context,
                    item.header().trim(),
                    item.description().trim(),
                    null,
                    "CSV_ADHOC",
                    job.id());
            audit(actor, context.projectId(), "TEST_CASE_CREATED_CSV_ADHOC", testCase.id(), correlationId);
            saved.add(summarize(actor, testCase));
        }
        return new TestCaseGenerationResult(job.id(), saved.size(), saved);
    }

    @Transactional
    public TestCaseSummary update(
            AuthenticatedUser actor,
            UUID projectId,
            UUID testCaseId,
            UpdateTestCaseCommand command,
            String ifMatch,
            String correlationId) {
        TestCase testCase = requireTestCase(actor, projectId, testCaseId);
        requireVersion(testCase.version(), ifMatch);
        ProjectTestCycle cycle = requireCycle(actor, projectId, testCase.testCycleId());
        GeneratedTestCase validated = validateManualCandidate(command.header(), command.description());
        UUID assigneeMembershipId = normalizeAssignee(actor, projectId, command.assigneeMembershipId());
        validateDueDate(command.dueDate(), cycle);
        validateStatus(command.status());
        testCase.update(
                validated.header().trim(),
                validated.description().trim(),
                assigneeMembershipId,
                command.dueDate(),
                command.status());
        audit(actor, projectId, "TEST_CASE_UPDATED", testCase.id(), correlationId);
        return summarize(actor, testCase);
    }

    @Transactional
    public void delete(AuthenticatedUser actor, UUID projectId, UUID testCaseId, String ifMatch, String correlationId) {
        TestCase testCase = requireTestCase(actor, projectId, testCaseId);
        requireVersion(testCase.version(), ifMatch);
        try {
            testCase.softDelete();
        } catch (IllegalStateException exception) {
            throw new ApiConflictException(exception.getMessage());
        }
        audit(actor, projectId, "TEST_CASE_DELETED", testCase.id(), correlationId);
    }

    private TestCase saveOne(
            AuthenticatedUser actor,
            Requirement requirement,
            String header,
            String description,
            LocalDate dueDate,
            String sourceType,
            UUID generationJobId) {
        int sequence = counters.allocate(requirement.projectId(), "TC");
        return testCases.save(TestCase.createRequirementLinked(
                actor.tenantId(),
                requirement.projectId(),
                requirement.id(),
                sequence,
                requirement.projectSuiteAssignmentId(),
                requirement.testCycleId(),
                null,
                header,
                description,
                dueDate,
                sourceType,
                generationJobId));
    }

    private TestCase saveAdhocOne(
            AuthenticatedUser actor,
            AdhocSelectionContext context,
            String header,
            String description,
            LocalDate dueDate,
            String sourceType,
            UUID generationJobId) {
        int sequence = counters.allocate(context.projectId(), "TC");
        return testCases.save(TestCase.createAdhoc(
                actor.tenantId(),
                context.projectId(),
                sequence,
                context.projectSuiteAssignmentId(),
                context.testCycleId(),
                null,
                header,
                description,
                dueDate,
                sourceType,
                generationJobId));
    }

    private Requirement requireRequirementForSelection(AuthenticatedUser actor, RequirementSelectionContext context) {
        requireProject(actor, context.projectId());
        Requirement requirement = requirements
                .findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(
                        actor.tenantId(), context.projectId(), context.requirementId())
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
        requireSuiteAssignment(actor, context.projectId(), requirement.projectSuiteAssignmentId());
        requireCycle(actor, context.projectId(), requirement.testCycleId());
        if (context.projectSuiteAssignmentId() != null
                && !context.projectSuiteAssignmentId().equals(requirement.projectSuiteAssignmentId())) {
            throw new AccessDeniedException("The requested resource is not available.");
        }
        if (context.testCycleId() != null && !context.testCycleId().equals(requirement.testCycleId())) {
            throw new AccessDeniedException("The requested resource is not available.");
        }
        assignmentScope.requireAccess(
                actor, context.projectId(), requirement.projectSuiteAssignmentId(), requirement.testCycleId());
        return requirement;
    }

    private void requireAdhocScope(AuthenticatedUser actor, AdhocSelectionContext context) {
        requireProject(actor, context.projectId());
        requireSuiteAssignment(actor, context.projectId(), context.projectSuiteAssignmentId());
        requireCycle(actor, context.projectId(), context.testCycleId());
        assignmentScope.requireAccess(
                actor, context.projectId(), context.projectSuiteAssignmentId(), context.testCycleId());
    }

    private TestCase requireTestCase(AuthenticatedUser actor, UUID projectId, UUID testCaseId) {
        requireProject(actor, projectId);
        TestCase testCase = testCases
                .findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(actor.tenantId(), projectId, testCaseId)
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
        assignmentScope.requireAccess(
                actor, projectId, testCase.projectSuiteAssignmentId(), testCase.testCycleId());
        return testCase;
    }

    private Project requireProject(AuthenticatedUser user, UUID projectId) {
        requireUuid(projectId, "Project");
        return projects.findByTenantIdAndIdAndActiveTrueAndDeletedAtIsNull(user.tenantId(), projectId)
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
    }

    private ProjectSuiteAssignment requireSuiteAssignment(
            AuthenticatedUser user, UUID projectId, UUID assignmentId) {
        requireUuid(assignmentId, "Test Suite");
        return suiteAssignments
                .findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(user.tenantId(), projectId, assignmentId)
                .filter(ProjectSuiteAssignment::active)
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
    }

    private ProjectTestCycle requireCycle(AuthenticatedUser user, UUID projectId, UUID cycleId) {
        requireUuid(cycleId, "Test Cycle");
        return cycles.findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(user.tenantId(), projectId, cycleId)
                .filter(ProjectTestCycle::active)
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
    }

    private UUID normalizeAssignee(AuthenticatedUser actor, UUID projectId, UUID assigneeMembershipId) {
        if (assigneeMembershipId == null) {
            return null;
        }
        return memberships
                .findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(actor.tenantId(), projectId, assigneeMembershipId)
                .filter(ProjectMembership::active)
                .map(ProjectMembership::id)
                .orElseThrow(() -> new ApiConflictException("Assignee must be an active member of the project."));
    }

    private TestCaseGenerationResult summarizeJob(
            AuthenticatedUser actor, GenerationJob job, UUID expectedProjectId) {
        if (!actor.tenantId().equals(job.tenantId()) || !expectedProjectId.equals(job.projectId())) {
            throw new AccessDeniedException("The requested resource is not available.");
        }
        List<TestCaseSummary> summaries = testCases.findByGenerationJobIdAndDeletedAtIsNullOrderByCreatedDateDesc(job.id())
                .stream()
                .filter(testCase -> assignmentScope.canAccess(
                        actor, testCase.projectId(), testCase.projectSuiteAssignmentId(), testCase.testCycleId()))
                .map(testCase -> summarize(actor, testCase))
                .sorted(Comparator.comparing(TestCaseSummary::testCaseId))
                .toList();
        return new TestCaseGenerationResult(job.id(), summaries.size(), summaries);
    }

    private TestCaseSummary summarize(AuthenticatedUser user, TestCase testCase) {
        Project project = requireProject(user, testCase.projectId());
        Requirement requirement = testCase.requirementId() == null
                ? null
                : requirements
                        .findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(
                                user.tenantId(), testCase.projectId(), testCase.requirementId())
                        .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
        ProjectSuiteAssignment assignment =
                requireSuiteAssignment(user, testCase.projectId(), testCase.projectSuiteAssignmentId());
        TestSuite suite = suites.findAvailableSuite(user.tenantId(), assignment.suiteId())
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
        ProjectTestCycle cycle = requireCycle(user, testCase.projectId(), testCase.testCycleId());
        AssigneeSummary assignee = assignee(user, testCase.assigneeMembershipId());
        return new TestCaseSummary(
                testCase.id(),
                testCase.projectId(),
                project.name(),
                testCase.projectSuiteAssignmentId(),
                suite.id(),
                suite.name(),
                testCase.testCycleId(),
                cycle.name(),
                requirement == null ? null : requirement.id(),
                requirement == null ? null : requirement.reqId(),
                requirement == null ? null : requirement.header(),
                requirement == null ? null : requirement.description(),
                testCase.testCaseId(),
                testCase.header(),
                testCase.description(),
                testCase.status(),
                testCase.sourceType(),
                testCase.createdDate(),
                testCase.dueDate(),
                assignee == null ? null : assignee.membershipId(),
                assignee == null ? null : assignee.displayName(),
                testCase.version());
    }

    private AssigneeSummary assignee(AuthenticatedUser user, UUID assigneeMembershipId) {
        if (assigneeMembershipId == null) {
            return null;
        }
        ProjectMembership membership = memberships.findById(assigneeMembershipId)
                .filter(ProjectMembership::active)
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
        ApplicationUser assignee = users.findById(membership.userId())
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
        if (!user.tenantId().equals(membership.tenantId())) {
            throw new AccessDeniedException("The requested resource is not available.");
        }
        return new AssigneeSummary(membership.id(), assignee.firstName() + " " + assignee.lastName());
    }

    private static List<GeneratedTestCase> validateGenerated(List<GeneratedTestCase> generated) {
        if (generated == null || generated.isEmpty()) {
            throw invalid("AI returned no test cases.");
        }
        List<String> errors = new ArrayList<>();
        for (int index = 0; index < generated.size(); index++) {
            validateCandidate(generated.get(index), "Candidate " + (index + 1), errors);
        }
        if (!errors.isEmpty()) {
            throw new TestCaseOperationException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "AI returned malformed test cases.", errors);
        }
        return generated;
    }

    private static GeneratedTestCase validateManualCandidate(String header, String description) {
        List<String> errors = new ArrayList<>();
        GeneratedTestCase candidate = new GeneratedTestCase(header, description);
        validateCandidate(candidate, "Manual test case", errors);
        if (!errors.isEmpty()) {
            throw new TestCaseOperationException(
                    HttpStatus.BAD_REQUEST, "Manual test case is invalid.", errors);
        }
        return candidate;
    }

    private static List<GeneratedTestCase> parseCsv(MultipartFile csv) {
        if (csv == null || csv.isEmpty()) {
            throw invalid("CSV file is required.");
        }
        String filename = csv.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw invalid("Upload a CSV file.");
        }
        List<GeneratedTestCase> rows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(csv.getInputStream(), StandardCharsets.UTF_8))) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreSurroundingSpaces(true)
                    .get()
                    .parse(reader);
            for (CSVRecord record : records) {
                String header = record.isMapped("Test Case Header") ? record.get("Test Case Header") : null;
                String description = record.isMapped("Description") ? record.get("Description") : null;
                GeneratedTestCase candidate = new GeneratedTestCase(header, description);
                validateCandidate(candidate, "Row " + record.getRecordNumber(), errors);
                rows.add(candidate);
            }
        } catch (IllegalArgumentException exception) {
            throw new TestCaseOperationException(
                    HttpStatus.BAD_REQUEST,
                    "CSV must include Test Case Header and Description columns.",
                    List.of("Header row must contain Test Case Header and Description."));
        } catch (Exception exception) {
            throw new TestCaseOperationException(HttpStatus.BAD_REQUEST, "CSV could not be read.");
        }
        if (rows.isEmpty()) {
            errors.add("CSV must contain at least one data row.");
        }
        if (!errors.isEmpty()) {
            throw new TestCaseOperationException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "CSV contains invalid test case rows.", errors);
        }
        return rows;
    }

    private static void validateCandidate(GeneratedTestCase candidate, String label, List<String> errors) {
        if (candidate == null) {
            errors.add(label + ": row is empty.");
            return;
        }
        if (blank(candidate.header())) {
            errors.add(label + ": Test Case Header is required.");
        } else if (candidate.header().trim().length() > 300) {
            errors.add(label + ": Test Case Header must be 300 characters or fewer.");
        }
        if (blank(candidate.description())) {
            errors.add(label + ": Description is required.");
        } else if (candidate.description().trim().length() > 20_000) {
            errors.add(label + ": Description must be 20000 characters or fewer.");
        }
    }

    private static void validateStatus(String status) {
        if (!STATUSES.contains(status)) {
            throw new ApiConflictException("Status must be one of: " + String.join(", ", STATUSES) + ".");
        }
    }

    private static void validateDueDate(LocalDate dueDate, ProjectTestCycle cycle) {
        if (dueDate == null) {
            return;
        }
        if (cycle.startDate() != null && dueDate.isBefore(cycle.startDate())) {
            throw new ApiConflictException(
                    "Due Date must be on or after the selected Test Cycle start date (" + cycle.startDate() + ").");
        }
        if (cycle.endDate() != null && dueDate.isAfter(cycle.endDate())) {
            throw new ApiConflictException(
                    "Due Date must be on or before the selected Test Cycle end date (" + cycle.endDate() + ").");
        }
    }

    private static String requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new TestCaseOperationException(HttpStatus.BAD_REQUEST, "Idempotency-Key is required.");
        }
        String trimmed = idempotencyKey.trim();
        if (trimmed.length() > 160) {
            throw new TestCaseOperationException(
                    HttpStatus.BAD_REQUEST, "Idempotency-Key must be 160 characters or fewer.");
        }
        return trimmed;
    }

    private static UUID requireUuid(UUID value, String label) {
        if (value == null) {
            throw invalid(label + " is required.");
        }
        return value;
    }

    private static void requireVersion(int currentVersion, String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new ApiConflictException("If-Match is required for this operation.");
        }
        String normalized = ifMatch.trim().replace("\"", "");
        try {
            if (Integer.parseInt(normalized) == currentVersion) {
                return;
            }
        } catch (NumberFormatException exception) {
            throw new ApiConflictException("If-Match must contain a numeric version.");
        }
        throw new ApiConflictException("The resource has changed. Refresh and retry.");
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static TestCaseOperationException invalid(String message) {
        return new TestCaseOperationException(HttpStatus.BAD_REQUEST, message);
    }

    private void audit(
            AuthenticatedUser actor, UUID projectId, String action, UUID testCaseId, String correlationId) {
        auditEvents.save(AuditEvent.project(
                action,
                actor.userId().toString(),
                actor.tenantId(),
                projectId,
                "TEST_CASE",
                testCaseId.toString(),
                correlationId));
    }

    private record AssigneeSummary(UUID membershipId, String displayName) {}

    public record ListQuery(
            @NotNull UUID projectId,
            UUID projectSuiteAssignmentId,
            UUID testCycleId,
            UUID requirementId) {}

    public record RequirementSelectionContext(
            @NotNull UUID projectId,
            UUID projectSuiteAssignmentId,
            UUID testCycleId,
            @NotNull UUID requirementId) {}

    public record AdhocSelectionContext(
            @NotNull UUID projectId,
            @NotNull UUID projectSuiteAssignmentId,
            @NotNull UUID testCycleId) {}

    public record CreateManualTestCaseCommand(
            @NotNull UUID projectId,
            UUID projectSuiteAssignmentId,
            UUID testCycleId,
            @NotNull UUID requirementId,
            @NotBlank @Size(max = 300) String header,
            @NotBlank @Size(max = 20_000) String description) {
        RequirementSelectionContext context() {
            return new RequirementSelectionContext(projectId, projectSuiteAssignmentId, testCycleId, requirementId);
        }
    }

    public record CreateAdhocTestCaseCommand(
            @NotNull UUID projectId,
            @NotNull UUID projectSuiteAssignmentId,
            @NotNull UUID testCycleId,
            @NotBlank @Size(max = 300) String header,
            @NotBlank @Size(max = 20_000) String description) {
        AdhocSelectionContext context() {
            return new AdhocSelectionContext(projectId, projectSuiteAssignmentId, testCycleId);
        }
    }

    public record AdhocListQuery(
            @NotNull UUID projectId,
            @NotNull UUID projectSuiteAssignmentId,
            @NotNull UUID testCycleId) {
        AdhocSelectionContext context() {
            return new AdhocSelectionContext(projectId, projectSuiteAssignmentId, testCycleId);
        }
    }

    public record UpdateTestCaseCommand(
            UUID assigneeMembershipId,
            LocalDate dueDate,
            @NotBlank @Size(max = 300) String header,
            @NotBlank @Size(max = 20_000) String description,
            @NotBlank String status) {}

    public record TestCaseGenerationResult(UUID jobId, int importedCount, List<TestCaseSummary> testCases) {}

    public record TestCaseSummary(
            UUID id,
            UUID projectId,
            String projectName,
            UUID projectSuiteAssignmentId,
            UUID suiteId,
            String suiteName,
            UUID testCycleId,
            String cycleName,
            UUID requirementId,
            String reqId,
            String requirementHeader,
            String requirementDescription,
            String testCaseId,
            String header,
            String description,
            String status,
            String sourceType,
            Instant createdDate,
            LocalDate dueDate,
            UUID assigneeMembershipId,
            String assigneeName,
            int version) {}
}
