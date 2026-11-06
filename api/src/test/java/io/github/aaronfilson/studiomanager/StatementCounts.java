package io.github.aaronfilson.studiomanager;

import static org.awaitility.Awaitility.await;

import jakarta.persistence.EntityManagerFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.modulith.events.core.EventPublicationRegistry;

/**
 * Counts the SQL statements Hibernate sends while some code runs, so a test can pin a list to a
 * fixed number of queries and catch an N+1 (ADR 21). The test profile turns on {@code
 * hibernate.generate_statistics}, and {@link IntegrationTest} provides this as a bean.
 *
 * <p>Hibernate's statistics are global, so it first waits for event listeners still working from
 * the test's setup. It counts only Hibernate's statements: SQL sent through {@code JdbcClient}
 * isn't seen.
 */
public class StatementCounts {

  private static final Duration RECENT = Duration.ofSeconds(20);

  private final Statistics statistics;
  private final EventPublicationRegistry publications;

  StatementCounts(
      EntityManagerFactory entityManagerFactory, EventPublicationRegistry publications) {
    this.statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    this.publications = publications;
  }

  /** What {@code work} returned, and how many statements Hibernate prepared for it. */
  public <T> Counted<T> of(Callable<T> work) throws Exception {
    // Listeners for events the test just caused. Older incomplete ones are failures left by other
    // tests in the shared context, which won't finish.
    Instant recent = Instant.now().minus(RECENT);
    await()
        .atMost(Duration.ofSeconds(30))
        .until(() ->
            publications.findIncompletePublications().stream()
                .noneMatch(publication -> publication.getPublicationDate().isAfter(recent)));
    statistics.clear();
    T result = work.call();
    return new Counted<>(result, statistics.getPrepareStatementCount());
  }

  public record Counted<T>(T result, long statements) {}
}
