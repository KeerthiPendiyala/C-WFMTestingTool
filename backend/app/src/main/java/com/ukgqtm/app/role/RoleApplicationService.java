package com.ukgqtm.app.role;

import com.ukgqtm.app.api.ApiConflictException;
import com.ukgqtm.audit.domain.AuditEvent;
import com.ukgqtm.audit.repository.AuditEventRepository;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.project.domain.AccessPermission;
import com.ukgqtm.project.domain.AccessRole;
import com.ukgqtm.project.domain.AccessRolePermission;
import com.ukgqtm.project.domain.UserAccessRoleAssignment;
import com.ukgqtm.project.repository.AccessRolePermissionRepository;
import com.ukgqtm.project.repository.AccessRoleRepository;
import com.ukgqtm.project.repository.UserAccessRoleAssignmentRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleApplicationService {
    private final AccessRoleRepository roles;
    private final AccessRolePermissionRepository rolePermissions;
    private final UserAccessRoleAssignmentRepository assignments;
    private final AuditEventRepository auditEvents;

    public RoleApplicationService(
            AccessRoleRepository roles,
            AccessRolePermissionRepository rolePermissions,
            UserAccessRoleAssignmentRepository assignments,
            AuditEventRepository auditEvents) {
        this.roles = roles;
        this.rolePermissions = rolePermissions;
        this.assignments = assignments;
        this.auditEvents = auditEvents;
    }

    @Transactional
    public List<RoleSummary> listRoles(AuthenticatedUser actor) {
        ensureDefaultRoles(actor.tenantId(), actor.userId());
        return roles.findByTenantIdOrderByNameAsc(actor.tenantId()).stream().map(this::toSummary).toList();
    }

    @Transactional
    public RoleSummary createRole(AuthenticatedUser actor, SaveRoleCommand command, String correlationId) {
        String name = command.name().trim();
        roles.findByTenantIdAndNameIgnoreCase(actor.tenantId(), name).ifPresent(existing -> {
            throw new ApiConflictException("A role with this name already exists.");
        });
        AccessRole role = roles.save(AccessRole.create(
                actor.tenantId(), name, command.description().trim(), false, actor.userId()));
        replacePermissions(role.id(), command.permissions());
        auditEvents.save(AuditEvent.authentication(
                "ACCESS_ROLE_CREATED",
                actor.userId().toString(),
                actor.tenantId(),
                role.id().toString(),
                correlationId));
        return toSummary(role);
    }

    @Transactional
    public RoleSummary updateRole(
            AuthenticatedUser actor, UUID roleId, SaveRoleCommand command, String correlationId) {
        AccessRole role = requireRole(actor.tenantId(), roleId);
        if (role.version() != command.version()) {
            throw new ApiConflictException("The role was updated by another user. Refresh and try again.");
        }
        String name = command.name().trim();
        roles.findByTenantIdAndNameIgnoreCase(actor.tenantId(), name)
                .filter(existing -> !existing.id().equals(roleId))
                .ifPresent(existing -> {
                    throw new ApiConflictException("A role with this name already exists.");
                });
        role.update(name, command.description().trim(), actor.userId());
        replacePermissions(role.id(), command.permissions());
        auditEvents.save(AuditEvent.authentication(
                "ACCESS_ROLE_UPDATED",
                actor.userId().toString(),
                actor.tenantId(),
                role.id().toString(),
                correlationId));
        return toSummary(role);
    }

    @Transactional(readOnly = true)
    public AccessRole requireRole(String tenantId, UUID roleId) {
        return roles.findByTenantIdAndId(tenantId, roleId)
                .orElseThrow(() -> new ApiConflictException("The selected role is unavailable."));
    }

    @Transactional(readOnly = true)
    public Optional<AccessRole> roleForUser(String tenantId, UUID userId) {
        return assignments.findByTenantIdAndUserId(tenantId, userId)
                .flatMap(assignment -> roles.findByTenantIdAndId(tenantId, assignment.roleId()));
    }

    @Transactional(readOnly = true)
    public Set<AccessPermission> permissionsForRole(UUID roleId) {
        return rolePermissions.findByRoleId(roleId).stream()
                .map(value -> AccessPermission.valueOf(value.permissionName()))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(AccessPermission.class)));
    }

    @Transactional
    public void assignRole(String tenantId, UUID userId, UUID roleId, UUID actorId) {
        UserAccessRoleAssignment assignment = assignments.findByTenantIdAndUserId(tenantId, userId)
                .map(existing -> {
                    existing.changeRole(roleId, actorId);
                    return existing;
                })
                .orElseGet(() -> UserAccessRoleAssignment.assign(tenantId, userId, roleId, actorId));
        assignments.save(assignment);
    }

    private void ensureDefaultRoles(String tenantId, UUID actorId) {
        if (!roles.findByTenantIdOrderByNameAsc(tenantId).isEmpty()) {
            return;
        }
        createDefault(tenantId, "Admin", "Full administrative access.", true, EnumSet.allOf(AccessPermission.class), actorId);
        createDefault(tenantId, "Test Manager", "Manages testing work and assignments.", false, EnumSet.allOf(AccessPermission.class), actorId);
        createDefault(
                tenantId,
                "Tester",
                "Creates, edits, executes and deletes test assets.",
                false,
                EnumSet.of(
                        AccessPermission.VIEW,
                        AccessPermission.CREATE,
                        AccessPermission.EDIT,
                        AccessPermission.EXECUTE,
                        AccessPermission.DELETE),
                actorId);
        createDefault(tenantId, "Viewer", "Read-only access.", false, EnumSet.of(AccessPermission.VIEW), actorId);
    }

    private void createDefault(
            String tenantId,
            String name,
            String description,
            boolean administratorRole,
            Set<AccessPermission> permissions,
            UUID actorId) {
        AccessRole role = roles.save(AccessRole.create(tenantId, name, description, administratorRole, actorId));
        replacePermissions(role.id(), permissions);
    }

    private void replacePermissions(UUID roleId, Set<AccessPermission> permissions) {
        rolePermissions.deleteAllInBatch(rolePermissions.findByRoleId(roleId));
        rolePermissions.saveAll(permissions.stream()
                .map(permission -> AccessRolePermission.create(roleId, permission))
                .toList());
    }

    private RoleSummary toSummary(AccessRole role) {
        Set<AccessPermission> assigned = permissionsForRole(role.id());
        List<AccessPermission> permissions = Arrays.stream(AccessPermission.values())
                .filter(assigned::contains)
                .toList();
        return new RoleSummary(
                role.id(),
                role.name(),
                role.description(),
                role.administratorRole(),
                permissions,
                role.version());
    }

    public record SaveRoleCommand(
            @NotBlank @Size(max = 120) String name,
            @NotNull @Size(max = 1000) String description,
            @NotNull Set<AccessPermission> permissions,
            @NotNull Integer version) {}

    public record RoleSummary(
            UUID id,
            String name,
            String description,
            boolean administratorRole,
            List<AccessPermission> permissions,
            int version) {}
}
