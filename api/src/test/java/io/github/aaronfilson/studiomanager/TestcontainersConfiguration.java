package io.github.aaronfilson.studiomanager;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/** A real PostgreSQL 18, with the same two logins as local development (compose.yaml). */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"))
                .withCopyFileToContainer(MountableFile.forHostPath("dev/postgres-init/00-app-role.sql"),
                        "/docker-entrypoint-initdb.d/00-app-role.sql");
    }

    /** Flyway as the container's superuser, which owns the schema; the application as studiomanager_app. */
    @Bean
    DynamicPropertyRegistrar databaseLogins(PostgreSQLContainer postgres) {
        return registry -> {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", () -> "studiomanager_app");
            registry.add("spring.datasource.password", () -> "studiomanager_app");
            registry.add("spring.flyway.user", postgres::getUsername);
            registry.add("spring.flyway.password", postgres::getPassword);
        };
    }

}
