package com.ukgqtm.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.ukgqtm.app.config.AuthSecurityProperties;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;

class EntraJwtValidatorTest {
    private static final String SECRET = "01234567890123456789012345678901";
    private static final String TENANT = "tenant-1";
    private static final String AUDIENCE = "api://ukg-qtm";

    @Test
    void acceptsSignedTokenWithExpectedIssuerAudienceTenantAndObjectId() throws Exception {
        var jwt = decoder().decode(signedToken(TENANT, "object-1", AUDIENCE, "https://login.microsoftonline.com/tenant-1/v2.0"));

        assertThat(jwt.getClaimAsString("tid")).isEqualTo(TENANT);
        assertThat(jwt.getClaimAsString("oid")).isEqualTo("object-1");
    }

    @Test
    void rejectsWrongTenant() throws Exception {
        assertThatThrownBy(() -> decoder().decode(
                        signedToken("other-tenant", "object-1", AUDIENCE, "https://login.microsoftonline.com/other-tenant/v2.0")))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Token tenant is not allowed");
    }

    @Test
    void rejectsWrongAudience() throws Exception {
        assertThatThrownBy(() -> decoder().decode(
                        signedToken(TENANT, "object-1", "api://other", "https://login.microsoftonline.com/tenant-1/v2.0")))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Token audience is not allowed");
    }

    @Test
    void rejectsWrongIssuer() throws Exception {
        assertThatThrownBy(() -> decoder().decode(
                        signedToken(TENANT, "object-1", AUDIENCE, "https://sts.windows.net/tenant-1/")))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Token issuer is not allowed");
    }

    @Test
    void rejectsMissingObjectId() throws Exception {
        assertThatThrownBy(() -> decoder().decode(signedToken(TENANT, null, AUDIENCE, "https://login.microsoftonline.com/tenant-1/v2.0")))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Missing oid claim");
    }

    private static NimbusJwtDecoder decoder() {
        AuthSecurityProperties.Entra entra = new AuthSecurityProperties.Entra();
        entra.setAllowedTenants(List.of(TENANT));
        entra.setAudiences(List.of(AUDIENCE));
        var decoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), new EntraJwtValidator(entra)));
        return decoder;
    }

    private static String signedToken(String tenant, String objectId, String audience, String issuer) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .issueTime(Date.from(now.minusSeconds(30)))
                .notBeforeTime(Date.from(now.minusSeconds(30)))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .claim("tid", tenant)
                .claim("email", "avery.admin@example.test")
                .claim("preferred_username", "avery.admin@example.test")
                .claim("name", "Avery Administrator");
        if (objectId != null) {
            claims.claim("oid", objectId);
        }
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(), claims.build());
        jwt.sign(new MACSigner(SECRET));
        return jwt.serialize();
    }
}
