package io.github.aaronfilson.studiomanager;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.storage.blob.BlobServiceClientBuilder;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Starts the application under the {@code migrate} and {@code sweep} profiles ({@code
 * spring.main.web-application-type: none}), matching the Azure Container App jobs ({@code
 * caj-*-migrate} and {@code caj-*-sweep}). Catches any non-web bean wiring failure, Flyway callback
 * failure, or sweep failure before deploy-dev runs.
 */
@EnabledIf(
    value = "io.github.aaronfilson.studiomanager.DockerAvailability#dockerAvailableOrCi",
    disabledReason = "Integration tests need Docker")
class JobProfilesStartTests {

  private static final String AZURITE_KEY =
      "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";
  private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(3);

  @Test
  void migrateAndSweepNonWebJobsRunAndExitZero() {
    try (PostgreSQLContainer postgres =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"))
                .withCopyFileToContainer(
                    MountableFile.forHostPath("dev/postgres-init/00-app-role.sql"),
                    "/docker-entrypoint-initdb.d/00-app-role.sql")
                .withStartupTimeout(STARTUP_TIMEOUT);
        GenericContainer<?> azurite =
            new GenericContainer<>(
                    DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:3.35.0"))
                .withCommand(
                    "azurite-blob",
                    "--blobHost",
                    "0.0.0.0",
                    "--skipApiVersionCheck",
                    "--inMemoryPersistence")
                .withExposedPorts(10000)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(STARTUP_TIMEOUT))) {
      postgres.start();
      azurite.start();

      String storageConnectionString =
          "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=%s;BlobEndpoint=http://%s:%d/devstoreaccount1;"
              .formatted(AZURITE_KEY, azurite.getHost(), azurite.getMappedPort(10000));
      new BlobServiceClientBuilder()
          .connectionString(storageConnectionString)
          .buildClient()
          .getBlobContainerClient("piece-photos")
          .createIfNotExists();

      ConfigurableApplicationContext migrateContext =
          new SpringApplicationBuilder(StudioManagerApplication.class)
              .profiles("test", "migrate")
              .run(
                  "--spring.datasource.url=" + postgres.getJdbcUrl(),
                  "--spring.datasource.username=" + postgres.getUsername(),
                  "--spring.datasource.password=" + postgres.getPassword(),
                  "--spring.flyway.user=" + postgres.getUsername(),
                  "--spring.flyway.password=" + postgres.getPassword(),
                  "--spring.flyway.placeholders.apiPrincipal=studiomanager_app",
                  "--studiomanager.storage.connection-string=" + storageConnectionString,
                  "--studiomanager.mail.sender=log");
      assertThat(SpringApplication.exit(migrateContext)).isZero();

      ConfigurableApplicationContext sweepContext =
          new SpringApplicationBuilder(StudioManagerApplication.class)
              .profiles("test", "sweep")
              .run(
                  "--spring.datasource.url=" + postgres.getJdbcUrl(),
                  "--spring.datasource.username=studiomanager_app",
                  "--spring.datasource.password=studiomanager_app",
                  "--spring.flyway.enabled=false",
                  "--studiomanager.storage.connection-string=" + storageConnectionString,
                  "--studiomanager.mail.sender=log");
      assertThat(SpringApplication.exit(sweepContext)).isZero();
    }
  }
}
