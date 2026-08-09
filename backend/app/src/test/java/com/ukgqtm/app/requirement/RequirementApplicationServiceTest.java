package com.ukgqtm.app.requirement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ukgqtm.app.requirement.RequirementApplicationService.CreateManualRequirementCommand;
import com.ukgqtm.app.requirement.RequirementApplicationService.UpdateRequirementCommand;
import com.ukgqtm.app.security.AssignmentScopeAuthorizationService;
import com.ukgqtm.audit.repository.AuditEventRepository;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.project.domain.Project;
import com.ukgqtm.project.domain.ProjectSuiteAssignment;
import com.ukgqtm.project.domain.ProjectTestCycle;
import com.ukgqtm.project.domain.TestSuite;
import com.ukgqtm.project.repository.ProjectIdentifierCounterRepository;
import com.ukgqtm.project.repository.ProjectRepository;
import com.ukgqtm.project.repository.ProjectSuiteAssignmentRepository;
import com.ukgqtm.project.repository.ProjectTestCycleRepository;
import com.ukgqtm.project.repository.TestSuiteRepository;
import com.ukgqtm.requirements.domain.Requirement;
import com.ukgqtm.requirements.repository.RequirementRepository;
import com.ukgqtm.testmanagement.repository.TestCaseRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RequirementApplicationServiceTest {
    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final ProjectSuiteAssignmentRepository assignments = mock(ProjectSuiteAssignmentRepository.class);
    private final TestSuiteRepository suites = mock(TestSuiteRepository.class);
    private final ProjectTestCycleRepository cycles = mock(ProjectTestCycleRepository.class);
    private final ProjectIdentifierCounterRepository counters = mock(ProjectIdentifierCounterRepository.class);
    private final RequirementRepository requirements = mock(RequirementRepository.class);
    private final TestCaseRepository testCases = mock(TestCaseRepository.class);
    private final AuditEventRepository auditEvents = mock(AuditEventRepository.class);
    private final AssignmentScopeAuthorizationService assignmentScope = mock(AssignmentScopeAuthorizationService.class);
    private final RequirementApplicationService service = new RequirementApplicationService(
            projects, assignments, suites, cycles, counters, requirements, testCases, auditEvents, assignmentScope);

    @Test
    void createsProjectScopedManualDraftWithAllocatedIdentifier() {
        UUID actorId = UUID.randomUUID();
        AuthenticatedUser actor = new AuthenticatedUser(
                actorId,
                "tenant-1",
                "object-1",
                "Avery",
                "Administrator",
                "avery@example.test",
                true);
        Project project = Project.create("tenant-1", "ABC", "ABC", null, actorId);
        TestSuite suite = TestSuite.create("tenant-1", "TIMEKEEPING", "Timekeeping", null, actorId);
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

        when(projects.findByTenantIdAndIdAndActiveTrueAndDeletedAtIsNull("tenant-1", project.id()))
                .thenReturn(Optional.of(project));
        when(assignments.findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(
                        "tenant-1", project.id(), assignment.id()))
                .thenReturn(Optional.of(assignment));
        when(cycles.findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull("tenant-1", project.id(), cycle.id()))
                .thenReturn(Optional.of(cycle));
        when(suites.findAvailableSuite("tenant-1", suite.id())).thenReturn(Optional.of(suite));
        when(counters.allocate(project.id(), "REQ")).thenReturn(7);
        when(requirements.save(any(Requirement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.createManual(
                actor,
                new CreateManualRequirementCommand(
                        project.id(), assignment.id(), cycle.id(), " Validate clock-in ", " Confirm capture. "),
                "corr-1");

        assertThat(created.reqId()).isEqualTo("REQ-007");
        assertThat(created.header()).isEqualTo("Validate clock-in");
        assertThat(created.description()).isEqualTo("Confirm capture.");
        assertThat(created.status()).isEqualTo("Draft");
        assertThat(created.suiteName()).isEqualTo("Timekeeping");
        assertThat(created.cycleName()).isEqualTo("Cycle 1");
        verify(auditEvents).save(any());
    }

    @Test
    void updatesRequirementDetailsWithoutChangingReqId() {
        UUID actorId = UUID.randomUUID();
        AuthenticatedUser actor = new AuthenticatedUser(
                actorId,
                "tenant-1",
                "object-1",
                "Avery",
                "Administrator",
                "avery@example.test",
                true);
        Project project = Project.create("tenant-1", "ABC", "ABC", null, actorId);
        TestSuite suite = TestSuite.create("tenant-1", "TIMEKEEPING", "Timekeeping", null, actorId);
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
        Requirement requirement =
                Requirement.createManual("tenant-1", project.id(), assignment.id(), cycle.id(), 7, "Old", "Old desc");

        when(projects.findByTenantIdAndIdAndActiveTrueAndDeletedAtIsNull("tenant-1", project.id()))
                .thenReturn(Optional.of(project));
        when(requirements.findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(
                        "tenant-1", project.id(), requirement.id()))
                .thenReturn(Optional.of(requirement));
        when(assignments.findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(
                        "tenant-1", project.id(), assignment.id()))
                .thenReturn(Optional.of(assignment));
        when(cycles.findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull("tenant-1", project.id(), cycle.id()))
                .thenReturn(Optional.of(cycle));
        when(suites.findAvailableSuite("tenant-1", suite.id())).thenReturn(Optional.of(suite));

        var updated = service.update(
                actor,
                project.id(),
                requirement.id(),
                new UpdateRequirementCommand(
                        " Updated header ",
                        " Updated description ",
                        " Updated acceptance ",
                        " Updated assumptions ",
                        " Updated dependencies "),
                "\"0\"",
                "corr-2");

        assertThat(updated.reqId()).isEqualTo("REQ-007");
        assertThat(updated.header()).isEqualTo("Updated header");
        assertThat(updated.description()).isEqualTo("Updated description");
        assertThat(updated.acceptanceCriteria()).isEqualTo("Updated acceptance");
        assertThat(updated.assumptions()).isEqualTo("Updated assumptions");
        assertThat(updated.dependencies()).isEqualTo("Updated dependencies");
        verify(auditEvents).save(any());
    }
}
