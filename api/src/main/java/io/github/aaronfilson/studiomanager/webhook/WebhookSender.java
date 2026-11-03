package io.github.aaronfilson.studiomanager.webhook;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.http.SafeHttpClient;
import io.github.aaronfilson.studiomanager.platform.http.SafeHttpClient.UnsafeUrlException;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Makes one attempt at a delivery: claims it, signs and POSTs it outside any transaction, then
 * records what happened and when to try again.
 *
 * <p>The claim is a conditional update, so of two instances (or the sweep and a first attempt)
 * reaching for the same delivery, one gets it. A claim lapses after {@link #CLAIM}, so a delivery
 * whose sender died is picked up again.
 */
@Component
class WebhookSender {

    private static final Logger log = LoggerFactory.getLogger(WebhookSender.class);

    static final Duration CLAIM = Duration.ofMinutes(2);
    static final int EXCERPT_CHARS = 1000;

    private final JdbcClient jdbc;
    private final TransactionOperations transactions;
    private final SafeHttpClient http;
    private final WebhookSecrets secrets;
    private final ApplicationEventPublisher events;

    WebhookSender(JdbcClient jdbc, TransactionOperations transactions, SafeHttpClient http, WebhookSecrets secrets,
            ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.http = http;
        this.secrets = secrets;
        this.events = events;
    }

    /** {@link #attempt}, on another thread, so the request that caused the event isn't held up. */
    @Async
    public void attemptSoon(TenantContext as, UUID deliveryId) {
        try {
            attempt(as, deliveryId);
        } catch (RuntimeException e) {
            // The delivery is still PENDING, so the sweep tries it again
            log.warn("Webhook delivery {} failed before it was recorded: {}", deliveryId, e.toString());
        }
    }

    /** Tries the delivery once, if it's still pending and nobody else has it. Returns whether it arrived. */
    public boolean attempt(TenantContext as, UUID deliveryId) {
        return TenantContext.callAs(as, () -> {
            Claimed claimed = transactions.execute(tx -> claim(deliveryId, now()));
            if (claimed == null) {
                return false;
            }
            Outcome outcome = send(claimed);
            Boolean arrived = transactions.execute(tx -> record(claimed, outcome, now()));
            return Boolean.TRUE.equals(arrived);
        });
    }

    private @Nullable Claimed claim(UUID deliveryId, Instant now) {
        return jdbc.sql("""
                update webhook_delivery d set claimed_until = :until
                from webhook_endpoint e, webhook_event ev
                where d.id = :id and d.status = 'PENDING' and (d.claimed_until is null or d.claimed_until < :now)
                  and e.id = d.endpoint_id and e.enabled and ev.id = d.event_id
                returning d.id, d.studio_id, d.endpoint_id, d.attempts, e.url, e.secret, e.previous_secret,
                          e.previous_secret_until, ev.id as event_id, ev.body
                """)
                .param("until", (now.plus(CLAIM)).atOffset(ZoneOffset.UTC))
                .param("now", now.atOffset(ZoneOffset.UTC))
                .param("id", deliveryId)
                .query(Claimed.class).optional().orElse(null);
    }

    private Outcome send(Claimed delivery) {
        Instant now = Instant.now();
        List<String> signWith = new ArrayList<>();
        signWith.add(secrets.decrypt(delivery.endpointId(), delivery.secret()));
        if (delivery.previousSecret() != null && delivery.previousSecretUntil() != null
                && delivery.previousSecretUntil().toInstant().isAfter(now)) {
            signWith.add(secrets.decrypt(delivery.endpointId(), delivery.previousSecret()));
        }
        var headers = WebhookSigner.headers(signWith, WebhookEvents.wireId(delivery.eventId()), now.getEpochSecond(),
                delivery.body());
        long started = System.nanoTime();
        try {
            SafeHttpClient.Response response = http.postJson(delivery.url(), headers, delivery.body());
            return new Outcome(response.statusCode(), null, millisSince(started), excerpt(response.body()));
        } catch (UnsafeUrlException e) {
            return new Outcome(null, e.getMessage(), millisSince(started), null);
        } catch (SocketTimeoutException e) {
            return new Outcome(null, "No answer within 10 seconds.", millisSince(started), null);
        } catch (IOException e) {
            return new Outcome(null, "Couldn't connect: " + e.getMessage(), millisSince(started), null);
        }
    }

    private boolean record(Claimed delivery, Outcome outcome, Instant now) {
        jdbc.sql("""
                insert into webhook_attempt (id, studio_id, delivery_id, attempted_at, status_code, error, duration_ms,
                                             response_excerpt)
                values (:id, :studioId, :deliveryId, :now, :status, :error, :ms, :excerpt)
                """)
                .param("id", Uuids.v7())
                .param("studioId", delivery.studioId())
                .param("deliveryId", delivery.id())
                .param("now", now.atOffset(ZoneOffset.UTC))
                .param("status", outcome.statusCode())
                .param("error", outcome.error())
                .param("ms", outcome.durationMs())
                .param("excerpt", outcome.excerpt())
                .update();
        int attempts = delivery.attempts() + 1;
        boolean arrived = outcome.statusCode() != null && Backoff.delivered(outcome.statusCode());
        if (arrived) {
            jdbc.sql("""
                    update webhook_delivery set status = 'DELIVERED', attempts = :attempts, delivered_at = :now,
                           next_attempt_at = null, claimed_until = null
                    where id = :id
                    """)
                    .param("attempts", attempts).param("now", now.atOffset(ZoneOffset.UTC)).param("id", delivery.id())
                    .update();
            jdbc.sql("update webhook_endpoint set consecutive_failures = 0, failing_since = null where id = :id")
                    .param("id", delivery.endpointId()).update();
            return true;
        }
        Optional<Instant> next = outcome.statusCode() != null && outcome.statusCode() == 410 ? Optional.empty()
                : Backoff.nextAttempt(attempts, now);
        jdbc.sql("""
                update webhook_delivery set status = :status, attempts = :attempts, next_attempt_at = :next,
                       claimed_until = null
                where id = :id
                """)
                .param("status", next.isPresent() ? "PENDING" : "FAILED")
                .param("attempts", attempts)
                .param("next", next.map(at -> at.atOffset(ZoneOffset.UTC)).orElse(null))
                .param("id", delivery.id())
                .update();
        Failing failing = jdbc.sql("""
                update webhook_endpoint set consecutive_failures = consecutive_failures + 1,
                       failing_since = coalesce(failing_since, :now)
                where id = :id
                returning consecutive_failures, failing_since
                """)
                .param("now", now.atOffset(ZoneOffset.UTC)).param("id", delivery.endpointId())
                .query(Failing.class).single();
        if (outcome.statusCode() != null && outcome.statusCode() == 410) {
            disable(delivery, "It answered 410 Gone, which means the receiver was removed.");
        } else if (Backoff.shouldDisable(failing.consecutiveFailures(), failing.failingSince().toInstant(), now)) {
            disable(delivery, "Every delivery failed for %s (%d in a row). The last: %s"
                    .formatted(failing.failingSince().toInstant().isBefore(now.minus(Backoff.DISABLE_AFTER))
                            ? "3 days" : "a while", failing.consecutiveFailures(), outcome.summary()));
        }
        return false;
    }

    private void disable(Claimed delivery, String reason) {
        Optional<String> url = jdbc.sql("""
                update webhook_endpoint set enabled = false, disabled_reason = :reason
                where id = :id and enabled
                returning url
                """)
                .param("reason", reason).param("id", delivery.endpointId())
                .query(String.class).optional();
        if (url.isEmpty()) {
            return;
        }
        // It won't be retried, and turning it back on doesn't replay what it missed
        jdbc.sql("""
                update webhook_delivery set status = 'FAILED', next_attempt_at = null, claimed_until = null
                where endpoint_id = :id and status = 'PENDING'
                """)
                .param("id", delivery.endpointId()).update();
        log.info("Disabled webhook endpoint {} in studio {}: {}", delivery.endpointId(), delivery.studioId(), reason);
        events.publishEvent(new WebhookEndpointDisabled(delivery.studioId(), delivery.endpointId(), url.get(), reason));
    }

    private static @Nullable String excerpt(String body) {
        if (body.isEmpty()) {
            return null;
        }
        return body.length() <= EXCERPT_CHARS ? body : body.substring(0, EXCERPT_CHARS);
    }

    private static int millisSince(long startedNanos) {
        return (int) Math.min(Integer.MAX_VALUE, (System.nanoTime() - startedNanos) / 1_000_000);
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    record Claimed(UUID id, UUID studioId, UUID endpointId, int attempts, String url, byte[] secret,
            byte @Nullable [] previousSecret, @Nullable OffsetDateTime previousSecretUntil, UUID eventId,
            String body) {
    }

    record Outcome(@Nullable Integer statusCode, @Nullable String error, int durationMs, @Nullable String excerpt) {

        String summary() {
            return statusCode != null ? "HTTP " + statusCode + "." : String.valueOf(error);
        }
    }

    record Failing(int consecutiveFailures, OffsetDateTime failingSince) {
    }

}
