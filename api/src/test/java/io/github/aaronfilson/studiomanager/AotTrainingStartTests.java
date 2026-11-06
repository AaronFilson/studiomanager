package io.github.aaronfilson.studiomanager;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;

/**
 * The Docker build starts the application once with the {@code aot-training} profile and no
 * database, to record the JVM's AOT cache (see api/Dockerfile). If that start fails, so does the
 * image build, and nothing deploys. This test makes the same start, with nothing listening on the
 * database's port, so a bean that needs the database while the context starts fails here rather
 * than in CI's image build.
 *
 * <p>The build passes {@code -Dspring.context.exit=onRefresh}, which ends the JVM once the context
 * has refreshed, before any runner. That would end the test's JVM too, so the test stops at the
 * same point another way: Boot announces {@link ApplicationStartedEvent} after the refresh and
 * before the runners, and a listener for it throws.
 */
class AotTrainingStartTests {

  @Test
  void theApplicationContextStartsWithoutADatabase() {
    String[] arguments = {
      "--spring.profiles.active=aot-training",
      // Somewhere nothing listens, like the Docker build
      "--spring.datasource.url=jdbc:postgresql://127.0.0.1:1/aot-training",
      "--server.port=0",
    };
    ApplicationListener<ApplicationStartedEvent> stopAfterRefresh =
        started -> {
          started.getApplicationContext().close();
          throw new RefreshedWithoutADatabase();
        };

    assertThatThrownBy(() ->
            new SpringApplicationBuilder(StudioManagerApplication.class)
                .listeners(stopAfterRefresh)
                .run(arguments))
        .isInstanceOf(RefreshedWithoutADatabase.class);
  }

  /** Thrown once the context has refreshed: the point where the Docker build's run ends. */
  private static final class RefreshedWithoutADatabase extends RuntimeException {}
}
