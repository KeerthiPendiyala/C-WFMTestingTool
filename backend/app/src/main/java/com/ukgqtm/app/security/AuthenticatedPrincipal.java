package com.ukgqtm.app.security;

import com.ukgqtm.identity.api.AuthenticatedUser;
import org.springframework.security.core.Authentication;

public final class AuthenticatedPrincipal {
    private AuthenticatedPrincipal() {}

    public static AuthenticatedUser require(Authentication authentication) {
        if (authentication instanceof ApplicationUserAuthenticationToken token) {
            return token.getPrincipal();
        }
        throw new IllegalStateException("Authenticated user principal was not resolved.");
    }
}
