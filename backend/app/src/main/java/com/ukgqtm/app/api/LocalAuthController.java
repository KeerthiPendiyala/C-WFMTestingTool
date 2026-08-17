package com.ukgqtm.app.api;

import com.ukgqtm.app.security.ApplicationUserAuthenticationToken;
import com.ukgqtm.app.security.AuthorizationPolicyService;
import com.ukgqtm.app.security.LocalAdminAuthenticationService;
import com.ukgqtm.identity.api.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnProperty(name = "app.security.local-auth-enabled", havingValue = "true")
public class LocalAuthController {
    private final LocalAdminAuthenticationService localUsers;
    private final AuthorizationPolicyService authorization;
    private final SecurityContextRepository securityContextRepository;

    public LocalAuthController(
            LocalAdminAuthenticationService localAdmins,
            AuthorizationPolicyService authorization,
            SecurityContextRepository securityContextRepository) {
        this.localUsers = localAdmins;
        this.authorization = authorization;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping(path = "/local-login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthController.AuthSessionResponse> login(
            @Valid @RequestBody LocalLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        AuthenticatedUser user = localUsers.authenticate(
                request.username(), request.password(), httpRequest.getHeader(ApiHeaders.CORRELATION_ID));
        var authentication = new ApplicationUserAuthenticationToken(user, null, java.util.List.of());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        return ResponseEntity.ok(AuthController.AuthSessionResponse.from(
                user,
                authorization.assignedRoleName(user),
                authorization.globalCapabilities(user).stream().map(Enum::name).sorted().toList(),
                authorization.effectivePermissions(user).stream().map(Enum::name).sorted().toList(),
                authorization.assignedProjectPermissions(user)));
    }

    public record LocalLoginRequest(
            @NotBlank(message = "Username is required.") String username,
            @NotBlank(message = "Password is required.") String password) {}
}
