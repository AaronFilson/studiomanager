package io.github.aaronfilson.studiomanager;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Measures how long the application takes to start, for the before-and-after numbers in ADR 21.
 *
 * <p>Off unless asked for: {@code bash scripts/integration.sh test --tests '*StartupTimeTests'
 * -PstartupRuns=12}. The first start migrates the database and loads every class, so it's printed
 * on its own; the later starts run in the same, warm JVM and are printed with their median.
 */
@EnabledIfSystemProperty(named = "studiomanager.startup-runs", matches = "[2-9]|[1-9][0-9]")
class StartupTimeTests {

  @Test
  void printsStartupTimes() {
    int runs = Integer.parseInt(System.getProperty("studiomanager.startup-runs", "0"));
    try (PostgreSQLContainer postgres =
        new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"))
            .withCopyFileToContainer(
                MountableFile.forHostPath("dev/postgres-init/00-app-role.sql"),
                "/docker-entrypoint-initdb.d/00-app-role.sql")
            .withStartupTimeout(Duration.ofMinutes(3))) {
      postgres.start();
      // As arguments, which outrank application.yaml (builder properties are only defaults)
      String[] arguments =
          Map.of(
                  "spring.datasource.url", postgres.getJdbcUrl(),
                  "spring.datasource.username", "studiomanager_app",
                  "spring.datasource.password", "studiomanager_app",
                  "spring.flyway.user", postgres.getUsername(),
                  "spring.flyway.password", postgres.getPassword(),
                  "server.port", "0",
                  "logging.level.root", "OFF",
                  "spring.main.banner-mode", "off",
                  "spring.jpa.properties.hibernate.generate_statistics", "false")
              .entrySet()
              .stream()
              .map(setting -> "--%s=%s".formatted(setting.getKey(), setting.getValue()))
              .toArray(String[]::new);
      List<Long> millis = new ArrayList<>();
      for (int run = 0; run < runs; run++) {
        long started = System.nanoTime();
        try (ConfigurableApplicationContext context =
            new SpringApplicationBuilder(StudioManagerApplication.class)
                .profiles("test")
                .run(arguments)) {
          long elapsed = Duration.ofNanos(System.nanoTime() - started).toMillis();
          assertThat(context.isActive()).isTrue();
          if (run == 0) {
            System.out.println("STARTUP first, cold, with migrations (ms): " + elapsed);
          } else {
            millis.add(elapsed);
          }
        }
      }
      List<Long> sorted = millis.stream().sorted().toList();
      System.out.println("STARTUP runs (ms): " + millis);
      System.out.println("STARTUP median (ms): " + sorted.get(sorted.size() / 2));
    }
  }
}
