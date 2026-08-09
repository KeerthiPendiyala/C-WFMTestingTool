package com.ukgqtm.app.testcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ukgqtm.ai.api.RequirementGenerationProvider;
import com.ukgqtm.ai.api.RequirementGenerationProvider.GeneratedTestCase;
import com.ukgqtm.ai.api.RequirementGenerationProvider.TestCaseGenerationResult;
import com.ukgqtm.ai.domain.GenerationJob;
import com.ukgqtm.ai.repository.GenerationJobRepository;
import com.ukgqtm.app.api.ApiConflictException;
import com.ukgqtm.app.security.AssignmentScopeAuthorizationService;
import com.ukgqtm.app.testcase.TestCaseApplicationService.AdhocListQuery;
import com.ukgqtm.app.testcase.TestCaseApplicationService.AdhocSelectionContext;
import com.ukgqtm.app.testcase.TestCaseApplicationService.CreateAdhocTestCaseCommand;
import com.ukgqtm.app.testcase.TestCaseApplicationService.CreateManualTestCaseCommand;
import com.ukgqtm.app.testcase.TestCaseApplicationService.RequirementSelectionContext;
import com.ukgqtm.app.testcase.TestCaseApplicationService.UpdateTestCaseCommand;
import com.ukgqtm.audit.repository.AuditEventRepository;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.identity.repository.ApplicationUserRepository;
import com.ukgqtm.project.domain.Project;
import com.ukgqtm.project.domain.ProjectMembership;
import com.ukgqtm.project.domain.ProjectRole;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

class TestCaseApplicationServiceTest {
    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final ProjectSuiteAssignmentRepository assignments = mock(ProjectSuiteAssignmentRepository.class);
    private final TestSuiteRepository suites = mock(TestSuiteRepository.class);
    private final ProjectTestCycleRepository cycles = mock(ProjectTestCycleRepository.class);
    private final RequirementRepository requirements = mock(RequirementRepository.class);
    private final TestCaseRepository testCases = mock(TestCaseRepository.class);
    private final ProjectIdentifierCounterRepository counters = mock(ProjectIdentifierCounterRepository.class);
    private final ProjectMembershipRepository memberships = mock(ProjectMembershipRepository.class);
    private final ApplicationUserRepository users = mock(ApplicationUserRepository.class);
    private final GenerationJobRepository generationJobs = mock(GenerationJobRepository.class);
    private final RequirementGenerationProvider aiProvider = mock(RequirementGenerationProvider.class);
    private final AuditEventRepository auditEvents = mock(AuditEventRepository.class);
    private final AssignmentScopeAuthorizationService assignmentScope = mock(AssignmentScopeAuthorizationService.class);
    private final TestCaseApplicationService service = new TestCaseApplicationService(
            projects,
            assignments,
            suites,
            cycles,
            requirements,
            testCases,
            counters,
            memberships,
            users,
            generationJobs,
            aiProvider,
            auditEvents,
            assignmentScope);

    @Test
    void createsManualDraftWithReqIdLinkageAndAllocatedTcIdentifier() {
        Fixture fixture = fixture();
        stubSelection(fixture);
        when(counters.allocate(fixture.project().id(), "TC")).thenReturn(9);
        when(testCases.save(any(TestCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.createManual(
                fixture.actor(),
                new CreateManualTestCaseCommand(
                        fixture.project().id(),
                        null,
                        null,
                        fixture.requirement().id(),
                        " Validate clock-in ",
                        " Confirm capture. "),
                "corr-1");

        assertThat(created.reqId()).isEqualTo("REQ-004");
        assertThat(created.testCaseId()).isEqualTo("TC-009");
        assertThat(created.header()).isEqualTo("Validate clock-in");
        assertThat(created.description()).isEqualTo("Confirm capture.");
        assertThat(created.status()).isEqualTo("Draft");
        assertThat(created.sourceType()).isEqualTo("MANUAL");
        assertThat(created.projectName()).isEqualTo("WFM");
        assertThat(created.projectSuiteAssignmentId()).isEqualTo(fixture.assignment().id());
        assertThat(created.testCycleId()).isEqualTo(fixture.cycle().id());
        assertThat(created.requirementDescription()).isEqualTo("Capture time");
        verify(auditEvents).save(any());
    }

    @Test
    void createsAdhocManualDraftWithNullRequirementAndAllocatedProjectTcIdentifier() {
        Fixture fixture = fixture();
        stubAdhocSelection(fixture);
        when(counters.allocate(fixture.project().id(), "TC")).thenReturn(11);
        when(testCases.save(any(TestCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.createAdhocManual(
                fixture.actor(),
                new CreateAdhocTestCaseCommand(
                        fixture.project().id(),
                        fixture.assignment().id(),
                        fixture.cycle().id(),
                        " Verify payroll export ",
                        " Confirm the export file is generated. "),
                "corr-adhoc-1");

        assertThat(created.requirementId()).isNull();
        assertThat(created.reqId()).isNull();
        assertThat(created.requirementHeader()).isNull();
        assertThat(created.testCaseId()).isEqualTo("TC-011");
        assertThat(created.header()).isEqualTo("Verify payroll export");
        assertThat(created.sourceType()).isEqualTo("MANUAL_ADHOC");
        assertThat(created.status()).isEqualTo("Draft");
        verify(auditEvents).save(any());
    }

    @Test
    void rejectsRequirementWhenOptionalSuiteOrCycleFilterDoesNotMatchReqIdScope() {
        Fixture fixture = fixture();
        Requirement mismatched = Requirement.createManual(
                "tenant-1",
                fixture.project().id(),
                UUID.randomUUID(),
                fixture.cycle().id(),
                4,
                "Clock-in",
                "Capture time");
        stubProjectAssignmentAndCycle(fixture);
        when(requirements.findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(
                        "tenant-1", fixture.project().id(), mismatched.id()))
                .thenReturn(Optional.of(mismatched));

        assertThatThrownBy(() -> service.createManual(
                        fixture.actor(),
                        new CreateManualTestCaseCommand(
                                fixture.project().id(),
                                fixture.assignment().id(),
                                fixture.cycle().id(),
                                mismatched.id(),
                                "Header",
                                "Description"),
                        "corr-1"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsAssigneeThatIsNotAnActiveProjectMember() {
        Fixture fixture = fixture();
        TestCase testCase = draftTestCase(fixture);
        stubSelection(fixture);
        stubUpdate(fixture, testCase);
        UUID wrongMembershipId = UUID.randomUUID();
        when(memberships.findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(
                        "tenant-1", fixture.project().id(), wrongMembershipId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                        fixture.actor(),
                        fixture.project().id(),
                        testCase.id(),
                        new UpdateTestCaseCommand(wrongMembershipId, null, "Clock-in case", "Validate clock-in", "Draft"),
                        "\"0\"",
                        "corr-1"))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("active member");
    }

    @Test
    void updatesEditableFieldsExceptGeneratedTestCaseIdentifier() {
        Fixture fixture = fixture();
        TestCase testCase = draftTestCase(fixture);
        stubSelection(fixture);
        stubUpdate(fixture, testCase);

        var updated = service.update(
                fixture.actor(),
                fixture.project().id(),
                testCase.id(),
                new UpdateTestCaseCommand(
                        null,
                        LocalDate.parse("2026-08-15"),
                        " Updated header ",
                        " Updated description ",
                        "Retest"),
                "\"0\"",
                "corr-1");

        assertThat(updated.testCaseId()).isEqualTo(testCase.testCaseId());
        assertThat(updated.header()).isEqualTo("Updated header");
        assertThat(updated.description()).isEqualTo("Updated description");
        assertThat(updated.status()).isEqualTo("Retest");
        assertThat(updated.dueDate()).isEqualTo(LocalDate.parse("2026-08-15"));
    }


    @Test
    void reportsCsvRowErrorsAndDoesNotImportPartially() {
        Fixture fixture = fixture();
        stubSelection(fixture);
        MockMultipartFile csv = new MockMultipartFile(
                "csv",
                "test-cases.csv",
                "text/csv",
                ("Test Case Header,Description\r\n"
                                + "Valid case,Valid description\r\n"
                                + "Missing description,\r\n")
                        .getBytes());

        assertThatThrownBy(() -> service.importCsv(
                        fixture.actor(),
                        projectRequirementContext(fixture),
                        csv,
                        "csv-key-1",
                        "corr-1"))
                .isInstanceOf(TestCaseOperationException.class)
                .satisfies(exception -> assertThat(((TestCaseOperationException) exception).rowErrors())
                        .anyMatch(error -> error.contains("Description is required")));
        verify(generationJobs, never()).save(any());
        verifyNoInteractions(testCases);
    }

    @Test
    void importsCsvUsingOnlyProjectAndRequirementSelection() {
        Fixture fixture = fixture();
        stubSelection(fixture);
        when(generationJobs.findByIdempotencyKey("csv-key-2")).thenReturn(Optional.empty());
        when(generationJobs.save(any(GenerationJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(counters.allocate(fixture.project().id(), "TC")).thenReturn(10);
        when(testCases.save(any(TestCase.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile csv = new MockMultipartFile(
                "csv",
                "test-cases.csv",
                "text/csv",
                "Test Case Header,Description\r\nValid case,Valid description\r\n".getBytes());

        var result = service.importCsv(
                fixture.actor(), projectRequirementContext(fixture), csv, "csv-key-2", "corr-1");

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.testCases().get(0).testCaseId()).isEqualTo("TC-010");
        assertThat(result.testCases().get(0).sourceType()).isEqualTo("CSV");
        assertThat(result.testCases().get(0).header()).isEqualTo("Valid case");
    }

    @Test
    void importsAdhocCsvAtomicallyWithNullRequirementAndProjectScopedTcCounter() {
        Fixture fixture = fixture();
        stubAdhocSelection(fixture);
        when(generationJobs.findByIdempotencyKey("adhoc-csv-key-1")).thenReturn(Optional.empty());
        when(generationJobs.save(any(GenerationJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(counters.allocate(fixture.project().id(), "TC")).thenReturn(12);
        when(testCases.save(any(TestCase.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile csv = new MockMultipartFile(
                "csv",
                "test-cases.csv",
                "text/csv",
                "Test Case Header,Description\r\nAd hoc case,Validate no requirement link\r\n".getBytes());

        var result = service.importAdhocCsv(
                fixture.actor(),
                adhocContext(fixture),
                csv,
                "adhoc-csv-key-1",
                "corr-adhoc-csv");

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.testCases().get(0).requirementId()).isNull();
        assertThat(result.testCases().get(0).reqId()).isNull();
        assertThat(result.testCases().get(0).testCaseId()).isEqualTo("TC-012");
        assertThat(result.testCases().get(0).sourceType()).isEqualTo("CSV_ADHOC");
        ArgumentCaptor<TestCase> saved = ArgumentCaptor.forClass(TestCase.class);
        verify(testCases).save(saved.capture());
        assertThat(saved.getValue().requirementId()).isNull();
    }

    @Test
    void listAdhocShowsOnlyNullRequirementRowsWithinAuthorizedSuiteAndCycle() {
        Fixture fixture = fixture();
        TestCase linked = draftTestCase(fixture);
        TestCase adhoc = TestCase.createAdhoc(
                "tenant-1",
                fixture.project().id(),
                13,
                fixture.assignment().id(),
                fixture.cycle().id(),
                null,
                "Ad hoc",
                "No requirement",
                null,
                "MANUAL_ADHOC",
                null);
        stubAdhocSelection(fixture);
        when(testCases.findByTenantIdAndProjectIdAndDeletedAtIsNullOrderByCreatedDateDesc(
                        "tenant-1", fixture.project().id()))
                .thenReturn(List.of(linked, adhoc));
        when(assignmentScope.canAccess(
                        fixture.actor(), fixture.project().id(), fixture.assignment().id(), fixture.cycle().id()))
                .thenReturn(true);

        var result = service.listAdhoc(
                fixture.actor(),
                new AdhocListQuery(fixture.project().id(), fixture.assignment().id(), fixture.cycle().id()));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).requirementId()).isNull();
        assertThat(result.get(0).sourceType()).isEqualTo("MANUAL_ADHOC");
    }

    @Test
    void rejectsOtherProjectAdhocImportBeforeParsingOrSaving() {
        Fixture fixture = fixture();
        UUID otherProjectId = UUID.randomUUID();
        MockMultipartFile csv = new MockMultipartFile(
                "csv",
                "test-cases.csv",
                "text/csv",
                "Test Case Header,Description\r\nAd hoc case,Validate no requirement link\r\n".getBytes());
        when(projects.findByTenantIdAndIdAndActiveTrueAndDeletedAtIsNull("tenant-1", otherProjectId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.importAdhocCsv(
                        fixture.actor(),
                        new AdhocSelectionContext(otherProjectId, fixture.assignment().id(), fixture.cycle().id()),
                        csv,
                        "adhoc-csv-key-2",
                        "corr-adhoc-csv"))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(generationJobs);
        verifyNoInteractions(testCases);
    }

    @Test
    void rejectsMalformedAiOutputBeforeSavingGeneratedRows() {
        Fixture fixture = fixture();
        stubSelection(fixture);
        when(generationJobs.findByIdempotencyKey("ai-key-1")).thenReturn(Optional.empty());
        when(aiProvider.generateTestCases(any())).thenReturn(new TestCaseGenerationResult(
                "test-model", List.of(new GeneratedTestCase("", "Description"))));

        assertThatThrownBy(() -> service.generateFromRequirement(
                        fixture.actor(), projectRequirementContext(fixture), "ai-key-1", "corr-1"))
                .isInstanceOf(TestCaseOperationException.class)
                .satisfies(exception -> assertThat(((TestCaseOperationException) exception).rowErrors())
                        .anyMatch(error -> error.contains("Test Case Header is required")));
        verifyNoInteractions(testCases);
    }

    @Test
    void validatesExactStatusLabels() {
        Fixture fixture = fixture();
        TestCase testCase = draftTestCase(fixture);
        stubUpdate(fixture, testCase);

        assertThatThrownBy(() -> service.update(
                        fixture.actor(),
                        fixture.project().id(),
                        testCase.id(),
                        new UpdateTestCaseCommand(null, null, "Clock-in case", "Validate clock-in", "In Progress"),
                        "\"0\"",
                        "corr-1"))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("Inprogress")
                .hasMessageContaining("Not applicable");
    }

    @Test
    void deletesOnlyDraftTestCases() {
        Fixture fixture = fixture();
        TestCase testCase = draftTestCase(fixture);
        testCase.update(testCase.header(), testCase.description(), null, null, "Inprogress");
        stubProjectAndTestCase(fixture, testCase);

        assertThatThrownBy(() -> service.delete(
                        fixture.actor(), fixture.project().id(), testCase.id(), "\"0\"", "corr-1"))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("Draft");
    }

    private void stubSelection(Fixture fixture) {
        stubProjectAssignmentAndCycle(fixture);
        when(requirements.findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(
                        "tenant-1", fixture.project().id(), fixture.requirement().id()))
                .thenReturn(Optional.of(fixture.requirement()));
        when(suites.findAvailableSuite("tenant-1", fixture.suite().id())).thenReturn(Optional.of(fixture.suite()));
    }

    private void stubAdhocSelection(Fixture fixture) {
        stubProjectAssignmentAndCycle(fixture);
        when(suites.findAvailableSuite("tenant-1", fixture.suite().id())).thenReturn(Optional.of(fixture.suite()));
    }

    private void stubProjectAssignmentAndCycle(Fixture fixture) {
        when(projects.findByTenantIdAndIdAndActiveTrueAndDeletedAtIsNull("tenant-1", fixture.project().id()))
                .thenReturn(Optional.of(fixture.project()));
        when(assignments.findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(
                        "tenant-1", fixture.project().id(), fixture.assignment().id()))
                .thenReturn(Optional.of(fixture.assignment()));
        when(cycles.findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(
                        "tenant-1", fixture.project().id(), fixture.cycle().id()))
                .thenReturn(Optional.of(fixture.cycle()));
    }

    private void stubUpdate(Fixture fixture, TestCase testCase) {
        stubProjectAndTestCase(fixture, testCase);
        when(cycles.findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(
                        "tenant-1", fixture.project().id(), fixture.cycle().id()))
                .thenReturn(Optional.of(fixture.cycle()));
    }

    private void stubProjectAndTestCase(Fixture fixture, TestCase testCase) {
        when(projects.findByTenantIdAndIdAndActiveTrueAndDeletedAtIsNull("tenant-1", fixture.project().id()))
                .thenReturn(Optional.of(fixture.project()));
        when(testCases.findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(
                        "tenant-1", fixture.project().id(), testCase.id()))
                .thenReturn(Optional.of(testCase));
    }

    private static RequirementSelectionContext context(Fixture fixture) {
        return new RequirementSelectionContext(
                fixture.project().id(),
                fixture.assignment().id(),
                fixture.cycle().id(),
                fixture.requirement().id());
    }

    private static RequirementSelectionContext projectRequirementContext(Fixture fixture) {
        return new RequirementSelectionContext(
                fixture.project().id(),
                null,
                null,
                fixture.requirement().id());
    }

    private static AdhocSelectionContext adhocContext(Fixture fixture) {
        return new AdhocSelectionContext(
                fixture.project().id(), fixture.assignment().id(), fixture.cycle().id());
    }

    private static TestCase draftTestCase(Fixture fixture) {
        return TestCase.createRequirementLinked(
                "tenant-1",
                fixture.project().id(),
                fixture.requirement().id(),
                3,
                fixture.assignment().id(),
                fixture.cycle().id(),
                null,
                "Clock-in case",
                "Validate clock-in",
                null,
                "MANUAL",
                null);
    }

    private static Fixture fixture() {
        UUID actorId = UUID.randomUUID();
        AuthenticatedUser actor = new AuthenticatedUser(
                actorId, "tenant-1", "object-1", "Avery", "Admin", "avery@example.test", true);
        Project project = Project.create("tenant-1", "WFM", "WFM", null, actorId);
        TestSuite suite = TestSuite.create("tenant-1", "TIME", "Timekeeping", null, actorId);
        ProjectSuiteAssignment assignment =
                ProjectSuiteAssignment.create("tenant-1", project.id(), suite.id(), actorId);
        ProjectTestCycle cycle = ProjectTestCycle.create(
                "tenant-1",
                project.id(),
                "Cycle 1",
                LocalDate.parse("2026-08-01"),
                LocalDate.parse("2026-08-31"),
                null,
                actorId);
        Requirement requirement = Requirement.createManual(
                "tenant-1",
                project.id(),
                assignment.id(),
                cycle.id(),
                4,
                "Clock-in",
                "Capture time");
        ProjectMembership membership =
                ProjectMembership.create("tenant-1", project.id(), UUID.randomUUID(), ProjectRole.TEST_ANALYST, actorId);
        return new Fixture(actor, project, suite, assignment, cycle, requirement, membership);
    }

    private record Fixture(
            AuthenticatedUser actor,
            Project project,
            TestSuite suite,
            ProjectSuiteAssignment assignment,
            ProjectTestCycle cycle,
            Requirement requirement,
            ProjectMembership membership) {}
}
