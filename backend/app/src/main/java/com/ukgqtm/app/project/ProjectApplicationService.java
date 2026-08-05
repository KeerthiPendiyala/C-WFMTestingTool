package com.ukgqtm.app.project;

import com.ukgqtm.app.api.ApiConflictException;
import com.ukgqtm.audit.domain.AuditEvent;
import com.ukgqtm.audit.repository.AuditEventRepository;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.identity.domain.ApplicationUser;
import com.ukgqtm.identity.repository.ApplicationUserRepository;
import com.ukgqtm.project.domain.Project;
import com.ukgqtm.project.domain.ProjectMembership;
import com.ukgqtm.project.domain.ProjectRole;
import com.ukgqtm.project.repository.ProjectMembershipRepository;
import com.ukgqtm.project.repository.ProjectRepository;
import com.ukgqtm.project.repository.ProjectSuiteAssignmentRepository;
import com.ukgqtm.project.repository.ProjectTestCycleRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectApplicationService {
    private static final String ACTIVE = "ACTIVE";

    private final ProjectRepository projects;
    private final ProjectMembershipRepository memberships;
    private final ProjectSuiteAssignmentRepository suiteAssignments;
    private final ProjectTestCycleRepository cycles;
    private final ApplicationUserRepository users;
    private final AuditEventRepository auditEvents;

    public ProjectApplicationService(
            ProjectRepository projects,
            ProjectMembershipRepository memberships,
            ProjectSuiteAssignmentRepository suiteAssignments,
            ProjectTestCycleRepository cycles,
            ApplicationUserRepository users,
            AuditEventRepository auditEvents) {
        this.projects = projects;
        this.memberships = memberships;
        this.suiteAssignments = suiteAssignments;
        this.cycles = cycles;
        this.users = users;
        this.auditEvents = auditEvents;
    }

    @Transactional(readOnly = true)
    public List<ProjectSummary> visibleProjects(AuthenticatedUser user) {
        List<Project> visible = user.globalAdministrator()
                ? projects.findByTenantIdAndActiveTrueAndDeletedAtIsNullOrderByNameAsc(user.tenantId())
                : projects.findAssignedActiveProjects(user.tenantId(), user.userId());
        return visible.stream().map(this::toSummary).toList();
    }

    @Transactional
    public ProjectSummary createProject(AuthenticatedUser user, CreateProjectCommand command, String correlationId) {
        String name = command.name().trim();
        String projectKey = normalizeProjectKey(
                command.projectKey() == null || command.projectKey().isBlank() ? name : command.projectKey());
        if (projectKey.isBlank()) {
            throw new ApiConflictException("Project code is required.");
        }
        if (projects.existsByTenantIdAndProjectKeyAndDeletedAtIsNull(user.tenantId(), projectKey)) {
            throw new ApiConflictException("Project code already exists.");
        }
        if (projects.existsActiveName(user.tenantId(), name)) {
            throw new ApiConflictException("Project name already exists.");
        }

        Project project = projects.save(Project.create(
                user.tenantId(), projectKey, name, normalizeNullable(command.description()), user.userId()));
        auditEvents.save(AuditEvent.project(
                "PROJECT_CREATED",
                user.userId().toString(),
                user.tenantId(),
                project.id(),
                "PROJECT",
                project.id().toString(),
                correlationId));
        return toSummary(project);
    }

    @Transactional(readOnly = true)
    public Optional<ProjectSummary> visibleProject(AuthenticatedUser user, UUID projectId) {
        return projects.findByTenantIdAndIdAndActiveTrueAndDeletedAtIsNull(user.tenantId(), projectId)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public ProjectDetail projectDetail(AuthenticatedUser user, UUID projectId) {
        Project project = requireProject(user.tenantId(), projectId);
        return new ProjectDetail(toSummary(project), listProjectMemberships(user, projectId));
    }

    @Transactional(readOnly = true)
    public List<ProjectMembershipSummary> listProjectMemberships(AuthenticatedUser user, UUID projectId) {
        requireProject(user.tenantId(), projectId);
        List<ProjectMembership> projectMemberships =
                memberships.findByTenantIdAndProjectIdAndDeletedAtIsNullOrderByProjectRoleAsc(user.tenantId(), projectId);
        Map<UUID, ApplicationUser> usersById = users.findByIdInAndDeletedAtIsNull(
                        projectMemberships.stream().map(ProjectMembership::userId).toList())
                .stream()
                .collect(Collectors.toMap(ApplicationUser::id, Function.identity()));
        return projectMemberships.stream()
                .map(membership -> toMembershipSummary(membership, usersById.get(membership.userId())))
                .sorted(Comparator.comparing(ProjectMembershipSummary::lastName)
                        .thenComparing(ProjectMembershipSummary::firstName))
                .toList();
    }

    @Transactional
    public ProjectMembershipSummary addProjectMembership(
            AuthenticatedUser actor, UUID projectId, AddProjectMemberCommand command, String correlationId) {
        requireProject(actor.tenantId(), projectId);
        String normalizedEmail = normalizeEmail(command.email());
        ProjectRole role = command.projectRole();

        ApplicationUser user = users.findByNormalizedContactEmailAndDeletedAtIsNull(normalizedEmail)
                .orElseGet(() -> users.save(ApplicationUser.preProvision(
                        command.firstName().trim(),
                        command.lastName().trim(),
                        normalizedEmail,
                        actor.userId())));

        if (user.isDisabled()) {
            throw new ApiConflictException("User is disabled.");
        }
        memberships.findByTenantIdAndProjectIdAndUserIdAndDeletedAtIsNull(actor.tenantId(), projectId, user.id())
                .ifPresent(existing -> {
                    throw new ApiConflictException("User is already assigned to this project.");
                });

        ProjectMembership membership =
                memberships.save(ProjectMembership.create(actor.tenantId(), projectId, user.id(), role, actor.userId()));
        auditEvents.save(AuditEvent.project(
                "PROJECT_MEMBERSHIP_ADDED",
                actor.userId().toString(),
                actor.tenantId(),
                projectId,
                "PROJECT_MEMBERSHIP",
                membership.id().toString(),
                correlationId));
        auditEvents.save(AuditEvent.project(
                "PROJECT_INVITATION_RECORDED",
                actor.userId().toString(),
                actor.tenantId(),
                projectId,
                "APPLICATION_USER",
                user.id().toString(),
                correlationId));
        return toMembershipSummary(membership, user);
    }

    @Transactional
    public ProjectMembershipSummary changeProjectMembershipRole(
            AuthenticatedUser actor,
            UUID projectId,
            UUID membershipId,
            ChangeProjectMemberRoleCommand command,
            String correlationId) {
        ProjectMembership membership = requireMembership(actor.tenantId(), projectId, membershipId);
        ProjectRole previousRole = ProjectRole.fromDatabaseValue(membership.projectRole());
        ProjectRole nextRole = command.projectRole();
        validateLastManager(actor, projectId, previousRole, nextRole, command.allowLastManagerOverride());
        membership.changeRole(nextRole, actor.userId());
        auditEvents.save(AuditEvent.project(
                "PROJECT_MEMBERSHIP_ROLE_CHANGED",
                actor.userId().toString(),
                actor.tenantId(),
                projectId,
                "PROJECT_MEMBERSHIP",
                membership.id().toString(),
                correlationId));
        ApplicationUser user = users.findById(membership.userId())
                .orElseThrow(() -> new ApiConflictException("Membership user is unavailable."));
        return toMembershipSummary(membership, user);
    }

    @Transactional
    public void disableProjectMembership(
            AuthenticatedUser actor,
            UUID projectId,
            UUID membershipId,
            boolean allowLastManagerOverride,
            String correlationId) {
        ProjectMembership membership = requireMembership(actor.tenantId(), projectId, membershipId);
        ProjectRole previousRole = ProjectRole.fromDatabaseValue(membership.projectRole());
        validateLastManager(actor, projectId, previousRole, null, allowLastManagerOverride);
        membership.disable(actor.userId());
        auditEvents.save(AuditEvent.project(
                "PROJECT_MEMBERSHIP_REMOVED",
                actor.userId().toString(),
                actor.tenantId(),
                projectId,
                "PROJECT_MEMBERSHIP",
                membership.id().toString(),
                correlationId));
    }

    private ProjectSummary toSummary(Project project) {
        return new ProjectSummary(
                project.id(),
                project.projectKey(),
                project.name(),
                project.description(),
                project.active(),
                suiteAssignments.countByTenantIdAndProjectIdAndActiveTrueAndDeletedAtIsNull(project.tenantId(), project.id()),
                cycles.countByTenantIdAndProjectIdAndActiveTrueAndDeletedAtIsNull(project.tenantId(), project.id()),
                memberships.countByTenantIdAndProjectIdAndMembershipStatusAndDeletedAtIsNull(
                        project.tenantId(), project.id(), ACTIVE));
    }

    private ProjectMembershipSummary toMembershipSummary(ProjectMembership membership, ApplicationUser user) {
        if (user == null) {
            throw new ApiConflictException("Membership user is unavailable.");
        }
        return new ProjectMembershipSummary(
                membership.id(),
                user.id(),
                user.firstName(),
                user.lastName(),
                user.normalizedContactEmail(),
                membership.projectRole(),
                membership.membershipStatus(),
                user.accessStatus(),
                user.hasAnyBinding());
    }

    private Project requireProject(String tenantId, UUID projectId) {
        return projects.findByTenantIdAndIdAndActiveTrueAndDeletedAtIsNull(tenantId, projectId)
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
    }

    private ProjectMembership requireMembership(String tenantId, UUID projectId, UUID membershipId) {
        requireProject(tenantId, projectId);
        return memberships.findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(tenantId, projectId, membershipId)
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
    }

    private void validateLastManager(
            AuthenticatedUser actor,
            UUID projectId,
            ProjectRole previousRole,
            ProjectRole nextRole,
            boolean allowLastManagerOverride) {
        boolean removesManager = previousRole == ProjectRole.TEST_MANAGER && nextRole != ProjectRole.TEST_MANAGER;
        if (!removesManager) {
            return;
        }
        long managers = memberships.countByTenantIdAndProjectIdAndProjectRoleAndMembershipStatusAndDeletedAtIsNull(
                actor.tenantId(), projectId, ProjectRole.TEST_MANAGER.databaseValue(), ACTIVE);
        if (managers > 1) {
            return;
        }
        if (actor.globalAdministrator() && allowLastManagerOverride) {
            return;
        }
        throw new ApiConflictException("Cannot remove the last effective project manager without Administrator override.");
    }

    private static String normalizeProjectKey(String value) {
        return value.trim()
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("^_+|_+$", "")
                .toUpperCase(Locale.ROOT);
    }

    private static String normalizeEmail(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record ProjectSummary(
            UUID id,
            String projectKey,
            String name,
            String description,
            boolean active,
            long suiteCount,
            long cycleCount,
            long userCount) {}

    public record ProjectDetail(ProjectSummary project, List<ProjectMembershipSummary> memberships) {}

    public record ProjectMembershipSummary(
            UUID id,
            UUID userId,
            String firstName,
            String lastName,
            String email,
            String projectRole,
            String membershipStatus,
            String invitationStatus,
            boolean entraBound) {}

    public record CreateProjectCommand(
            @Size(max = 80) String projectKey,
            @NotBlank @Size(max = 240) String name,
            @Size(max = 4000) String description) {}

    public record AddProjectMemberCommand(
            @NotBlank @Size(max = 120) String firstName,
            @NotBlank @Size(max = 120) String lastName,
            @NotBlank @Email @Size(max = 320) String email,
            @NotNull ProjectRole projectRole) {}

    public record ChangeProjectMemberRoleCommand(
            @NotNull ProjectRole projectRole,
            boolean allowLastManagerOverride) {}
}
