package com.ukgqtm.app.project;

import com.ukgqtm.app.api.ApiConflictException;
import com.ukgqtm.audit.domain.AuditEvent;
import com.ukgqtm.audit.repository.AuditEventRepository;
import com.ukgqtm.identity.api.AuthenticatedUser;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectStructureApplicationService {
    private final ProjectRepository projects;
    private final TestSuiteRepository suites;
    private final ProjectSuiteAssignmentRepository suiteAssignments;
    private final ProjectTestCycleRepository cycles;
    private final RequirementRepository requirements;
    private final TestCaseRepository testCases;
    private final AuditEventRepository auditEvents;
    private final ApplicationUserRepository users;

    public ProjectStructureApplicationService(
            ProjectRepository projects,
            TestSuiteRepository suites,
            ProjectSuiteAssignmentRepository suiteAssignments,
            ProjectTestCycleRepository cycles,
            RequirementRepository requirements,
            TestCaseRepository testCases,
            AuditEventRepository auditEvents,
            ApplicationUserRepository users) {
        this.projects = projects;
        this.suites = suites;
        this.suiteAssignments = suiteAssignments;
        this.cycles = cycles;
        this.requirements = requirements;
        this.testCases = testCases;
        this.auditEvents = auditEvents;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<SuiteCatalogSummary> listSuiteCatalog(AuthenticatedUser user, UUID projectId) {
        requireProject(user.tenantId(), projectId);
        Set<UUID> visibleSuiteIds = restricted(user)
                ? projects.findAssignedActiveProjects(user.tenantId(), user.userId()).stream()
                        .flatMap(project -> suiteAssignments
                                .findByTenantIdAndProjectIdAndDeletedAtIsNullOrderByIdAsc(
                                        user.tenantId(), project.id())
                                .stream())
                        .filter(ProjectSuiteAssignment::active)
                        .map(ProjectSuiteAssignment::suiteId)
                        .collect(Collectors.toSet())
                : null;
        return suites.findAvailableSuites(user.tenantId()).stream()
                .filter(suite -> visibleSuiteIds == null || visibleSuiteIds.contains(suite.id()))
                .map(this::toCatalogSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectSuiteAssignmentSummary> listProjectSuiteAssignments(AuthenticatedUser user, UUID projectId) {
        requireProject(user.tenantId(), projectId);
        List<ProjectSuiteAssignment> assignments =
                suiteAssignments.findByTenantIdAndProjectIdAndDeletedAtIsNullOrderByIdAsc(user.tenantId(), projectId);
        Map<UUID, TestSuite> suitesById = suites.findAvailableSuites(user.tenantId()).stream()
                .collect(Collectors.toMap(TestSuite::id, Function.identity()));
        return assignments.stream()
                .filter(ProjectSuiteAssignment::active)
                .map(assignment -> toAssignmentSummary(assignment, suitesById.get(assignment.suiteId())))
                .sorted(Comparator.comparing(ProjectSuiteAssignmentSummary::name))
                .toList();
    }

    @Transactional
    public ProjectSuiteAssignmentSummary createOrAssignSuite(
            AuthenticatedUser actor, UUID projectId, AssignSuiteCommand command, String correlationId) {
        requireProject(actor.tenantId(), projectId);
        String suiteKey = normalizeKey(command.name());
        TestSuite suite = command.suiteId() == null
                ? suites.findTenantSuiteByKey(actor.tenantId(), suiteKey)
                        .orElseGet(() -> suites.save(TestSuite.create(
                                actor.tenantId(),
                                suiteKey,
                                command.name().trim(),
                                normalizeNullable(command.description()),
                                actor.userId())))
                : suites.findAvailableSuite(actor.tenantId(), command.suiteId())
                        .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));

        suiteAssignments.findByTenantIdAndProjectIdAndSuiteIdAndDeletedAtIsNull(actor.tenantId(), projectId, suite.id())
                .ifPresent(existing -> {
                    throw new ApiConflictException("Suite is already assigned to this project.");
                });

        ProjectSuiteAssignment assignment =
                suiteAssignments.save(ProjectSuiteAssignment.create(actor.tenantId(), projectId, suite.id(), actor.userId()));
        auditEvents.save(AuditEvent.project(
                "PROJECT_SUITE_ASSIGNED",
                actor.userId().toString(),
                actor.tenantId(),
                projectId,
                "PROJECT_SUITE_ASSIGNMENT",
                assignment.id().toString(),
                correlationId));
        return toAssignmentSummary(assignment, suite);
    }

    @Transactional
    public SuiteCatalogSummary updateSuite(
            AuthenticatedUser actor,
            UUID projectId,
            UUID suiteId,
            UpdateSuiteCommand command,
            String ifMatch,
            String correlationId) {
        requireProject(actor.tenantId(), projectId);
        requireSuiteAssignedToProject(actor.tenantId(), projectId, suiteId);
        TestSuite suite = suites.findAvailableSuite(actor.tenantId(), suiteId)
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
        requireVersion(suite.version(), ifMatch);
        String suiteKey = normalizeKey(command.name());
        if (suites.existsTenantSuiteByKeyExcluding(actor.tenantId(), suiteKey, suite.id())) {
            throw new ApiConflictException("Suite name already exists.");
        }
        suite.update(suiteKey, command.name().trim(), normalizeNullable(command.description()), actor.userId());
        auditEvents.save(AuditEvent.project(
                "TEST_SUITE_UPDATED",
                actor.userId().toString(),
                actor.tenantId(),
                null,
                "TEST_SUITE",
                suite.id().toString(),
                correlationId));
        return toCatalogSummary(suite);
    }

    @Transactional
    public void deleteSuite(AuthenticatedUser actor, UUID projectId, UUID suiteId, String ifMatch, String correlationId) {
        requireProject(actor.tenantId(), projectId);
        ProjectSuiteAssignment assignment = requireSuiteAssignedToProject(actor.tenantId(), projectId, suiteId);
        TestSuite suite = suites.findAvailableSuite(actor.tenantId(), suiteId)
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
        requireVersion(suite.version(), ifMatch);
        long requirementReferences =
                requirements.countByProjectIdAndProjectSuiteAssignmentIdAndDeletedAtIsNull(projectId, assignment.id());
        long testCaseReferences =
                testCases.countByProjectIdAndProjectSuiteAssignmentIdAndDeletedAtIsNull(projectId, assignment.id());
        if (requirementReferences + testCaseReferences > 0) {
            throw new ApiConflictException("Suite assignment is referenced by requirements or test cases.");
        }
        boolean finalProjectAssignment =
                suiteAssignments.countByTenantIdAndSuiteIdAndDeletedAtIsNull(actor.tenantId(), suiteId) == 1;
        assignment.unassign(actor.userId());
        auditEvents.save(AuditEvent.project(
                "PROJECT_SUITE_UNASSIGNED",
                actor.userId().toString(),
                actor.tenantId(),
                projectId,
                "PROJECT_SUITE_ASSIGNMENT",
                assignment.id().toString(),
                correlationId));
        if (finalProjectAssignment) {
            suite.softDelete(actor.userId());
            auditEvents.save(AuditEvent.project(
                    "TEST_SUITE_DELETED",
                    actor.userId().toString(),
                    actor.tenantId(),
                    null,
                    "TEST_SUITE",
                    suite.id().toString(),
                    correlationId));
        }
    }

    @Transactional
    public void unassignSuite(
            AuthenticatedUser actor, UUID projectId, UUID assignmentId, String ifMatch, String correlationId) {
        requireProject(actor.tenantId(), projectId);
        ProjectSuiteAssignment assignment = suiteAssignments
                .findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(actor.tenantId(), projectId, assignmentId)
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
        requireVersion(assignment.version(), ifMatch);
        long requirementReferences =
                requirements.countByProjectIdAndProjectSuiteAssignmentIdAndDeletedAtIsNull(projectId, assignmentId);
        long testCaseReferences =
                testCases.countByProjectIdAndProjectSuiteAssignmentIdAndDeletedAtIsNull(projectId, assignmentId);
        if (requirementReferences + testCaseReferences > 0) {
            throw new ApiConflictException("Suite assignment is referenced by requirements or test cases.");
        }
        assignment.unassign(actor.userId());
        auditEvents.save(AuditEvent.project(
                "PROJECT_SUITE_UNASSIGNED",
                actor.userId().toString(),
                actor.tenantId(),
                projectId,
                "PROJECT_SUITE_ASSIGNMENT",
                assignment.id().toString(),
                correlationId));
    }

    @Transactional(readOnly = true)
    public List<ProjectCycleSummary> listProjectCycles(AuthenticatedUser user, UUID projectId) {
        requireProject(user.tenantId(), projectId);
        return cycles.findByTenantIdAndProjectIdAndDeletedAtIsNullOrderByStartDateAscNameAsc(user.tenantId(), projectId)
                .stream()
                .filter(ProjectTestCycle::active)
                .map(this::toCycleSummary)
                .toList();
    }

    @Transactional
    public ProjectCycleSummary createCycle(
            AuthenticatedUser actor, UUID projectId, SaveCycleCommand command, String correlationId) {
        requireProject(actor.tenantId(), projectId);
        validateDates(command.startDate(), command.endDate());
        if (cycles.existsActiveName(actor.tenantId(), projectId, command.name().trim())) {
            throw new ApiConflictException("Cycle name already exists for this project.");
        }
        ProjectTestCycle cycle = cycles.save(ProjectTestCycle.create(
                actor.tenantId(),
                projectId,
                command.name().trim(),
                command.startDate(),
                command.endDate(),
                normalizeNullable(command.description()),
                actor.userId()));
        auditEvents.save(AuditEvent.project(
                "PROJECT_CYCLE_CREATED",
                actor.userId().toString(),
                actor.tenantId(),
                projectId,
                "PROJECT_TEST_CYCLE",
                cycle.id().toString(),
                correlationId));
        return toCycleSummary(cycle);
    }

    @Transactional
    public ProjectCycleSummary updateCycle(
            AuthenticatedUser actor,
            UUID projectId,
            UUID cycleId,
            SaveCycleCommand command,
            String ifMatch,
            String correlationId) {
        requireProject(actor.tenantId(), projectId);
        validateDates(command.startDate(), command.endDate());
        ProjectTestCycle cycle = cycles.findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(actor.tenantId(), projectId, cycleId)
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
        requireVersion(cycle.version(), ifMatch);
        if (cycles.existsActiveNameExcluding(actor.tenantId(), projectId, command.name().trim(), cycleId)) {
            throw new ApiConflictException("Cycle name already exists for this project.");
        }
        cycle.update(command.name().trim(), command.startDate(), command.endDate(), normalizeNullable(command.description()), actor.userId());
        auditEvents.save(AuditEvent.project(
                "PROJECT_CYCLE_UPDATED",
                actor.userId().toString(),
                actor.tenantId(),
                projectId,
                "PROJECT_TEST_CYCLE",
                cycle.id().toString(),
                correlationId));
        return toCycleSummary(cycle);
    }

    @Transactional
    public void deleteCycle(AuthenticatedUser actor, UUID projectId, UUID cycleId, String ifMatch, String correlationId) {
        requireProject(actor.tenantId(), projectId);
        ProjectTestCycle cycle = cycles.findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(actor.tenantId(), projectId, cycleId)
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
        requireVersion(cycle.version(), ifMatch);
        long requirementReferences = requirements.countByProjectIdAndTestCycleIdAndDeletedAtIsNull(projectId, cycleId);
        long testCaseReferences = testCases.countByProjectIdAndTestCycleIdAndDeletedAtIsNull(projectId, cycleId);
        if (requirementReferences + testCaseReferences > 0) {
            throw new ApiConflictException("Cycle cannot be deleted because it is referenced by "
                    + requirementReferences
                    + (requirementReferences == 1 ? " requirement" : " requirements")
                    + " and "
                    + testCaseReferences
                    + (testCaseReferences == 1 ? " test case" : " test cases")
                    + ". Reassign or remove those references before deleting the cycle.");
        }
        cycle.softDelete(actor.userId());
        auditEvents.save(AuditEvent.project(
                "PROJECT_CYCLE_DELETED",
                actor.userId().toString(),
                actor.tenantId(),
                projectId,
                "PROJECT_TEST_CYCLE",
                cycle.id().toString(),
                correlationId));
    }

    private Project requireProject(String tenantId, UUID projectId) {
        return projects.findByTenantIdAndIdAndActiveTrueAndDeletedAtIsNull(tenantId, projectId)
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
    }

    private boolean restricted(AuthenticatedUser user) {
        return !user.globalAdministrator()
                && users.findById(user.userId()).map(candidate -> candidate.assignmentScoped()).orElse(false);
    }

    private ProjectSuiteAssignment requireSuiteAssignedToProject(String tenantId, UUID projectId, UUID suiteId) {
        return suiteAssignments.findByTenantIdAndProjectIdAndSuiteIdAndDeletedAtIsNull(tenantId, projectId, suiteId)
                .filter(ProjectSuiteAssignment::active)
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
    }

    private ProjectSuiteAssignmentSummary toAssignmentSummary(ProjectSuiteAssignment assignment, TestSuite suite) {
        if (suite == null) {
            throw new ApiConflictException("Suite assignment references an unavailable suite.");
        }
        return new ProjectSuiteAssignmentSummary(
                assignment.id(),
                assignment.projectId(),
                suite.id(),
                suite.suiteKey(),
                suite.name(),
                suite.description(),
                assignment.active(),
                assignment.version(),
                suite.version());
    }

    private SuiteCatalogSummary toCatalogSummary(TestSuite suite) {
        return new SuiteCatalogSummary(
                suite.id(),
                suite.suiteKey(),
                suite.name(),
                suite.description(),
                suite.active(),
                suite.version());
    }

    private ProjectCycleSummary toCycleSummary(ProjectTestCycle cycle) {
        return new ProjectCycleSummary(
                cycle.id(),
                cycle.projectId(),
                cycle.name(),
                cycle.startDate(),
                cycle.endDate(),
                cycle.description(),
                cycle.active(),
                cycle.version());
    }

    private static void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new ApiConflictException("Cycle start date must be before or equal to end date.");
        }
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

    private static String normalizeKey(String value) {
        String normalized = value.trim()
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("^_+|_+$", "")
                .toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new ApiConflictException("Suite name is required.");
        }
        return normalized;
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record SuiteCatalogSummary(
            UUID id, String suiteKey, String name, String description, boolean active, int version) {}

    public record ProjectSuiteAssignmentSummary(
            UUID id,
            UUID projectId,
            UUID suiteId,
            String suiteKey,
            String name,
            String description,
            boolean active,
            int version,
            int suiteVersion) {}

    public record ProjectCycleSummary(
            UUID id,
            UUID projectId,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            String description,
            boolean active,
            int version) {}

    public record AssignSuiteCommand(
            UUID suiteId,
            @NotBlank @Size(max = 160) String name,
            @Size(max = 4000) String description) {}

    public record UpdateSuiteCommand(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 4000) String description) {}

    public record SaveCycleCommand(
            @NotBlank @Size(max = 160) String name,
            LocalDate startDate,
            LocalDate endDate,
            @Size(max = 4000) String description) {}
}
