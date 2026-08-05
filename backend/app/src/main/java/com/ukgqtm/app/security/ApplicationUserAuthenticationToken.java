package com.ukgqtm.app.security;

import com.ukgqtm.identity.api.AuthenticatedUser;
import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class ApplicationUserAuthenticationToken extends AbstractAuthenticationToken {
    private final AuthenticatedUser principal;
    private final Jwt token;

    public ApplicationUserAuthenticationToken(
            AuthenticatedUser principal, Jwt token, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.token = token;
        setAuthenticated(true);
    }

    @Override
    public AuthenticatedUser getPrincipal() {
        return principal;
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    public Jwt getToken() {
        return token;
    }

    @Override
    public String getName() {
        return principal.immutablePrincipalKey();
    }
}
