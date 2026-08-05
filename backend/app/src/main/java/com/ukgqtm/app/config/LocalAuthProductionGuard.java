package com.ukgqtm.app.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class LocalAuthProductionGuard implements ApplicationRunner {
    private final AuthSecurityProperties securityProperties;
    private final Environment environment;

    public LocalAuthProductionGuard(AuthSecurityProperties securityProperties, Environment environment) {
        this.securityProperties = securityProperties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!securityProperties.isLocalAuthEnabled()) {
            return;
        }
        if (isProductionRuntime()) {
            throw new IllegalStateException("Local password auth is forbidden in production.");
        }
        if (securityProperties.getDevelopmentAdmin().isEnabled()) {
            return;
        }
        AuthSecurityProperties.LocalAdmin localAdmin = securityProperties.getLocalAdmin();
        if (isBlank(localAdmin.getUsername())) {
            throw new IllegalStateException("LOCAL_ADMIN_USERNAME is required when local auth is enabled.");
        }
        if (isBlank(localAdmin.getPassword()) && isBlank(localAdmin.getPasswordHash())) {
            throw new IllegalStateException(
                    "LOCAL_ADMIN_PASSWORD or LOCAL_ADMIN_PASSWORD_HASH is required when local auth is enabled.");
        }
        if (isBlank(localAdmin.getTenantId()) || isBlank(localAdmin.getObjectId())) {
            throw new IllegalStateException(
                    "LOCAL_ADMIN_TENANT_ID and LOCAL_ADMIN_OBJECT_ID are required when local auth is enabled.");
        }
    }

    private boolean isProductionRuntime() {
        if (securityProperties.isProduction()) {
            return true;
        }
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
