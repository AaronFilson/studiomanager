package io.github.aaronfilson.studiomanager;

import java.time.Duration;

import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * A real PostgreSQL 18, with the same two logins as local development (compose.yaml), and
 * Azurite for photo storage.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /** Azurite's fixed development key, which is public. */
    private static final String AZURITE_KEY =
            "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";

    /**
     * A test with its own properties (TelemetryTests) gets its own context and so its own database.
     * Starting one while the machine is busy has taken longer than Testcontainers' default minute.
     */
    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(3);

    @Bean
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"))
                .withCopyFileToContainer(MountableFile.forHostPath("dev/postgres-init/00-app-role.sql"),
                        "/docker-entrypoint-initdb.d/00-app-role.sql")
                .withStartupTimeout(STARTUP_TIMEOUT);
    }

    @Bean
    GenericContainer<?> azurite() {
        return new GenericContainer<>(DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:3.35.0"))
                .withCommand("azurite-blob", "--blobHost", "0.0.0.0", "--skipApiVersionCheck", "--inMemoryPersistence")
                .withExposedPorts(10000)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(STARTUP_TIMEOUT));
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

    /** Creates the photo container up front, rather than racing the API's own background setup. */
    @Bean
    DynamicPropertyRegistrar blobStorage(GenericContainer<?> azurite) {
        String connectionString = "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=%s;BlobEndpoint=http://%s:%d/devstoreaccount1;"
                .formatted(AZURITE_KEY, azurite.getHost(), azurite.getMappedPort(10000));
        new BlobServiceClientBuilder().connectionString(connectionString).buildClient()
                .getBlobContainerClient("piece-photos").createIfNotExists();
        return registry -> registry.add("studiomanager.storage.connection-string", () -> connectionString);
    }

}
