package com.ukgqtm.app.api;

import com.ukgqtm.app.role.RoleApplicationService;
import com.ukgqtm.app.role.RoleApplicationService.RoleSummary;
import com.ukgqtm.app.role.RoleApplicationService.SaveRoleCommand;
import com.ukgqtm.app.security.AuthenticatedPrincipal;
import com.ukgqtm.identity.api.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/roles", produces = MediaType.APPLICATION_JSON_VALUE)
public class RoleController {
    private final RoleApplicationService roles;

    public RoleController(RoleApplicationService roles) {
        this.roles = roles;
    }

    @GetMapping
    public RoleListResponse list(Authentication authentication) {
        return new RoleListResponse(roles.listRoles(requireAdministrator(authentication)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RoleSummary> create(
            Authentication authentication,
            @Valid @RequestBody SaveRoleCommand command,
            HttpServletRequest request) {
        RoleSummary created = roles.createRole(
                requireAdministrator(authentication), command, request.getHeader(ApiHeaders.CORRELATION_ID));
        return ResponseEntity.created(URI.create("/api/v1/roles/" + created.id())).body(created);
    }

    @PatchMapping(path = "/{roleId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RoleSummary update(
            Authentication authentication,
            @PathVariable("roleId") UUID roleId,
            @Valid @RequestBody SaveRoleCommand command,
            HttpServletRequest request) {
        return roles.updateRole(
                requireAdministrator(authentication), roleId, command, request.getHeader(ApiHeaders.CORRELATION_ID));
    }

    private static AuthenticatedUser requireAdministrator(Authentication authentication) {
        AuthenticatedUser actor = AuthenticatedPrincipal.require(authentication);
        if (!actor.globalAdministrator()) {
            throw new AccessDeniedException("The requested resource is not available.");
        }
        return actor;
    }

    public record RoleListResponse(List<RoleSummary> roles) {}
}
