package com.ukgqtm.app.api;

import com.ukgqtm.app.security.AuthenticatedPrincipal;
import com.ukgqtm.app.security.AuthorizationPolicy;
import com.ukgqtm.app.security.AuthorizationPolicyService;
import com.ukgqtm.app.user.UserAccessApplicationService;
import com.ukgqtm.app.user.UserAccessApplicationService.CreateUserCommand;
import com.ukgqtm.app.user.UserAccessApplicationService.UpdateUserCommand;
import com.ukgqtm.app.user.UserAccessApplicationService.UserSummary;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserAccessController {
    private final UserAccessApplicationService users;
    private final AuthorizationPolicyService authorization;

    public UserAccessController(UserAccessApplicationService users, AuthorizationPolicyService authorization) {
        this.users = users;
        this.authorization = authorization;
    }

    @GetMapping
    public UserListResponse list(Authentication authentication, HttpServletRequest request) {
        AuthenticatedUser actor = requireAdministrator(authentication, request);
        return new UserListResponse(users.listUsers(actor));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserSummary> create(
            Authentication authentication,
            @Valid @RequestBody CreateUserCommand command,
            HttpServletRequest request) {
        AuthenticatedUser actor = requireAdministrator(authentication, request);
        UserSummary created = users.createUser(actor, command, request.getHeader(ApiHeaders.CORRELATION_ID));
        return ResponseEntity.created(URI.create("/api/v1/users/" + created.id())).body(created);
    }

    @PatchMapping(path = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UserSummary update(
            Authentication authentication,
            @PathVariable("userId") UUID userId,
            @Valid @RequestBody UpdateUserCommand command,
            HttpServletRequest request) {
        AuthenticatedUser actor = requireAdministrator(authentication, request);
        return users.updateUser(actor, userId, command, request.getHeader(ApiHeaders.CORRELATION_ID));
    }

    private AuthenticatedUser requireAdministrator(Authentication authentication, HttpServletRequest request) {
        AuthenticatedUser actor = AuthenticatedPrincipal.require(authentication);
        if (actor.globalAdministrator()) {
            return actor;
        }
        authorization.require(
                authentication,
                AuthorizationPolicy.USER_ACCESS_MANAGE,
                null,
                "APPLICATION_USER",
                "access-management",
                request.getHeader(ApiHeaders.CORRELATION_ID));
        throw new AccessDeniedException("The requested resource is not available.");
    }

    public record UserListResponse(List<UserSummary> users) {}
}
