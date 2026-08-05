package com.ukgqtm.app.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class AuthSecurityProperties {
    private boolean oauth2ResourceServerEnabled;
    private boolean localAuthEnabled;
    private boolean production;
    private Entra entra = new Entra();
    private BootstrapAdmin bootstrapAdmin = new BootstrapAdmin();
    private DevelopmentAdmin developmentAdmin = new DevelopmentAdmin();
    private LocalAdmin localAdmin = new LocalAdmin();

    public boolean isOauth2ResourceServerEnabled() {
        return oauth2ResourceServerEnabled;
    }

    public void setOauth2ResourceServerEnabled(boolean oauth2ResourceServerEnabled) {
        this.oauth2ResourceServerEnabled = oauth2ResourceServerEnabled;
    }

    public boolean isLocalAuthEnabled() {
        return localAuthEnabled;
    }

    public void setLocalAuthEnabled(boolean localAuthEnabled) {
        this.localAuthEnabled = localAuthEnabled;
    }

    public boolean isProduction() {
        return production;
    }

    public void setProduction(boolean production) {
        this.production = production;
    }

    public Entra getEntra() {
        return entra;
    }

    public void setEntra(Entra entra) {
        this.entra = entra;
    }

    public BootstrapAdmin getBootstrapAdmin() {
        return bootstrapAdmin;
    }

    public void setBootstrapAdmin(BootstrapAdmin bootstrapAdmin) {
        this.bootstrapAdmin = bootstrapAdmin;
    }

    public LocalAdmin getLocalAdmin() {
        return localAdmin;
    }

    public DevelopmentAdmin getDevelopmentAdmin() {
        return developmentAdmin;
    }

    public void setDevelopmentAdmin(DevelopmentAdmin developmentAdmin) {
        this.developmentAdmin = developmentAdmin;
    }

    public void setLocalAdmin(LocalAdmin localAdmin) {
        this.localAdmin = localAdmin;
    }

    public static class Entra {
        private List<String> allowedTenants = new ArrayList<>();
        private List<String> audiences = new ArrayList<>();
        private String issuerHost = "https://login.microsoftonline.com";
        private String jwkSetUri;

        public List<String> getAllowedTenants() {
            return allowedTenants;
        }

        public void setAllowedTenants(List<String> allowedTenants) {
            this.allowedTenants = allowedTenants;
        }

        public List<String> getAudiences() {
            return audiences;
        }

        public void setAudiences(List<String> audiences) {
            this.audiences = audiences;
        }

        public String getIssuerHost() {
            return issuerHost;
        }

        public void setIssuerHost(String issuerHost) {
            this.issuerHost = issuerHost;
        }

        public String getJwkSetUri() {
            return jwkSetUri;
        }

        public void setJwkSetUri(String jwkSetUri) {
            this.jwkSetUri = jwkSetUri;
        }
    }

    public static class BootstrapAdmin {
        private boolean enabled;
        private String normalizedEmail;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getNormalizedEmail() {
            return normalizedEmail;
        }

        public void setNormalizedEmail(String normalizedEmail) {
            this.normalizedEmail = normalizedEmail;
        }
    }

    public static class LocalAdmin {
        private String username;
        private String password;
        private String passwordHash;
        private String tenantId = "dev-tenant";
        private String objectId = "local-admin";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPasswordHash() {
            return passwordHash;
        }

        public void setPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getObjectId() {
            return objectId;
        }

        public void setObjectId(String objectId) {
            this.objectId = objectId;
        }
    }

    public static class DevelopmentAdmin {
        private boolean enabled;
        private String email;
        private String password;
        private String passwordHash;
        private String firstName = "Development";
        private String lastName = "Administrator";
        private String tenantId = "dev-tenant";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getPasswordHash() { return passwordHash; }
        public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    }
}
