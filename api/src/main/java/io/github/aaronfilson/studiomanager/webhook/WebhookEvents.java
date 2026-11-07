package io.github.aaronfilson.studiomanager.webhook;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.StudioService;
import io.github.aaronfilson.studiomanager.studio.StudioView;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes an event and one delivery per subscribed endpoint, in the transaction of whatever caused
 * it. If that rolls back, nothing is sent. Once it commits, each delivery's first attempt starts
 * straight away on another thread. If the instance stops first, the sweep picks the delivery up a
 * minute later, so nothing is lost either.
 */
@Component
class WebhookEvents {

  /** The sweep leaves a new delivery alone this long, while its first attempt runs. */
  static final Duration FIRST_ATTEMPT_GRACE = Duration.ofMinutes(1);

  private final WebhookQueries queries;
  private final WebhookEndpointRepository endpoints;
  private final WebhookDeliveryRepository deliveries;
  private final EntityManager entityManager;
  private final StudioService studios;
  private final JsonMapper json;
  private final WebhookSender sender;
  private final Clock clock;

  WebhookEvents(
      WebhookQueries queries,
      WebhookEndpointRepository endpoints,
      WebhookDeliveryRepository deliveries,
      EntityManager entityManager,
      StudioService studios,
      JsonMapper json,
      WebhookSender sender,
      Clock clock) {
    this.queries = queries;
    this.endpoints = endpoints;
    this.deliveries = deliveries;
    this.entityManager = entityManager;
    this.studios = studios;
    this.json = json;
    this.sender = sender;
    this.clock = clock;
  }

  /**
   * An event, sent to every enabled endpoint subscribed to its type. Nothing is stored if there are
   * none.
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public void publish(WebhookEventType type, String dedupeKey, Map<String, Object> data) {
    // A studio has at most ten endpoints, so they're filtered here rather than in the query
    List<UUID> subscribed =
        endpoints.findByEnabledTrue().stream()
            .filter(endpoint -> endpoint.wants(type))
            .map(WebhookEndpoint::id)
            .toList();
    if (subscribed.isEmpty()) {
      return;
    }
    record(type, dedupeKey, data, subscribed)
        .ifPresent(event -> {
          TenantContext caller = TenantContext.require();
          if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                  @Override
                  public void afterCommit() {
                    event.deliveryIds().forEach(id -> sender.attemptSoon(caller, id));
                  }
                });
          }
        });
  }

  /**
   * Stores the event and a delivery to each of {@code endpointIds}, without sending. Returns empty
   * if an event with this key was already recorded.
   */
  @Transactional(propagation = Propagation.REQUIRED)
  Optional<Recorded> record(
      WebhookEventType type, String dedupeKey, Map<String, Object> data, List<UUID> endpointIds) {
    StudioView studio = studios.current();
    Instant now = clock.instant();
    UUID eventId = Uuids.v7();
    Map<String, Object> envelope = new LinkedHashMap<>();
    envelope.put("id", wireId(eventId));
    envelope.put("type", type.wireName());
    envelope.put("version", 1);
    envelope.put("created_at", now.truncatedTo(ChronoUnit.SECONDS).toString());
    envelope.put("studio", Map.of("id", studio.id().toString(), "name", studio.name()));
    envelope.put("data", data);
    String body = json.writeValueAsString(envelope);
    if (!queries.insertEvent(eventId, studio.id(), type, dedupeKey, body, now)) {
      return Optional.empty();
    }
    List<UUID> added = new ArrayList<>();
    for (UUID endpointId : endpointIds) {
      added.add(addDelivery(endpointId, eventId, now));
    }
    return Optional.of(new Recorded(eventId, added));
  }

  /** A new delivery of an event already stored, such as a resend. */
  UUID addDelivery(UUID endpointId, UUID eventId, Instant now) {
    // Persisted, not saved: a delivery has no version, so save would look it up first. The event
    // is only referenced, not loaded.
    WebhookDelivery delivery =
        new WebhookDelivery(
            endpointId,
            eventId,
            entityManager.getReference(WebhookEvent.class, eventId),
            now,
            now.plus(FIRST_ATTEMPT_GRACE));
    entityManager.persist(delivery);
    // The first attempt claims it with SQL, once this commits
    deliveries.flush();
    return delivery.id();
  }

  /** The id receivers see, in the payload and the {@code webhook-id} header. */
  static String wireId(UUID eventId) {
    return "evt_" + eventId.toString().replace("-", "");
  }

  record Recorded(UUID eventId, List<UUID> deliveryIds) {}
}
