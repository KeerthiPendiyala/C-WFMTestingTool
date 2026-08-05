package com.ukgqtm.app.user;

import com.ukgqtm.app.api.ApiConflictException;
import com.ukgqtm.audit.domain.AuditEvent;
import com.ukgqtm.audit.repository.AuditEventRepository;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.identity.domain.ApplicationUser;
import com.ukgqtm.identity.domain.GlobalAdministratorAssignment;
import com.ukgqtm.identity.domain.LocalUserCredential;
import com.ukgqtm.identity.repository.ApplicationUserRepository;
import com.ukgqtm.identity.repository.GlobalAdministratorAssignmentRepository;
import com.ukgqtm.identity.repository.LocalUserCredentialRepository;
import com.ukgqtm.project.domain.AccessPermission;
import com.ukgqtm.project.domain.ProjectMembership;
import com.ukgqtm.project.domain.ProjectRole;
import com.ukgqtm.project.domain.UserCycleScope;
import com.ukgqtm.project.domain.UserProjectPermission;
import com.ukgqtm.project.domain.UserSuiteScope;
import com.ukgqtm.project.repository.ProjectMembershipRepository;
import com.ukgqtm.project.repository.ProjectRepository;
import com.ukgqtm.project.repository.ProjectSuiteAssignmentRepository;
import com.ukgqtm.project.repository.ProjectTestCycleRepository;
import com.ukgqtm.project.repository.UserCycleScopeRepository;
import com.ukgqtm.project.repository.UserProjectPermissionRepository;
import com.ukgqtm.project.repository.UserSuiteScopeRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccessApplicationService {
    private final ApplicationUserRepository users;
    private final LocalUserCredentialRepository credentials;
    private final GlobalAdministratorAssignmentRepository administrators;
    private final ProjectRepository projects;
    private final ProjectMembershipRepository memberships;
    private final ProjectSuiteAssignmentRepository suiteAssignments;
    private final ProjectTestCycleRepository cycles;
    private final UserProjectPermissionRepository permissions;
    private final UserSuiteScopeRepository suiteScopes;
    private final UserCycleScopeRepository cycleScopes;
    private final AuditEventRepository auditEvents;
    private final PasswordEncoder passwordEncoder;

    public UserAccessApplicationService(
            ApplicationUserRepository users,
            LocalUserCredentialRepository credentials,
            GlobalAdministratorAssignmentRepository administrators,
            ProjectRepository projects,
            ProjectMembershipRepository memberships,
            ProjectSuiteAssignmentRepository suiteAssignments,
            ProjectTestCycleRepository cycles,
            UserProjectPermissionRepository permissions,
            UserSuiteScopeRepository suiteScopes,
            UserCycleScopeRepository cycleScopes,
            AuditEventRepository auditEvents,
            PasswordEncoder passwordEncoder) {
        this.users = users;
        this.credentials = credentials;
        this.administrators = administrators;
        this.projects = projects;
        this.memberships = memberships;
        this.suiteAssignments = suiteAssignments;
        this.cycles = cycles;
        this.permissions = permissions;
        this.suiteScopes = suiteScopes;
        this.cycleScopes = cycleScopes;
        this.auditEvents = auditEvents;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserSummary> listUsers(AuthenticatedUser actor) {
        return users.findByDeletedAtIsNullOrderByLastNameAscFirstNameAsc().stream()
                .map(user -> toSummary(actor.tenantId(), user))
                .toList();
    }

    @Transactional
    public UserSummary createUser(AuthenticatedUser actor, CreateUserCommand command, String correlationId) {
        validatePassword(command.password(), command.confirmPassword());
        String email = command.email().trim().toLowerCase(Locale.ROOT);
        if (users.findByNormalizedContactEmailAndDeletedAtIsNull(email).isPresent()) {
            throw new ApiConflictException("A user with this email already exists.");
        }

        Set<UUID> projectIds = new HashSet<>(command.projectIds());
        if (projectIds.size() != command.projectIds().size()) {
            throw new ApiConflictException("Projects must not contain duplicates.");
        }
        if (command.role() != UserRole.ADMINISTRATOR && projectIds.isEmpty()) {
            throw new ApiConflictException("Select at least one project for a non-Administrator user.");
        }
        projectIds.forEach(projectId -> projects
                .findByTenantIdAndIdAndActiveTrueAndDeletedAtIsNull(actor.tenantId(), projectId)
                .orElseThrow(() -> new ApiConflictException("A selected project is unavailable.")));

        Map<UUID, UUID> suiteProjects = command.suiteAssignmentIds().stream()
                .map(id -> suiteAssignments
                        .findById(id)
                        .filter(value -> value.active() && actor.tenantId().equals(value.tenantId()))
                        .orElseThrow(() -> new ApiConflictException("A selected test suite is unavailable.")))
                .collect(Collectors.toMap(value -> value.id(), value -> value.projectId(), (left, right) -> left));
        if (suiteProjects.size() != command.suiteAssignmentIds().size()) {
            throw new ApiConflictException("Test suites must not contain duplicates.");
        }
        if (!projectIds.containsAll(suiteProjects.values())) {
            throw new ApiConflictException("Test suites must belong to a selected project.");
        }

        Map<UUID, UUID> cycleProjects = command.testCycleIds().stream()
                .map(id -> cycles
                        .findById(id)
                        .filter(value -> value.active() && actor.tenantId().equals(value.tenantId()))
                        .orElseThrow(() -> new ApiConflictException("A selected test cycle is unavailable.")))
                .collect(Collectors.toMap(value -> value.id(), value -> value.projectId(), (left, right) -> left));
        if (cycleProjects.size() != command.testCycleIds().size()) {
            throw new ApiConflictException("Test cycles must not contain duplicates.");
        }
        if (!projectIds.containsAll(cycleProjects.values())) {
            throw new ApiConflictException("Test cycles must belong to a selected project.");
        }
        if (!command.permissions().contains(AccessPermission.VIEW)) {
            throw new ApiConflictException("View permission is required for assigned projects.");
        }

        ApplicationUser user = users.save(ApplicationUser.localUser(
                command.firstName().trim(),
                command.lastName().trim(),
                email,
                command.status() == UserStatus.ACTIVE,
                true));
        credentials.save(LocalUserCredential.create(
                user.id(), actor.tenantId(), passwordEncoder.encode(command.password())));

        if (command.role() == UserRole.ADMINISTRATOR) {
            administrators.save(GlobalAdministratorAssignment.assign(user.id(), actor.userId()));
        } else {
            ProjectRole projectRole = command.role().projectRole();
            memberships.saveAll(projectIds.stream()
                    .map(projectId -> ProjectMembership.create(
                            actor.tenantId(), projectId, user.id(), projectRole, actor.userId()))
                    .toList());
        }

        permissions.saveAll(projectIds.stream()
                .flatMap(projectId -> command.permissions().stream().map(permission -> UserProjectPermission.create(
                        actor.tenantId(), user.id(), projectId, permission, actor.userId())))
                .toList());
        suiteScopes.saveAll(suiteProjects.entrySet().stream()
                .map(entry -> UserSuiteScope.create(
                        actor.tenantId(), user.id(), entry.getValue(), entry.getKey(), actor.userId()))
                .toList());
        cycleScopes.saveAll(cycleProjects.entrySet().stream()
                .map(entry -> UserCycleScope.create(
                        actor.tenantId(), user.id(), entry.getValue(), entry.getKey(), actor.userId()))
                .toList());
        auditEvents.save(AuditEvent.authentication(
                "USER_CREATED_WITH_ACCESS",
                actor.userId().toString(),
                actor.tenantId(),
                user.id().toString(),
                correlationId));
        return toSummary(actor.tenantId(), user);
    }

    private UserSummary toSummary(String tenantId, ApplicationUser user) {
        List<ProjectMembership> userMemberships =
                memberships.findByTenantIdAndUserIdAndDeletedAtIsNull(tenantId, user.id());
        boolean administrator = administrators.existsByUserIdAndDeletedAtIsNull(user.id());
        String role = administrator
                ? UserRole.ADMINISTRATOR.name()
                : userMemberships.stream()
                        .map(ProjectMembership::projectRole)
                        .findFirst()
                        .map(ProjectRole::fromDatabaseValue)
                        .map(value -> UserRole.valueOf(value.name()).name())
                        .orElse(UserRole.TEST_ANALYST.name());
        return new UserSummary(
                user.id(),
                user.firstName(),
                user.lastName(),
                user.normalizedContactEmail(),
                role,
                user.isActive() ? UserStatus.ACTIVE.name() : UserStatus.INACTIVE.name(),
                userMemberships.stream().filter(ProjectMembership::active).map(ProjectMembership::projectId).toList());
    }

    private static void validatePassword(String password, String confirmation) {
        if (!password.equals(confirmation)) {
            throw new ApiConflictException("Password and confirmation must match.");
        }
        if (password.length() < 10
                || password.chars().noneMatch(Character::isUpperCase)
                || password.chars().noneMatch(Character::isLowerCase)
                || password.chars().noneMatch(Character::isDigit)
                || password.chars().allMatch(Character::isLetterOrDigit)) {
            throw new ApiConflictException(
                    "Password must be at least 10 characters and include upper, lower, number and special characters.");
        }
    }

    public enum UserRole {
        ADMINISTRATOR,
        TEST_MANAGER,
        TEST_LEAD,
        TEST_ANALYST;

        ProjectRole projectRole() {
            return ProjectRole.valueOf(name());
        }
    }

    public enum UserStatus {
        ACTIVE,
        INACTIVE
    }

    public record CreateUserCommand(
            @NotBlank @Size(max = 120) String firstName,
            @NotBlank @Size(max = 120) String lastName,
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 10, max = 200) String password,
            @NotBlank @Size(min = 10, max = 200) String confirmPassword,
            @NotNull UserRole role,
            @NotNull UserStatus status,
            @NotNull List<UUID> projectIds,
            @NotNull List<UUID> suiteAssignmentIds,
            @NotNull List<UUID> testCycleIds,
            @NotEmpty Set<AccessPermission> permissions) {}

    public record UserSummary(
            UUID id,
            String firstName,
            String lastName,
            String email,
            String role,
            String status,
            List<UUID> projectIds) {}
}
