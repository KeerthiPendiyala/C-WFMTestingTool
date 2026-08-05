package com.ukgqtm.app.security;

import com.ukgqtm.app.config.AuthSecurityProperties;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class EntraJwtValidator implements OAuth2TokenValidator<Jwt> {
    private final Set<String> allowedTenants;
    private final Set<String> audiences;
    private final String issuerHost;

    public EntraJwtValidator(AuthSecurityProperties.Entra entra) {
        this.allowedTenants = normalizeSet(entra.getAllowedTenants());
        this.audiences = normalizeSet(entra.getAudiences());
        this.issuerHost = stripTrailingSlash(entra.getIssuerHost());
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String tenantId = token.getClaimAsString("tid");
        String objectId = token.getClaimAsString("oid");
        if (isBlank(tenantId)) {
            return failure("invalid_token", "Missing tid claim.");
        }
        if (isBlank(objectId)) {
            return failure("invalid_token", "Missing oid claim.");
        }
        if (allowedTenants.isEmpty() || !allowedTenants.contains(normalize(tenantId))) {
            return failure("invalid_token", "Token tenant is not allowed.");
        }
        if (audiences.isEmpty()
                || token.getAudience().stream().map(EntraJwtValidator::normalize).noneMatch(audiences::contains)) {
            return failure("invalid_token", "Token audience is not allowed.");
        }
        String expectedIssuer = issuerHost + "/" + tenantId + "/v2.0";
        if (token.getIssuer() == null || !expectedIssuer.equals(stripTrailingSlash(token.getIssuer().toString()))) {
            return failure("invalid_token", "Token issuer is not allowed.");
        }
        return OAuth2TokenValidatorResult.success();
    }

    private static OAuth2TokenValidatorResult failure(String code, String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(code, description, null));
    }

    private static Set<String> normalizeSet(Iterable<String> values) {
        return values == null
                ? Set.of()
                : java.util.stream.StreamSupport.stream(values.spliterator(), false)
                        .filter(value -> !isBlank(value))
                        .map(EntraJwtValidator::normalize)
                        .collect(Collectors.toUnmodifiableSet());
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String stripTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        String stripped = value.trim();
        while (stripped.endsWith("/")) {
            stripped = stripped.substring(0, stripped.length() - 1);
        }
        return stripped;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
