package com.ukgqtm.app.security;

import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.identity.api.AuthenticatedUserResolver;
import com.ukgqtm.identity.api.AuthenticationDeniedException;
import com.ukgqtm.identity.api.EntraTokenClaims;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;

public class ApplicationUserJwtAuthenticationConverter
        implements Converter<Jwt, ApplicationUserAuthenticationToken> {
    private final AuthenticatedUserResolver users;

    public ApplicationUserJwtAuthenticationConverter(AuthenticatedUserResolver users) {
        this.users = users;
    }

    @Override
    public ApplicationUserAuthenticationToken convert(Jwt source) {
        try {
            AuthenticatedUser user = users.resolve(new EntraTokenClaims(
                    source.getClaimAsString("tid"),
                    source.getClaimAsString("oid"),
                    source.getClaimAsString("email"),
                    source.getClaimAsString("preferred_username"),
                    source.getClaimAsString("name"),
                    MDC.get("correlationId")));
            return new ApplicationUserAuthenticationToken(user, source, authorities(user));
        } catch (AuthenticationDeniedException ex) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("access_denied", ex.getMessage(), null), ex.getMessage(), ex);
        }
    }

    private static List<GrantedAuthority> authorities(AuthenticatedUser user) {
        List<GrantedAuthority> values = new ArrayList<>();
        values.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (user.globalAdministrator()) {
            values.add(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"));
        }
        return values;
    }
}
