package io.github.aaronfilson.studiomanager.platform;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * How far behind the outbox is: the event publications not yet completed, and the age of the
 * oldest. A count that keeps growing, or an age past the retry window, means listeners are failing
 * (an email provider down, a broker unreachable). Read when the metrics are exported, about once a
 * minute while a replica is up.
 */
@Component
class OutboxMetrics implements MeterBinder {

  private final JdbcClient jdbc;

  OutboxMetrics(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void bindTo(MeterRegistry registry) {
    Gauge.builder("studiomanager.outbox.incomplete", this, OutboxMetrics::incompleteCount)
        .description("Event publications not yet completed")
        .register(registry);
    Gauge.builder("studiomanager.outbox.oldest.age", this, OutboxMetrics::oldestIncompleteSeconds)
        .description("Age of the oldest incomplete event publication")
        .baseUnit("seconds")
        .register(registry);
  }

  double incompleteCount() {
    return jdbc.sql("select count(*) from event_publication where completion_date is null")
        .query(Long.class)
        .single();
  }

  double oldestIncompleteSeconds() {
    return jdbc.sql(
            """
            select coalesce(extract(epoch from now() - min(publication_date))::float8, 0)
            from event_publication where completion_date is null
            """)
        .query(Double.class)
        .single();
  }
}
