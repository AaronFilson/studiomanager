package io.github.aaronfilson.studiomanager.stream;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/**
 * Weekly totals from the event stream: firings by kiln, pieces fired, members joined and fees by
 * currency. Every write runs inside the event's studio, under row-level security.
 *
 * <p>Applying an event records its id in the same transaction as the totals, so one delivered
 * twice, or a topic replayed from the start, counts once. Only events it counts are recorded: a
 * type it doesn't read yet is left alone, so a later version that reads it can count it on replay.
 *
 * <p>To rebuild a studio's totals, delete its {@code weekly_activity} and {@code stream_event_seen}
 * rows together, then replay the topic. Only what the topic still holds (7 days) comes back.
 */
@Component
class ActivityProjection {

  /** Longer than the topic keeps events, so a replay never meets an id that was forgotten. */
  static final Duration REMEMBER_SEEN = Duration.ofDays(30);

  static final Set<String> TYPES =
      Set.of(
          StudioStreamEvent.FiringCompleted.TYPE,
          StudioStreamEvent.FeeCharged.TYPE,
          StudioStreamEvent.MemberJoined.TYPE);

  enum Metric {
    FIRINGS,
    PIECES_FIRED,
    MEMBERS_JOINED,
    FEES
  }

  record Row(
      LocalDate weekStart,
      Metric metric,
      String dimension,
      @Nullable String label,
      int count,
      BigDecimal amount) {}

  private final JdbcClient jdbc;

  ActivityProjection(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Adds one event to the totals, unless it was applied before. The caller binds the studio.
   *
   * @return false if it had been counted already, or isn't a type this reads
   */
  @Transactional
  public boolean apply(UUID studioId, String type, JsonNode event) {
    if (!TYPES.contains(type)) {
      return false;
    }
    UUID eventId = UUID.fromString(event.get("id").asString());
    int fresh =
        jdbc.sql(
                """
                insert into stream_event_seen (studio_id, event_id, seen_at) values (?, ?, now())
                on conflict do nothing
                """)
            .params(studioId, eventId)
            .update();
    if (fresh == 0) {
      return false;
    }
    Instant at = Instant.parse(event.get("occurredAt").asString());
    switch (type) {
      case StudioStreamEvent.FiringCompleted.TYPE -> {
        add(
            studioId,
            at,
            Metric.FIRINGS,
            event.get("kilnId").asString(),
            event.get("kiln").asString(),
            1,
            BigDecimal.ZERO);
        add(
            studioId,
            at,
            Metric.PIECES_FIRED,
            "",
            null,
            event.get("pieces").asInt(),
            BigDecimal.ZERO);
      }
      case StudioStreamEvent.FeeCharged.TYPE ->
          add(
              studioId,
              at,
              Metric.FEES,
              event.get("currency").asString(),
              null,
              1,
              new BigDecimal(event.get("amount").asString()));
      default -> add(studioId, at, Metric.MEMBERS_JOINED, "", null, 1, BigDecimal.ZERO);
    }
    return true;
  }

  /** Every row from the week {@code weeks - 1} weeks before this one, newest first. */
  @Transactional(readOnly = true)
  public List<Row> since(UUID studioId, int weeks) {
    return jdbc.sql(
            """
            select a.week_start, a.metric, a.dimension, a.label, a.count, a.amount
            from weekly_activity a
            join studio s on s.id = a.studio_id
            where a.studio_id = ?
              and a.week_start >= date_trunc('week', now() at time zone s.time_zone)::date - 7 * (? - 1)
            order by a.week_start desc, a.metric, a.label, a.dimension
            """)
        .params(studioId, weeks)
        .query((rs, n) ->
            new Row(
                rs.getObject("week_start", LocalDate.class),
                Metric.valueOf(rs.getString("metric")),
                rs.getString("dimension"),
                rs.getString("label"),
                rs.getInt("count"),
                rs.getBigDecimal("amount")))
        .list();
  }

  /** Forgets ids seen before the cut-off, in every studio. */
  @Transactional
  public int forgetSeenBefore(Instant cutOff) {
    return jdbc.sql("select app.purge_stream_event_seen(?)")
        .param(java.sql.Timestamp.from(cutOff))
        .query(Integer.class)
        .single();
  }

  /**
   * Adds to one total. The week is the Monday of {@code at} in the studio's time zone, worked out
   * in the same statement. The label always takes the newest value, so a renamed kiln shows its
   * current name for the whole week.
   */
  private void add(
      UUID studioId,
      Instant at,
      Metric metric,
      String dimension,
      @Nullable String label,
      int count,
      BigDecimal amount) {
    jdbc.sql(
            """
            insert into weekly_activity (studio_id, week_start, metric, dimension, label, count, amount)
            select s.id, date_trunc('week', ?::timestamptz at time zone s.time_zone)::date, ?, ?, ?, ?, ?
            from studio s where s.id = ?
            on conflict (studio_id, week_start, metric, dimension)
            do update set count = weekly_activity.count + excluded.count,
                          amount = weekly_activity.amount + excluded.amount,
                          label = coalesce(excluded.label, weekly_activity.label)
            """)
        .params(
            java.sql.Timestamp.from(at), metric.name(), dimension, label, count, amount, studioId)
        .update();
  }
}
