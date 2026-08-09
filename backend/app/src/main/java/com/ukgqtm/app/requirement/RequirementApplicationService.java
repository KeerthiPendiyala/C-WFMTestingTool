package com.ukgqtm.app.requirement;

import com.ukgqtm.app.api.ApiConflictException;
import com.ukgqtm.app.security.AssignmentScopeAuthorizationService;
import com.ukgqtm.audit.domain.AuditEvent;
import com.ukgqtm.audit.repository.AuditEventRepository;
import com.ukgqtm.identity.api.AuthenticatedUser;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequirementApplicationService {
    private final ProjectRepository projects;
    private final ProjectSuiteAssignmentRepository suiteAssignments;
    private final TestSuiteRepository suites;
    private final ProjectTestCycleRepository cycles;
    private final ProjectIdentifierCounterRepository counters;
    private final RequirementRepository requirements;
    private final TestCaseRepository testCases;
    private final AuditEventRepository auditEvents;
    private final AssignmentScopeAuthorizationService assignmentScope;

    public RequirementApplicationService(
            ProjectRepository projects,
            ProjectSuiteAssignmentRepository suiteAssignments,
            TestSuiteRepository suites,
            ProjectTestCycleRepository cycles,
            ProjectIdentifierCounterRepository counters,
            RequirementRepository requirements,
            TestCaseRepository testCases,
            AuditEventRepository auditEvents,
            AssignmentScopeAuthorizationService assignmentScope) {
        this.projects = projects;
        this.suiteAssignments = suiteAssignments;
        this.suites = suites;
        this.cycles = cycles;
        this.counters = counters;
        this.requirements = requirements;
        this.testCases = testCases;
        this.auditEvents = auditEvents;
        this.assignmentScope = assignmentScope;
    }

    @Transactional(readOnly = true)
    public List<RequirementSummary> list(AuthenticatedUser user, UUID projectId) {
        requireProject(user, projectId);
        return requirements.findByTenantIdAndProjectIdAndDeletedAtIsNullOrderByCreatedDateDesc(
                        user.tenantId(), projectId)
                .stream()
                .filter(requirement -> assignmentScope.canAccess(
                        user,
                        requirement.projectId(),
                        requirement.projectSuiteAssignmentId(),
                        requirement.testCycleId()))
                .map(requirement -> summarize(user, requirement))
                .toList();
    }

    @Transactional
    public RequirementSummary createManual(
            AuthenticatedUser actor, CreateManualRequirementCommand command, String correlationId) {
        requireProject(actor, command.projectId());
        ProjectSuiteAssignment assignment = requireSuiteAssignment(
                actor, command.projectId(), command.projectSuiteAssignmentId());
        ProjectTestCycle cycle = requireCycle(actor, command.projectId(), command.testCycleId());
        assignmentScope.requireAccess(
                actor, command.projectId(), command.projectSuiteAssignmentId(), command.testCycleId());
        int sequence = counters.allocate(command.projectId(), "REQ");
        Requirement requirement = requirements.save(Requirement.createManual(
                actor.tenantId(),
                command.projectId(),
                assignment.id(),
                cycle.id(),
                sequence,
                command.header().trim(),
                command.description().trim()));
        auditEvents.save(AuditEvent.project(
                "REQUIREMENT_CREATED",
                actor.userId().toString(),
                actor.tenantId(),
                command.projectId(),
                "REQUIREMENT",
                requirement.id().toString(),
                correlationId));
        return summarize(actor, requirement);
    }

    @Transactional
    public RequirementSummary approve(
            AuthenticatedUser actor, UUID projectId, UUID requirementId, String ifMatch, String correlationId) {
        Requirement requirement = requireRequirement(actor, projectId, requirementId);
        requireVersion(requirement.version(), ifMatch);
        try {
            requirement.approve(actor.userId());
        } catch (IllegalStateException exception) {
            throw new ApiConflictException(exception.getMessage());
        }
        auditEvents.save(AuditEvent.project(
                "REQUIREMENT_APPROVED",
                actor.userId().toString(),
                actor.tenantId(),
                projectId,
                "REQUIREMENT",
                requirementId.toString(),
                correlationId));
        return summarize(actor, requirement);
    }

    @Transactional
    public RequirementSummary update(
            AuthenticatedUser actor,
            UUID projectId,
            UUID requirementId,
            UpdateRequirementCommand command,
            String ifMatch,
            String correlationId) {
        Requirement requirement = requireRequirement(actor, projectId, requirementId);
        requireVersion(requirement.version(), ifMatch);
        requirement.updateDetails(
                command.header().trim(),
                command.description().trim(),
                normalizeOptionalDetail(command.acceptanceCriteria()),
                normalizeOptionalDetail(command.assumptions()),
                normalizeOptionalDetail(command.dependencies()));
        auditEvents.save(AuditEvent.project(
                "REQUIREMENT_UPDATED",
                actor.userId().toString(),
                actor.tenantId(),
                projectId,
                "REQUIREMENT",
                requirementId.toString(),
                correlationId));
        return summarize(actor, requirement);
    }

    @Transactional
    public void delete(
            AuthenticatedUser actor, UUID projectId, UUID requirementId, String ifMatch, String correlationId) {
        Requirement requirement = requireRequirement(actor, projectId, requirementId);
        requireVersion(requirement.version(), ifMatch);
        if (testCases.countByRequirementIdAndDeletedAtIsNull(requirementId) > 0) {
            throw new ApiConflictException("Requirement cannot be deleted while linked test cases exist.");
        }
        requirement.softDelete();
        auditEvents.save(AuditEvent.project(
                "REQUIREMENT_DELETED",
                actor.userId().toString(),
                actor.tenantId(),
                projectId,
                "REQUIREMENT",
                requirementId.toString(),
                correlationId));
    }

    private RequirementSummary summarize(AuthenticatedUser user, Requirement requirement) {
        ProjectSuiteAssignment assignment =
                requireSuiteAssignment(user, requirement.projectId(), requirement.projectSuiteAssignmentId());
        TestSuite suite = suites.findAvailableSuite(user.tenantId(), assignment.suiteId())
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
        ProjectTestCycle cycle = requireCycle(user, requirement.projectId(), requirement.testCycleId());
        return new RequirementSummary(
                requirement.id(),
                requirement.projectId(),
                assignment.id(),
                suite.id(),
                suite.name(),
                cycle.id(),
                cycle.name(),
                requirement.reqId(),
                requirement.header(),
                requirement.description(),
                requirement.acceptanceCriteria(),
                requirement.assumptions(),
                requirement.dependencies(),
                requirement.status(),
                requirement.sourceType(),
                requirement.createdDate(),
                requirement.approvedAt(),
                requirement.approvedBy(),
                requirement.version());
    }

    private void requireProject(AuthenticatedUser user, UUID projectId) {
        projects.findByTenantIdAndIdAndActiveTrueAndDeletedAtIsNull(user.tenantId(), projectId)
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
    }

    private ProjectSuiteAssignment requireSuiteAssignment(
            AuthenticatedUser user, UUID projectId, UUID assignmentId) {
        return suiteAssignments
                .findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(user.tenantId(), projectId, assignmentId)
                .filter(ProjectSuiteAssignment::active)
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
    }

    private ProjectTestCycle requireCycle(AuthenticatedUser user, UUID projectId, UUID cycleId) {
        return cycles.findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(user.tenantId(), projectId, cycleId)
                .filter(ProjectTestCycle::active)
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
    }

    private Requirement requireRequirement(AuthenticatedUser user, UUID projectId, UUID requirementId) {
        requireProject(user, projectId);
        Requirement requirement = requirements
                .findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(user.tenantId(), projectId, requirementId)
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
        assignmentScope.requireAccess(
                user, projectId, requirement.projectSuiteAssignmentId(), requirement.testCycleId());
        return requirement;
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

    private static String normalizeOptionalDetail(String value) {
        return value == null ? "" : value.trim();
    }

    public record CreateManualRequirementCommand(
            @NotNull UUID projectId,
            @NotNull UUID projectSuiteAssignmentId,
            @NotNull UUID testCycleId,
            @NotBlank @Size(max = 300) String header,
            @NotBlank @Size(max = 20_000) String description) {}

    public record UpdateRequirementCommand(
            @NotBlank @Size(max = 300) String header,
            @NotBlank @Size(max = 20_000) String description,
            @Size(max = 20_000) String acceptanceCriteria,
            @Size(max = 20_000) String assumptions,
            @Size(max = 20_000) String dependencies) {}

    public record RequirementSummary(
            UUID id,
            UUID projectId,
            UUID projectSuiteAssignmentId,
            UUID suiteId,
            String suiteName,
            UUID testCycleId,
            String cycleName,
            String reqId,
            String header,
            String description,
            String acceptanceCriteria,
            String assumptions,
            String dependencies,
            String status,
            String sourceType,
            Instant createdDate,
            Instant approvedAt,
            UUID approvedBy,
            int version) {}
}
