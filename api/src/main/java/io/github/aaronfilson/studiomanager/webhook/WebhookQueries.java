package io.github.aaronfilson.studiomanager.webhook;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * The webhook module's SQL, each statement for one of ADR 21's reasons. Endpoints, new deliveries
 * and the delivery log are Hibernate.
 *
 * <p>Most of it is the delivery queue: a delivery is claimed with one conditional update, so of two
 * instances reaching for it one wins, and its outcome is recorded the same way. None of these load
 * entities, so there is nothing to flush first.
 */
@Component
class WebhookQueries {

  private final JdbcClient jdbc;

  WebhookQueries(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Counters and upserts: stores an event, or nothing if one with this key is already stored
   * ({@code on conflict do nothing}), so a listener that runs twice adds nothing. Returns whether
   * it was stored.
   */
  boolean insertEvent(
      UUID eventId,
      UUID studioId,
      WebhookEventType type,
      String dedupeKey,
      String body,
      Instant now) {
    return jdbc.sql(
            """
            insert into webhook_event (id, studio_id, type, dedupe_key, body, created_at)
            values (:id, :studioId, :type, :key, :body, :now)
            on conflict (studio_id, dedupe_key) do nothing
            returning id
            """)
        .param("id", eventId)
        .param("studioId", studioId)
        .param("type", type.wireName())
        .param("key", dedupeKey)
        .param("body", body)
        .param("now", utc(now))
        .query(UUID.class)
        .optional()
        .isPresent();
  }

  /**
   * Claims and queues: takes the delivery for {@link WebhookSender#CLAIM}, if it's still pending,
   * nobody holds it and its endpoint is on. Returns what sending it needs, or empty.
   */
  Optional<WebhookSender.Claimed> claim(UUID deliveryId, Instant now, Instant until) {
    return jdbc.sql(
            """
            update webhook_delivery d set claimed_until = :until
            from webhook_endpoint e, webhook_event ev
            where d.id = :id and d.status = 'PENDING' and (d.claimed_until is null or d.claimed_until < :now)
              and e.id = d.endpoint_id and e.enabled and ev.id = d.event_id
            returning d.id, d.studio_id, d.endpoint_id, d.attempts, e.url, e.secret, e.previous_secret,
                      e.previous_secret_until, ev.id as event_id, ev.body
            """)
        .param("until", utc(until))
        .param("now", utc(now))
        .param("id", deliveryId)
        .query(WebhookSender.Claimed.class)
        .optional();
  }

  /** Claims and queues: logs one attempt at a claimed delivery. */
  void recordAttempt(
      UUID studioId, UUID deliveryId, Instant at, WebhookSender.Outcome outcome, UUID attemptId) {
    jdbc.sql(
            """
            insert into webhook_attempt (id, studio_id, delivery_id, attempted_at, status_code, error, duration_ms,
                                         response_excerpt)
            values (:id, :studioId, :deliveryId, :now, :status, :error, :ms, :excerpt)
            """)
        .param("id", attemptId)
        .param("studioId", studioId)
        .param("deliveryId", deliveryId)
        .param("now", utc(at))
        .param("status", outcome.statusCode())
        .param("error", outcome.error())
        .param("ms", outcome.durationMs())
        .param("excerpt", outcome.excerpt())
        .update();
  }

  /** Claims and queues: releases a delivery that arrived, and clears its endpoint's failures. */
  void delivered(UUID deliveryId, UUID endpointId, int attempts, Instant now) {
    jdbc.sql(
            """
            update webhook_delivery set status = 'DELIVERED', attempts = :attempts, delivered_at = :now,
                   next_attempt_at = null, claimed_until = null
            where id = :id
            """)
        .param("attempts", attempts)
        .param("now", utc(now))
        .param("id", deliveryId)
        .update();
    jdbc.sql(
            "update webhook_endpoint set consecutive_failures = 0, failing_since = null where id = :id")
        .param("id", endpointId)
        .update();
  }

  /**
   * Claims and queues: releases a delivery that failed, to be tried at {@code nextAttempt} or, with
   * none, never again. Counts the failure on its endpoint in the same statement that reads the
   * count, so two failing at once both count.
   */
  Failing failed(
      UUID deliveryId, UUID endpointId, int attempts, @Nullable Instant nextAttempt, Instant now) {
    jdbc.sql(
            """
            update webhook_delivery set status = :status, attempts = :attempts, next_attempt_at = :next,
                   claimed_until = null
            where id = :id
            """)
        .param("status", nextAttempt != null ? "PENDING" : "FAILED")
        .param("attempts", attempts)
        .param("next", nextAttempt == null ? null : utc(nextAttempt))
        .param("id", deliveryId)
        .update();
    return jdbc.sql(
            """
            update webhook_endpoint set consecutive_failures = consecutive_failures + 1,
                   failing_since = coalesce(failing_since, :now)
            where id = :id
            returning consecutive_failures, failing_since
            """)
        .param("now", utc(now))
        .param("id", endpointId)
        .query((rs, row) ->
            new Failing(
                rs.getInt("consecutive_failures"),
                rs.getObject("failing_since", OffsetDateTime.class).toInstant()))
        .single();
  }

  /** How long an endpoint has been failing, and how many times in a row. */
  record Failing(int consecutiveFailures, Instant failingSince) {}

  /**
   * Claims and queues: turns the endpoint off, if it's still on, and fails everything waiting for
   * it. Returns its URL if this call turned it off, so only one caller announces it.
   */
  Optional<String> disable(UUID endpointId, String reason) {
    Optional<String> url =
        jdbc.sql(
                """
                update webhook_endpoint set enabled = false, disabled_reason = :reason
                where id = :id and enabled
                returning url
                """)
            .param("reason", reason)
            .param("id", endpointId)
            .query(String.class)
            .optional();
    if (url.isPresent()) {
      // It won't be retried, and turning it back on doesn't replay what it missed
      jdbc.sql(
              """
              update webhook_delivery set status = 'FAILED', next_attempt_at = null, claimed_until = null
              where endpoint_id = :id and status = 'PENDING'
              """)
          .param("id", endpointId)
          .update();
    }
    return url;
  }

  /**
   * Cross-tenant functions: the deliveries due at {@code now}, in every studio ({@code
   * app.due_webhook_deliveries}, ADR 10). Only ids; each is then sent inside its studio.
   */
  List<WebhookSweep.Due> dueDeliveries(Instant now, int max) {
    return jdbc.sql("select delivery_id, studio_id from app.due_webhook_deliveries(:now, :max)")
        .param("now", utc(now))
        .param("max", max)
        .query((rs, row) ->
            new WebhookSweep.Due(
                rs.getObject("delivery_id", UUID.class), rs.getObject("studio_id", UUID.class)))
        .list();
  }

  /**
   * Cross-tenant functions: deletes events from before {@code before}, with their deliveries and
   * attempts, in every studio ({@code app.purge_webhook_history}). Returns how many events.
   */
  int purgeHistory(Instant before) {
    return jdbc.sql("select app.purge_webhook_history(:before)")
        .param("before", utc(before))
        .query(Integer.class)
        .single();
  }

  private static OffsetDateTime utc(Instant instant) {
    return instant.atOffset(ZoneOffset.UTC);
  }
}
