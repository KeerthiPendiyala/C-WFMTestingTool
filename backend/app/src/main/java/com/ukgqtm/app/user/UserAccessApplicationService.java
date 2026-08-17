package com.ukgqtm.app.user;

import com.ukgqtm.app.api.ApiConflictException;
import com.ukgqtm.app.role.RoleApplicationService;
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
import com.ukgqtm.project.domain.AccessRole;
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
    private final RoleApplicationService roles;
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
            RoleApplicationService roles,
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
        this.roles = roles;
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
        AccessRole role = roles.requireRole(actor.tenantId(), command.roleId());
        String email = command.email().trim().toLowerCase(Locale.ROOT);
        if (users.findByNormalizedContactEmailAndDeletedAtIsNull(email).isPresent()) {
            throw new ApiConflictException("A user with this email already exists.");
        }

        Set<UUID> projectIds = new HashSet<>(command.projectIds());
        if (projectIds.size() != command.projectIds().size()) {
            throw new ApiConflictException("Projects must not contain duplicates.");
        }
        if (!role.administratorRole() && projectIds.isEmpty()) {
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
        ApplicationUser user = users.save(ApplicationUser.localUser(
                command.firstName().trim(),
                command.lastName().trim(),
                email,
                command.status() == UserStatus.ACTIVE,
                true));
        credentials.save(LocalUserCredential.create(
                user.id(), actor.tenantId(), passwordEncoder.encode(command.password())));

        roles.assignRole(actor.tenantId(), user.id(), role.id(), actor.userId());
        if (role.administratorRole()) {
            administrators.save(GlobalAdministratorAssignment.assign(user.id(), actor.userId()));
        } else {
            ProjectRole projectRole = membershipRole(role);
            memberships.saveAll(projectIds.stream()
                    .map(projectId -> ProjectMembership.create(
                            actor.tenantId(), projectId, user.id(), projectRole, actor.userId()))
                    .toList());
        }

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

    @Transactional
    public UserSummary updateUser(
            AuthenticatedUser actor, UUID userId, UpdateUserCommand command, String correlationId) {
        ApplicationUser user = users.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiConflictException("The selected user is unavailable."));
        String email = command.email().trim().toLowerCase(Locale.ROOT);
        users.findByNormalizedContactEmailAndDeletedAtIsNull(email)
                .filter(existing -> !existing.id().equals(userId))
                .ifPresent(existing -> {
                    throw new ApiConflictException("A user with this email already exists.");
                });

        AccessRole role = roles.requireRole(actor.tenantId(), command.roleId());
        Set<UUID> projectIds = validateProjects(actor, role, command.projectIds());
        List<ProjectMembership> currentMemberships =
                memberships.findByTenantIdAndUserIdAndDeletedAtIsNull(actor.tenantId(), userId);

        roles.assignRole(actor.tenantId(), user.id(), role.id(), actor.userId());
        if (role.administratorRole()) {
            currentMemberships.forEach(membership -> membership.disable(actor.userId()));
            if (!administrators.existsByUserIdAndDeletedAtIsNull(userId)) {
                administrators.save(GlobalAdministratorAssignment.assign(userId, actor.userId()));
            }
        } else {
            administrators.findByUserIdAndDeletedAtIsNull(userId)
                    .ifPresent(assignment -> assignment.revoke(actor.userId()));
            ProjectRole projectRole = membershipRole(role);
            Map<UUID, ProjectMembership> membershipsByProject = currentMemberships.stream()
                    .collect(Collectors.toMap(ProjectMembership::projectId, value -> value));
            currentMemberships.forEach(membership -> {
                if (projectIds.contains(membership.projectId())) {
                    membership.changeRole(projectRole, actor.userId());
                } else {
                    membership.disable(actor.userId());
                }
            });
            memberships.saveAll(projectIds.stream()
                    .filter(projectId -> !membershipsByProject.containsKey(projectId))
                    .map(projectId -> ProjectMembership.create(
                            actor.tenantId(), projectId, userId, projectRole, actor.userId()))
                    .toList());
        }

        synchronizeAssignmentScope(actor, user, role, projectIds);
        resetPasswordIfRequested(actor, user, command, correlationId);
        user.updateAccessProfile(
                command.firstName().trim(),
                command.lastName().trim(),
                email,
                command.status() == UserStatus.ACTIVE);
        auditEvents.save(AuditEvent.authentication(
                "USER_ACCESS_UPDATED",
                actor.userId().toString(),
                actor.tenantId(),
                user.id().toString(),
                correlationId));
        return toSummary(actor.tenantId(), user);
    }

    private void resetPasswordIfRequested(
            AuthenticatedUser actor, ApplicationUser user, UpdateUserCommand command, String correlationId) {
        String newPassword = command.newPassword() == null ? "" : command.newPassword();
        String confirmation = command.confirmNewPassword() == null ? "" : command.confirmNewPassword();
        if (newPassword.isEmpty() && confirmation.isEmpty()) {
            return;
        }
        validatePassword(newPassword, confirmation);
        LocalUserCredential credential = credentials.findById(user.id())
                .orElseThrow(() -> new ApiConflictException("This user does not have a local password to reset."));
        credential.resetPassword(passwordEncoder.encode(newPassword));
        auditEvents.save(AuditEvent.authentication(
                "USER_PASSWORD_RESET",
                actor.userId().toString(),
                actor.tenantId(),
                user.id().toString(),
                correlationId));
    }

    private Set<UUID> validateProjects(AuthenticatedUser actor, AccessRole role, List<UUID> requestedProjectIds) {
        Set<UUID> projectIds = new HashSet<>(requestedProjectIds);
        if (projectIds.size() != requestedProjectIds.size()) {
            throw new ApiConflictException("Projects must not contain duplicates.");
        }
        if (!role.administratorRole() && projectIds.isEmpty()) {
            throw new ApiConflictException("Select at least one project for a non-Administrator user.");
        }
        projectIds.forEach(projectId -> projects
                .findByTenantIdAndIdAndActiveTrueAndDeletedAtIsNull(actor.tenantId(), projectId)
                .orElseThrow(() -> new ApiConflictException("A selected project is unavailable.")));
        return projectIds;
    }

    private void synchronizeAssignmentScope(
            AuthenticatedUser actor,
            ApplicationUser user,
            AccessRole role,
            Set<UUID> requestedProjectIds) {
        Set<UUID> retainedProjectIds = role.administratorRole() ? Set.of() : requestedProjectIds;
        List<UserProjectPermission> currentPermissions =
                permissions.findByTenantIdAndUserId(actor.tenantId(), user.id());
        permissions.deleteAllInBatch(currentPermissions);
        suiteScopes.deleteAll(suiteScopes.findByTenantIdAndUserId(actor.tenantId(), user.id()).stream()
                .filter(value -> !retainedProjectIds.contains(value.projectId()))
                .toList());
        cycleScopes.deleteAll(cycleScopes.findByTenantIdAndUserId(actor.tenantId(), user.id()).stream()
                .filter(value -> !retainedProjectIds.contains(value.projectId()))
                .toList());

    }

    private UserSummary toSummary(String tenantId, ApplicationUser user) {
        List<ProjectMembership> userMemberships =
                memberships.findByTenantIdAndUserIdAndDeletedAtIsNull(tenantId, user.id());
        AccessRole assignedRole = roles.roleForUser(tenantId, user.id()).orElse(null);
        boolean administrator = assignedRole != null
                ? assignedRole.administratorRole()
                : administrators.existsByUserIdAndDeletedAtIsNull(user.id());
        List<UUID> activeProjectIds = userMemberships.stream()
                .filter(ProjectMembership::active)
                .map(ProjectMembership::projectId)
                .toList();
        List<AccessPermission> assignedPermissions = assignedRole == null
                ? List.of(AccessPermission.VIEW)
                : roles.permissionsForRole(assignedRole.id()).stream().sorted().toList();
        return new UserSummary(
                user.id(),
                user.firstName(),
                user.lastName(),
                user.normalizedContactEmail(),
                assignedRole == null ? null : assignedRole.id(),
                assignedRole == null ? (administrator ? "Admin" : "Viewer") : assignedRole.name(),
                administrator,
                user.accessStatus(),
                activeProjectIds,
                assignedPermissions);
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
            @NotNull UUID roleId,
            @NotNull UserStatus status,
            @NotNull List<UUID> projectIds,
            @NotNull List<UUID> suiteAssignmentIds,
            @NotNull List<UUID> testCycleIds) {}

    public record UpdateUserCommand(
            @NotBlank @Size(max = 120) String firstName,
            @NotBlank @Size(max = 120) String lastName,
            @NotBlank @Email @Size(max = 320) String email,
            @NotNull UUID roleId,
            @NotNull UserStatus status,
            @NotNull List<UUID> projectIds,
            @Size(max = 200) String newPassword,
            @Size(max = 200) String confirmNewPassword) {}

    public record UserSummary(
            UUID id,
            String firstName,
            String lastName,
            String email,
            UUID roleId,
            String roleName,
            boolean administratorRole,
            String status,
            List<UUID> projectIds,
            List<AccessPermission> permissions) {}

    private static ProjectRole membershipRole(AccessRole role) {
        return "Test Manager".equalsIgnoreCase(role.name())
                ? ProjectRole.TEST_MANAGER
                : ProjectRole.TEST_ANALYST;
    }
}
