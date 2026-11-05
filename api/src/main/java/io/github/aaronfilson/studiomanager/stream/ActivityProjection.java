package io.github.aaronfilson.studiomanager.stream;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/**
 * Weekly totals from the event stream: firings by kiln, pieces fired, members joined and fees by
 * currency. Every write runs inside the event's studio, under row-level security.
 *
 * <p>Applying an event records its id in the same transaction as the totals, so one delivered
 * twice, or a topic replayed from the start, counts once.
 */
@Component
class ActivityProjection {

    enum Metric { FIRINGS, PIECES_FIRED, MEMBERS_JOINED, FEES }

    record Row(LocalDate weekStart, Metric metric, String dimension, int count, BigDecimal amount) {
    }

    private final JdbcClient jdbc;

    ActivityProjection(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Adds one event to the totals, unless it was applied before. The caller binds the studio.
     *
     * @return false if it had been seen already, or isn't a type this reads
     */
    @Transactional
    public boolean apply(UUID studioId, String type, JsonNode event) {
        UUID eventId = UUID.fromString(event.get("id").asString());
        int fresh = jdbc.sql("""
                insert into stream_event_seen (studio_id, event_id, seen_at) values (?, ?, now())
                on conflict do nothing
                """).params(studioId, eventId).update();
        if (fresh == 0) {
            return false;
        }
        LocalDate week = weekOf(Instant.parse(event.get("occurredAt").asString()));
        switch (type) {
            case StudioStreamEvent.FiringCompleted.TYPE -> {
                add(studioId, week, Metric.FIRINGS, event.get("kiln").asString(), 1, BigDecimal.ZERO);
                add(studioId, week, Metric.PIECES_FIRED, "", event.get("pieces").asInt(), BigDecimal.ZERO);
            }
            case StudioStreamEvent.FeeCharged.TYPE -> add(studioId, week, Metric.FEES,
                    event.get("currency").asString(), 1, new BigDecimal(event.get("amount").asString()));
            case StudioStreamEvent.MemberJoined.TYPE -> add(studioId, week, Metric.MEMBERS_JOINED, "", 1,
                    BigDecimal.ZERO);
            default -> {
                // A newer producer's type: remembered as seen, counted nowhere
                return false;
            }
        }
        return true;
    }

    /** Every row from the week {@code weeks - 1} weeks before this one, newest first. */
    @Transactional(readOnly = true)
    public List<Row> since(int weeks) {
        LocalDate from = weekOf(Instant.now()).minusWeeks(weeks - 1L);
        return jdbc.sql("""
                select week_start, metric, dimension, count, amount from weekly_activity
                where week_start >= ? order by week_start desc, metric, dimension
                """).param(from)
                .query((rs, n) -> new Row(rs.getObject("week_start", LocalDate.class),
                        Metric.valueOf(rs.getString("metric")), rs.getString("dimension"), rs.getInt("count"),
                        rs.getBigDecimal("amount")))
                .list();
    }

    private void add(UUID studioId, LocalDate week, Metric metric, String dimension, int count, BigDecimal amount) {
        jdbc.sql("""
                insert into weekly_activity (studio_id, week_start, metric, dimension, count, amount)
                values (?, ?, ?, ?, ?, ?)
                on conflict (studio_id, week_start, metric, dimension)
                do update set count = weekly_activity.count + excluded.count,
                              amount = weekly_activity.amount + excluded.amount
                """).params(studioId, week, metric.name(), dimension, count, amount).update();
    }

    /** The Monday that starts the week holding {@code at}, in the bound studio's time zone. */
    private LocalDate weekOf(Instant at) {
        ZoneId zone = ZoneId.of(jdbc.sql("select time_zone from studio").query(String.class).single());
        return at.atZone(zone).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

}
