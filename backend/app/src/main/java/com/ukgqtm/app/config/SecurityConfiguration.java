package com.ukgqtm.app.config;

import com.ukgqtm.app.security.ApplicationUserJwtAuthenticationConverter;
import com.ukgqtm.app.security.EntraJwtValidator;
import com.ukgqtm.identity.api.AuthenticatedUserResolver;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(AuthSecurityProperties.class)
public class SecurityConfiguration {
    private final AuthSecurityProperties securityProperties;

    SecurityConfiguration(AuthSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectProvider<AuthenticatedUserResolver> authenticatedUsers,
            SecurityContextRepository securityContextRepository) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.securityContext(securityContext -> securityContext.securityContextRepository(securityContextRepository));
        http.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) -> writeProblem(
                        response,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Unauthorized",
                        "Authentication is required."))
                .accessDeniedHandler((request, response, exception) -> writeProblem(
                        response,
                        HttpServletResponse.SC_FORBIDDEN,
                        "Forbidden",
                        "The requested resource is not available.")));
        http.authorizeHttpRequests(authorize -> {
            authorize.requestMatchers(
                    "/",
                    "/index.html",
                    "/assets/**",
                    "/favicon.svg",
                    "/api/v1/health",
                    "/api/v1/ready",
                    "/actuator/health/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html").permitAll();
            if (securityProperties.isLocalAuthEnabled()) {
                authorize.requestMatchers("/api/v1/auth/local-login").permitAll();
            }
            if (securityProperties.isOauth2ResourceServerEnabled() || securityProperties.isLocalAuthEnabled()) {
                authorize.anyRequest().authenticated();
            } else {
                authorize.anyRequest().denyAll();
            }
        });

        if (securityProperties.isOauth2ResourceServerEnabled()) {
            AuthenticatedUserResolver resolver = authenticatedUsers.getIfAvailable(() -> {
                throw new IllegalStateException("AuthenticatedUserResolver is required when OAuth2 is enabled.");
            });
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(
                    jwt -> jwt.jwtAuthenticationConverter(new ApplicationUserJwtAuthenticationConverter(resolver))));
        }

        return http.build();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnProperty(name = "app.security.oauth2-resource-server-enabled", havingValue = "true")
    JwtDecoder jwtDecoder(AuthSecurityProperties properties) {
        AuthSecurityProperties.Entra entra = properties.getEntra();
        if (isBlank(entra.getJwkSetUri())) {
            throw new IllegalStateException("ENTRA_JWK_SET_URI is required when OAuth2 resource server is enabled.");
        }
        if (entra.getAllowedTenants().isEmpty()) {
            throw new IllegalStateException("ENTRA_ALLOWED_TENANTS is required when OAuth2 resource server is enabled.");
        }
        if (entra.getAudiences().isEmpty()) {
            throw new IllegalStateException("ENTRA_AUDIENCES is required when OAuth2 resource server is enabled.");
        }

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(entra.getJwkSetUri()).build();
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(JwtValidators.createDefault());
        validators.add(new EntraJwtValidator(entra));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return decoder;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void writeProblem(HttpServletResponse response, int status, String title, String detail)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter()
                .write("""
                        {"type":"https://ukgqtm.local/problems/%s","title":"%s","status":%d,"detail":"%s"}"""
                        .formatted(title.toLowerCase(), title, status, detail));
    }
}
