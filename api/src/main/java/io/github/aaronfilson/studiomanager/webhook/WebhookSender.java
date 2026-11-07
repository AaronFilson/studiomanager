package io.github.aaronfilson.studiomanager.webhook;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.http.SafeHttpClient;
import io.github.aaronfilson.studiomanager.platform.http.SafeHttpClient.UnsafeUrlException;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
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

  private final WebhookQueries queries;
  private final TransactionOperations transactions;
  private final SafeHttpClient http;
  private final WebhookSecrets secrets;
  private final ApplicationEventPublisher events;
  private final MeterRegistry meters;
  private final Clock clock;

  WebhookSender(
      WebhookQueries queries,
      TransactionOperations transactions,
      SafeHttpClient http,
      WebhookSecrets secrets,
      ApplicationEventPublisher events,
      MeterRegistry meters,
      Clock clock) {
    this.queries = queries;
    this.transactions = transactions;
    this.http = http;
    this.secrets = secrets;
    this.events = events;
    this.meters = meters;
    this.clock = clock;
  }

  /** {@link #attempt}, on another thread, so the request that caused the event isn't held up. */
  @Async
  public void attemptSoon(TenantContext as, UUID deliveryId) {
    try {
      attempt(as, deliveryId);
    } catch (RuntimeException e) {
      // The delivery is still PENDING, so the sweep tries it again
      log.atWarn()
          .setCause(e)
          .addKeyValue("deliveryId", deliveryId)
          .addKeyValue("studioId", as.studioId())
          .log("Webhook delivery {} failed before it was recorded", deliveryId);
    }
  }

  /**
   * Tries the delivery once, if it's still pending and nobody else has it. Returns whether it
   * arrived.
   */
  public boolean attempt(TenantContext as, UUID deliveryId) {
    return TenantContext.callAs(
        as,
        () -> {
          Instant claimedAt = clock.instant();
          Claimed claimed =
              transactions.execute(
                  tx -> queries.claim(deliveryId, claimedAt, claimedAt.plus(CLAIM)).orElse(null));
          if (claimed == null) {
            return false;
          }
          Outcome outcome = send(claimed);
          DeliveryResult result =
              transactions.execute(tx -> record(claimed, outcome, clock.instant()));
          if (result == null) {
            return false;
          }
          countOutcome(result);
          return result == DeliveryResult.DELIVERED;
        });
  }

  private Outcome send(Claimed delivery) {
    Instant now = clock.instant();
    List<String> signWith = new ArrayList<>();
    signWith.add(secrets.decrypt(delivery.endpointId(), delivery.secret()));
    byte[] previousSecret = delivery.previousSecret();
    OffsetDateTime previousUntil = delivery.previousSecretUntil();
    if (previousSecret != null && previousUntil != null && previousUntil.toInstant().isAfter(now)) {
      signWith.add(secrets.decrypt(delivery.endpointId(), previousSecret));
    }
    var headers =
        WebhookSigner.headers(
            signWith,
            WebhookEvents.wireId(delivery.eventId()),
            now.getEpochSecond(),
            delivery.body());
    long started = System.nanoTime();
    try {
      SafeHttpClient.Response response = http.postJson(delivery.url(), headers, delivery.body());
      return new Outcome(
          response.statusCode(), null, millisSince(started), excerpt(response.body()));
    } catch (UnsafeUrlException e) {
      return new Outcome(null, e.getMessage(), millisSince(started), null);
    } catch (SocketTimeoutException e) {
      return new Outcome(null, "No answer within 10 seconds.", millisSince(started), null);
    } catch (IOException e) {
      return new Outcome(null, "Couldn't connect: " + e.getMessage(), millisSince(started), null);
    }
  }

  private DeliveryResult record(Claimed delivery, Outcome outcome, Instant now) {
    queries.recordAttempt(delivery.studioId(), delivery.id(), now, outcome, Uuids.v7());
    int attempts = delivery.attempts() + 1;
    Integer statusCode = outcome.statusCode();
    if (statusCode != null && Backoff.delivered(statusCode)) {
      queries.delivered(delivery.id(), delivery.endpointId(), attempts, now);
      return DeliveryResult.DELIVERED;
    }
    boolean gone = statusCode != null && statusCode == 410;
    Optional<Instant> next = gone ? Optional.empty() : Backoff.nextAttempt(attempts, now);
    WebhookQueries.Failing failing =
        queries.failed(delivery.id(), delivery.endpointId(), attempts, next.orElse(null), now);
    if (gone) {
      disable(delivery, "It answered 410 Gone, which means the receiver was removed.");
    } else if (Backoff.shouldDisable(failing.consecutiveFailures(), failing.failingSince(), now)) {
      disable(
          delivery,
          "Every delivery failed for %s (%d in a row). The last: %s"
              .formatted(
                  failing.failingSince().isBefore(now.minus(Backoff.DISABLE_AFTER))
                      ? "3 days"
                      : "a while",
                  failing.consecutiveFailures(),
                  outcome.summary()));
    }
    return next.isPresent() ? DeliveryResult.WILL_RETRY : DeliveryResult.GAVE_UP;
  }

  /**
   * Counts {@code studiomanager.webhook.deliveries} by result: one per attempt, so the share that
   * fail is a rate an alert can watch.
   */
  private void countOutcome(DeliveryResult result) {
    String resultTag =
        switch (result) {
          case DELIVERED -> "delivered";
          case WILL_RETRY -> "will_retry";
          case GAVE_UP -> "gave_up";
        };
    meters.counter("studiomanager.webhook.deliveries", "result", resultTag).increment();
  }

  private void disable(Claimed delivery, String reason) {
    Optional<String> url = queries.disable(delivery.endpointId(), reason);
    if (url.isEmpty()) {
      return;
    }
    log.info(
        "Disabled webhook endpoint {} in studio {}: {}",
        delivery.endpointId(),
        delivery.studioId(),
        reason);
    events.publishEvent(
        new WebhookEndpointDisabled(delivery.studioId(), delivery.endpointId(), url.get(), reason));
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

  // A claimed delivery, carried from the claim to the send and never compared
  @SuppressWarnings("ArrayRecordComponent")
  record Claimed(
      UUID id,
      UUID studioId,
      UUID endpointId,
      int attempts,
      String url,
      byte[] secret,
      byte @Nullable [] previousSecret,
      @Nullable OffsetDateTime previousSecretUntil,
      UUID eventId,
      String body) {}

  record Outcome(
      @Nullable Integer statusCode,
      @Nullable String error,
      int durationMs,
      @Nullable String excerpt) {

    String summary() {
      return statusCode != null ? "HTTP " + statusCode + "." : String.valueOf(error);
    }
  }

  /** How an attempt ended: it arrived, it will be tried again, or it won't be. */
  enum DeliveryResult {
    DELIVERED,
    WILL_RETRY,
    GAVE_UP
  }
}
