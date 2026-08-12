package com.ukgqtm.app.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ukgqtm.app.api.ApiConflictException;
import com.ukgqtm.audit.repository.AuditEventRepository;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.identity.domain.ApplicationUser;
import com.ukgqtm.identity.repository.ApplicationUserRepository;
import com.ukgqtm.project.domain.Project;
import com.ukgqtm.project.domain.ProjectSuiteAssignment;
import com.ukgqtm.project.domain.ProjectTestCycle;
import com.ukgqtm.project.domain.TestSuite;
import com.ukgqtm.project.repository.ProjectRepository;
import com.ukgqtm.project.repository.ProjectSuiteAssignmentRepository;
import com.ukgqtm.project.repository.ProjectTestCycleRepository;
import com.ukgqtm.project.repository.TestSuiteRepository;
import com.ukgqtm.requirements.repository.RequirementRepository;
import com.ukgqtm.testmanagement.repository.TestCaseRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class ProjectStructureApplicationServiceTest {
    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final TestSuiteRepository suites = mock(TestSuiteRepository.class);
    private final ProjectSuiteAssignmentRepository suiteAssignments = mock(ProjectSuiteAssignmentRepository.class);
    private final ProjectTestCycleRepository cycles = mock(ProjectTestCycleRepository.class);
    private final RequirementRepository requirements = mock(RequirementRepository.class);
    private final TestCaseRepository testCases = mock(TestCaseRepository.class);
    private final AuditEventRepository auditEvents = mock(AuditEventRepository.class);
    private final ApplicationUserRepository users = mock(ApplicationUserRepository.class);
    private final ProjectStructureApplicationService service = new ProjectStructureApplicationService(
            projects,
            suites,
            suiteAssignments,
            cycles,
            requirements,
            testCases,
            auditEvents,
            users);

    private final AuthenticatedUser manager = user(false);
    private final UUID projectId = UUID.randomUUID();
    private Project project;

    @BeforeEach
    void setUp() {
        project = Project.create(manager.tenantId(), "ABC", "Australian Broadcasting Corporation", "Timekeeping", manager.userId());
        when(projects.findByTenantIdAndIdAndActiveTrueAndDeletedAtIsNull(manager.tenantId(), projectId))
                .thenReturn(Optional.of(project));
        when(suites.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(suiteAssignments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(cycles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createOrAssignSuiteCreatesTenantSuiteWhenNotAvailableAndAuditsAssignment() {
        when(suites.findTenantSuiteByKey(manager.tenantId(), "TIMEKEEPING")).thenReturn(Optional.empty());

        var assignment = service.createOrAssignSuite(
                manager,
                projectId,
                new ProjectStructureApplicationService.AssignSuiteCommand(null, "Timekeeping", "Core time capture"),
                "corr-1");

        assertThat(assignment.suiteKey()).isEqualTo("TIMEKEEPING");
        assertThat(assignment.projectId()).isEqualTo(projectId);
        verify(suites).save(any(TestSuite.class));
        verify(suiteAssignments).save(any(ProjectSuiteAssignment.class));
        verify(auditEvents).save(any());
    }

    @Test
    void createOrAssignSuiteRejectsDuplicateProjectAssignment() {
        TestSuite suite = TestSuite.create(manager.tenantId(), "INTEGRATION", "Integration", "Integrations", manager.userId());
        when(suites.findTenantSuiteByKey(manager.tenantId(), "INTEGRATION")).thenReturn(Optional.of(suite));
        when(suiteAssignments.findByTenantIdAndProjectIdAndSuiteIdAndDeletedAtIsNull(
                        manager.tenantId(), projectId, suite.id()))
                .thenReturn(Optional.of(ProjectSuiteAssignment.create(manager.tenantId(), projectId, suite.id(), manager.userId())));

        assertThatThrownBy(() -> service.createOrAssignSuite(
                        manager,
                        projectId,
                        new ProjectStructureApplicationService.AssignSuiteCommand(null, "Integration", null),
                        "corr-1"))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("already assigned");
    }

    @Test
    void updateSuiteRequiresMatchingIfMatchVersionAndAudits() {
        TestSuite suite = TestSuite.create(manager.tenantId(), "PERSONAS", "Personas", "Old", manager.userId());
        when(suiteAssignments.findByTenantIdAndProjectIdAndSuiteIdAndDeletedAtIsNull(
                        manager.tenantId(), projectId, suite.id()))
                .thenReturn(Optional.of(ProjectSuiteAssignment.create(
                        manager.tenantId(), projectId, suite.id(), manager.userId())));
        when(suites.findAvailableSuite(manager.tenantId(), suite.id())).thenReturn(Optional.of(suite));

        var updated = service.updateSuite(
                manager,
                projectId,
                suite.id(),
                new ProjectStructureApplicationService.UpdateSuiteCommand("Personas", "Updated"),
                "0",
                "corr-1");

        assertThat(updated.description()).isEqualTo("Updated");
        verify(auditEvents).save(any());
    }

    @Test
    void updateSuiteRejectsStaleVersion() {
        TestSuite suite = TestSuite.create(manager.tenantId(), "PERSONAS", "Personas", "Old", manager.userId());
        when(suiteAssignments.findByTenantIdAndProjectIdAndSuiteIdAndDeletedAtIsNull(
                        manager.tenantId(), projectId, suite.id()))
                .thenReturn(Optional.of(ProjectSuiteAssignment.create(
                        manager.tenantId(), projectId, suite.id(), manager.userId())));
        when(suites.findAvailableSuite(manager.tenantId(), suite.id())).thenReturn(Optional.of(suite));

        assertThatThrownBy(() -> service.updateSuite(
                        manager,
                        projectId,
                        suite.id(),
                        new ProjectStructureApplicationService.UpdateSuiteCommand("Personas", "Updated"),
                        "7",
                        "corr-1"))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("changed");
    }

    @Test
    void updateSuiteRejectsSuiteNotAssignedToSelectedProject() {
        TestSuite suite = TestSuite.create(manager.tenantId(), "PERSONAS", "Personas", "Old", manager.userId());
        when(suiteAssignments.findByTenantIdAndProjectIdAndSuiteIdAndDeletedAtIsNull(
                        manager.tenantId(), projectId, suite.id()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateSuite(
                        manager,
                        projectId,
                        suite.id(),
                        new ProjectStructureApplicationService.UpdateSuiteCommand("Personas", "Updated"),
                        "0",
                        "corr-1"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not available");
        verify(suites, never()).findAvailableSuite(manager.tenantId(), suite.id());
    }

    @Test
    void unassignSuiteIsBlockedWhenRequirementsOrTestCasesReferenceAssignment() {
        UUID assignmentId = UUID.randomUUID();
        ProjectSuiteAssignment assignment =
                ProjectSuiteAssignment.create(manager.tenantId(), projectId, UUID.randomUUID(), manager.userId());
        when(suiteAssignments.findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(
                        manager.tenantId(), projectId, assignmentId))
                .thenReturn(Optional.of(assignment));
        when(requirements.countByProjectIdAndProjectSuiteAssignmentIdAndDeletedAtIsNull(projectId, assignmentId))
                .thenReturn(1L);

        assertThatThrownBy(() -> service.unassignSuite(manager, projectId, assignmentId, "0", "corr-1"))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("referenced");
    }

    @Test
    void deleteSuiteUnassignsSelectedProjectAndDeletesCatalogWhenUnreferenced() {
        TestSuite suite = TestSuite.create(manager.tenantId(), "TIMEKEEPING", "Timekeeping", "Core", manager.userId());
        ProjectSuiteAssignment assignment =
                ProjectSuiteAssignment.create(manager.tenantId(), projectId, suite.id(), manager.userId());
        when(suiteAssignments.findByTenantIdAndProjectIdAndSuiteIdAndDeletedAtIsNull(
                        manager.tenantId(), projectId, suite.id()))
                .thenReturn(Optional.of(assignment));
        when(suites.findAvailableSuite(manager.tenantId(), suite.id())).thenReturn(Optional.of(suite));
        when(suiteAssignments.countByTenantIdAndSuiteIdAndDeletedAtIsNull(manager.tenantId(), suite.id()))
                .thenReturn(1L);

        service.deleteSuite(manager, projectId, suite.id(), "0", "corr-1");

        assertThat(assignment.active()).isFalse();
        assertThat(suite.active()).isFalse();
        verify(auditEvents, times(2)).save(any());
    }

    @Test
    void deleteSuiteIsBlockedWhenReferencedBySelectedProjectContent() {
        TestSuite suite = TestSuite.create(manager.tenantId(), "TIMEKEEPING", "Timekeeping", "Core", manager.userId());
        ProjectSuiteAssignment assignment =
                ProjectSuiteAssignment.create(manager.tenantId(), projectId, suite.id(), manager.userId());
        when(suiteAssignments.findByTenantIdAndProjectIdAndSuiteIdAndDeletedAtIsNull(
                        manager.tenantId(), projectId, suite.id()))
                .thenReturn(Optional.of(assignment));
        when(suites.findAvailableSuite(manager.tenantId(), suite.id())).thenReturn(Optional.of(suite));
        when(requirements.countByProjectIdAndProjectSuiteAssignmentIdAndDeletedAtIsNull(projectId, assignment.id()))
                .thenReturn(1L);

        assertThatThrownBy(() -> service.deleteSuite(manager, projectId, suite.id(), "0", "corr-1"))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("referenced");
        verify(auditEvents, never()).save(any());
    }

    @Test
    void deleteSuiteRemovesSelectedProjectAssignmentButKeepsReusableSuiteAssignedElsewhere() {
        TestSuite suite = TestSuite.create(manager.tenantId(), "TIMEKEEPING", "Timekeeping", "Core", manager.userId());
        ProjectSuiteAssignment assignment =
                ProjectSuiteAssignment.create(manager.tenantId(), projectId, suite.id(), manager.userId());
        when(suiteAssignments.findByTenantIdAndProjectIdAndSuiteIdAndDeletedAtIsNull(
                        manager.tenantId(), projectId, suite.id()))
                .thenReturn(Optional.of(assignment));
        when(suites.findAvailableSuite(manager.tenantId(), suite.id())).thenReturn(Optional.of(suite));
        when(suiteAssignments.countByTenantIdAndSuiteIdAndDeletedAtIsNull(manager.tenantId(), suite.id()))
                .thenReturn(2L);

        service.deleteSuite(manager, projectId, suite.id(), "0", "corr-1");

        assertThat(assignment.active()).isFalse();
        assertThat(suite.active()).isTrue();
        verify(auditEvents).save(any());
    }

    @Test
    void createCycleValidatesDateOrder() {
        assertThatThrownBy(() -> service.createCycle(
                        manager,
                        projectId,
                        new ProjectStructureApplicationService.SaveCycleCommand(
                                "Cycle 1", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 8, 1), "Bad dates"),
                        "corr-1"))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("start date");
    }

    @Test
    void createCycleRejectsDuplicateProjectName() {
        when(cycles.existsActiveName(manager.tenantId(), projectId, "Cycle 1")).thenReturn(true);

        assertThatThrownBy(() -> service.createCycle(
                        manager,
                        projectId,
                        new ProjectStructureApplicationService.SaveCycleCommand(
                                "Cycle 1", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), "Regression"),
                        "corr-1"))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createCyclePersistsAndAudits() {
        var cycle = service.createCycle(
                manager,
                projectId,
                new ProjectStructureApplicationService.SaveCycleCommand(
                        "Cycle 1", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), "Regression"),
                "corr-1");

        assertThat(cycle.name()).isEqualTo("Cycle 1");
        assertThat(cycle.startDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        verify(cycles).save(any(ProjectTestCycle.class));
        verify(auditEvents).save(any());
    }

    @Test
    void managerSeesNewProjectCycleOnSubsequentReads() {
        ProjectTestCycle monthly = ProjectTestCycle.create(
                manager.tenantId(),
                projectId,
                "Monthly",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                null,
                UUID.randomUUID());
        List<ProjectTestCycle> persistedCycles = new ArrayList<>(List.of(monthly));
        when(cycles.save(any(ProjectTestCycle.class))).thenAnswer(invocation -> {
            ProjectTestCycle cycle = invocation.getArgument(0);
            persistedCycles.add(cycle);
            return cycle;
        });
        when(cycles.findByTenantIdAndProjectIdAndDeletedAtIsNullOrderByStartDateAscNameAsc(
                        manager.tenantId(), projectId))
                .thenAnswer(invocation -> List.copyOf(persistedCycles));

        service.createCycle(
                manager,
                projectId,
                new ProjectStructureApplicationService.SaveCycleCommand(
                        "Regression", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), "New cycle"),
                "corr-regression");

        assertThat(service.listProjectCycles(manager, projectId))
                .extracting(ProjectStructureApplicationService.ProjectCycleSummary::name)
                .containsExactly("Monthly", "Regression");
        assertThat(service.listProjectCycles(manager, projectId))
                .extracting(ProjectStructureApplicationService.ProjectCycleSummary::name)
                .containsExactly("Monthly", "Regression");
    }

    @Test
    void deleteCycleIsBlockedWhenReferenced() {
        UUID cycleId = UUID.randomUUID();
        ProjectTestCycle cycle = ProjectTestCycle.create(
                manager.tenantId(),
                projectId,
                "Cycle 1",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                "Regression",
                manager.userId());
        when(cycles.findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(manager.tenantId(), projectId, cycleId))
                .thenReturn(Optional.of(cycle));
        when(testCases.countByProjectIdAndTestCycleIdAndDeletedAtIsNull(projectId, cycleId)).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteCycle(manager, projectId, cycleId, "0", "corr-1"))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("referenced");
    }

    @Test
    void listSelectorsStayProjectScoped() {
        TestSuite suite = TestSuite.create(manager.tenantId(), "TIMEKEEPING", "Timekeeping", "Core", manager.userId());
        ProjectSuiteAssignment assignment =
                ProjectSuiteAssignment.create(manager.tenantId(), projectId, suite.id(), manager.userId());
        when(suiteAssignments.findByTenantIdAndProjectIdAndDeletedAtIsNullOrderByIdAsc(manager.tenantId(), projectId))
                .thenReturn(List.of(assignment));
        when(suites.findAvailableSuites(manager.tenantId())).thenReturn(List.of(suite));

        assertThat(service.listProjectSuiteAssignments(manager, projectId))
                .singleElement()
                .extracting(ProjectStructureApplicationService.ProjectSuiteAssignmentSummary::projectId)
                .isEqualTo(projectId);
    }

    @Test
    void assignmentScopedManagerSeesNewSuiteAfterCreatingItInAuthorizedProject() {
        ApplicationUser applicationUser =
                ApplicationUser.localUser("Avery", "Tester", manager.contactEmail(), true, true);
        when(users.findById(manager.userId())).thenReturn(Optional.of(applicationUser));

        TestSuite integration =
                TestSuite.create(manager.tenantId(), "INTEGRATION", "Integration", "Existing suite", manager.userId());
        List<TestSuite> persistedSuites = new ArrayList<>(List.of(integration));
        List<ProjectSuiteAssignment> persistedAssignments = new ArrayList<>(List.of(
                ProjectSuiteAssignment.create(manager.tenantId(), projectId, integration.id(), UUID.randomUUID())));

        when(suites.findTenantSuiteByKey(manager.tenantId(), "PERSONAS")).thenReturn(Optional.empty());
        when(suites.save(any(TestSuite.class))).thenAnswer(invocation -> {
            TestSuite suite = invocation.getArgument(0);
            persistedSuites.add(suite);
            return suite;
        });
        when(suiteAssignments.save(any(ProjectSuiteAssignment.class))).thenAnswer(invocation -> {
            ProjectSuiteAssignment assignment = invocation.getArgument(0);
            persistedAssignments.add(assignment);
            return assignment;
        });
        when(suites.findAvailableSuites(manager.tenantId())).thenAnswer(invocation -> List.copyOf(persistedSuites));
        when(suiteAssignments.findByTenantIdAndProjectIdAndDeletedAtIsNullOrderByIdAsc(
                        manager.tenantId(), projectId))
                .thenAnswer(invocation -> List.copyOf(persistedAssignments));

        service.createOrAssignSuite(
                manager,
                projectId,
                new ProjectStructureApplicationService.AssignSuiteCommand(null, "Personas", "New suite"),
                "corr-personas");

        assertThat(service.listProjectSuiteAssignments(manager, projectId))
                .extracting(ProjectStructureApplicationService.ProjectSuiteAssignmentSummary::name)
                .containsExactly("Integration", "Personas");

        // A subsequent authenticated request reads the same persisted project assignments.
        assertThat(service.listProjectSuiteAssignments(manager, projectId))
                .extracting(ProjectStructureApplicationService.ProjectSuiteAssignmentSummary::name)
                .containsExactly("Integration", "Personas");
    }

    @Test
    void assignmentScopedSuiteCatalogExcludesSuitesBelongingOnlyToUnauthorizedProjects() {
        ApplicationUser applicationUser =
                ApplicationUser.localUser("Avery", "Tester", manager.contactEmail(), true, true);
        when(users.findById(manager.userId())).thenReturn(Optional.of(applicationUser));
        when(projects.findAssignedActiveProjects(manager.tenantId(), manager.userId())).thenReturn(List.of(project));

        TestSuite woolesSuite =
                TestSuite.create(manager.tenantId(), "PERSONAS", "Personas", null, manager.userId());
        TestSuite unauthorizedSuite =
                TestSuite.create(manager.tenantId(), "PRIVATE", "Private project suite", null, UUID.randomUUID());
        ProjectSuiteAssignment woolesAssignment = ProjectSuiteAssignment.create(
                manager.tenantId(), project.id(), woolesSuite.id(), manager.userId());
        when(suiteAssignments.findByTenantIdAndProjectIdAndDeletedAtIsNullOrderByIdAsc(
                        manager.tenantId(), project.id()))
                .thenReturn(List.of(woolesAssignment));
        when(suites.findAvailableSuites(manager.tenantId())).thenReturn(List.of(woolesSuite, unauthorizedSuite));

        assertThat(service.listSuiteCatalog(manager, projectId))
                .extracting(ProjectStructureApplicationService.SuiteCatalogSummary::name)
                .containsExactly("Personas");
    }

    private static AuthenticatedUser user(boolean administrator) {
        return new AuthenticatedUser(
                UUID.randomUUID(),
                "tenant-1",
                UUID.randomUUID().toString(),
                "Avery",
                "Tester",
                "avery@example.test",
                administrator);
    }
}
