package com.ukgqtm.app.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ukgqtm.audit.repository.AuditEventRepository;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.project.domain.AccessPermission;
import com.ukgqtm.project.domain.AccessRole;
import com.ukgqtm.project.domain.AccessRolePermission;
import com.ukgqtm.project.repository.AccessRolePermissionRepository;
import com.ukgqtm.project.repository.AccessRoleRepository;
import com.ukgqtm.project.repository.UserAccessRoleAssignmentRepository;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoleApplicationServiceTest {
    private final AccessRoleRepository roles = mock(AccessRoleRepository.class);
    private final AccessRolePermissionRepository permissions = mock(AccessRolePermissionRepository.class);
    private final UserAccessRoleAssignmentRepository assignments = mock(UserAccessRoleAssignmentRepository.class);
    private final AuditEventRepository auditEvents = mock(AuditEventRepository.class);
    private final RoleApplicationService service =
            new RoleApplicationService(roles, permissions, assignments, auditEvents);

    @Test
    void administratorCanCreateAndEditRolePermissions() {
        AuthenticatedUser actor = administrator();
        List<AccessRolePermission> storedPermissions = new ArrayList<>();
        when(roles.findByTenantIdAndNameIgnoreCase(any(), any())).thenReturn(Optional.empty());
        when(roles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(permissions.findByRoleId(any())).thenAnswer(invocation -> List.copyOf(storedPermissions));
        when(permissions.saveAll(any())).thenAnswer(invocation -> {
            List<AccessRolePermission> values = new ArrayList<>();
            ((Iterable<AccessRolePermission>) invocation.getArgument(0)).forEach(values::add);
            storedPermissions.clear();
            storedPermissions.addAll(values);
            return values;
        });

        var created = service.createRole(
                actor,
                new RoleApplicationService.SaveRoleCommand(
                        "Release Manager",
                        "Coordinates releases",
                        EnumSet.of(AccessPermission.VIEW, AccessPermission.EXECUTE),
                        0),
                "corr-create");

        assertThat(created.name()).isEqualTo("Release Manager");
        assertThat(created.permissions()).containsExactly(AccessPermission.VIEW, AccessPermission.EXECUTE);

        AccessRole persisted = AccessRole.create(
                actor.tenantId(), created.name(), created.description(), false, actor.userId());
        when(roles.findByTenantIdAndId(actor.tenantId(), created.id())).thenReturn(Optional.of(persisted));

        var updated = service.updateRole(
                actor,
                created.id(),
                new RoleApplicationService.SaveRoleCommand(
                        "Release Manager",
                        "Coordinates and approves releases",
                        EnumSet.of(AccessPermission.VIEW, AccessPermission.EDIT, AccessPermission.APPROVE_REQUIREMENTS),
                        0),
                "corr-update");

        assertThat(updated.description()).isEqualTo("Coordinates and approves releases");
        assertThat(updated.permissions()).containsExactly(
                AccessPermission.VIEW, AccessPermission.EDIT, AccessPermission.APPROVE_REQUIREMENTS);
    }

    private static AuthenticatedUser administrator() {
        return new AuthenticatedUser(
                UUID.randomUUID(),
                "tenant-1",
                "admin-object",
                "Avery",
                "Administrator",
                "avery@example.test",
                true);
    }
}
