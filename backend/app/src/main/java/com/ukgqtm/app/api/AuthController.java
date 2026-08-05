package com.ukgqtm.app.api;

import com.ukgqtm.app.security.AuthenticatedPrincipal;
import com.ukgqtm.app.security.AuthorizationPolicyService;
import com.ukgqtm.app.security.LocalAdminAuthenticationService;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.identity.api.AuthenticatedUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {
    private final ObjectProvider<AuthenticatedUserResolver> authenticatedUsers;
    private final ObjectProvider<LocalAdminAuthenticationService> localAdmins;
    private final AuthorizationPolicyService authorization;

    public AuthController(
            ObjectProvider<AuthenticatedUserResolver> authenticatedUsers,
            ObjectProvider<LocalAdminAuthenticationService> localAdmins,
            AuthorizationPolicyService authorization) {
        this.authenticatedUsers = authenticatedUsers;
        this.localAdmins = localAdmins;
        this.authorization = authorization;
    }

    @GetMapping("/me")
    public ResponseEntity<AuthSessionResponse> me(Authentication authentication) {
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        return ResponseEntity.ok(AuthSessionResponse.from(
                user, authorization.globalCapabilities(user).stream().map(Enum::name).sorted().toList()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication, HttpServletRequest request) {
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        AuthenticatedUserResolver entraResolver = authenticatedUsers.getIfAvailable();
        LocalAdminAuthenticationService localAdminService = localAdmins.getIfAvailable();
        if (localAdminService != null && localAdminService.isLocalPrincipal(user)) {
            localAdminService.observeLogout(user, request.getHeader(ApiHeaders.CORRELATION_ID));
        } else if (entraResolver != null) {
            entraResolver.observeLogout(user, request.getHeader(ApiHeaders.CORRELATION_ID));
        }
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    public record AuthSessionResponse(
            String userId,
            String tenantId,
            String objectId,
            String firstName,
            String lastName,
            String contactEmail,
            boolean globalAdministrator,
            String principalKey,
            List<String> globalCapabilities) {
        static AuthSessionResponse from(AuthenticatedUser user, List<String> globalCapabilities) {
            return new AuthSessionResponse(
                    user.userId().toString(),
                    user.tenantId(),
                    user.objectId(),
                    user.firstName(),
                    user.lastName(),
                    user.contactEmail(),
                    user.globalAdministrator(),
                    user.immutablePrincipalKey(),
                    globalCapabilities);
        }
    }
}
