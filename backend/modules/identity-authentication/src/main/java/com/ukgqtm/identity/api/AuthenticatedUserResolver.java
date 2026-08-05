package com.ukgqtm.identity.api;

public interface AuthenticatedUserResolver {
    AuthenticatedUser resolve(EntraTokenClaims claims);

    void observeLogout(AuthenticatedUser user, String correlationId);
}
