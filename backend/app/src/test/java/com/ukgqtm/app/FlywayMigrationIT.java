package com.ukgqtm.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationIT {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ukgqtm")
            .withUsername("ukgqtm")
            .withPassword("ukgqtm");

    @Test
    void productionMigrationsApplyCleanlyToPostgreSql() {
        Flyway flyway = flyway("classpath:db/migration");

        flyway.clean();
        assertThat(flyway.migrate().migrationsExecuted).isGreaterThanOrEqualTo(2);
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
    }

    @Test
    void developmentSeedMigrationsApplyOnlyWhenDevLocationIsIncluded() {
        Flyway flyway = flyway("classpath:db/migration", "classpath:db/dev-migration");

        flyway.clean();
        assertThat(flyway.migrate().migrationsExecuted).isGreaterThanOrEqualTo(3);
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
    }

    private Flyway flyway(String... locations) {
        return Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations(locations)
                .cleanDisabled(false)
                .load();
    }
}
