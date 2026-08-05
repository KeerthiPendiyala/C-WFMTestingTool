package com.ukgqtm.app.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.env.Environment;

class LocalAuthProductionGuardTest {
    @Test
    void rejectsLocalAuthInProduction() {
        AuthSecurityProperties properties = new AuthSecurityProperties();
        properties.setLocalAuthEnabled(true);
        properties.setProduction(true);

        assertThatThrownBy(() -> new LocalAuthProductionGuard(properties, mock(Environment.class))
                        .run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("forbidden in production");
    }

    @Test
    void rejectsLocalAuthInProductionProfile() {
        AuthSecurityProperties properties = new AuthSecurityProperties();
        properties.setLocalAuthEnabled(true);
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[] {"production"});

        assertThatThrownBy(() -> new LocalAuthProductionGuard(properties, environment)
                        .run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("forbidden in production");
    }

    @Test
    void allowsLocalAuthOutsideProduction() {
        AuthSecurityProperties properties = new AuthSecurityProperties();
        properties.setLocalAuthEnabled(true);
        properties.getLocalAdmin().setUsername("avery.admin@example.test");
        properties.getLocalAdmin().setPassword(UUID.randomUUID().toString());
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[] {"dev"});

        assertThatCode(() -> new LocalAuthProductionGuard(properties, environment)
                        .run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsLocalAuthWithoutConfiguredCredential() {
        AuthSecurityProperties properties = new AuthSecurityProperties();
        properties.setLocalAuthEnabled(true);
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[] {"dev"});

        assertThatThrownBy(() -> new LocalAuthProductionGuard(properties, environment)
                        .run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LOCAL_ADMIN_USERNAME");
    }
}
