package io.github.aaronfilson.studiomanager.webhook;

import io.github.aaronfilson.studiomanager.platform.Capped;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.http.SafeHttpClient;
import io.github.aaronfilson.studiomanager.platform.http.SafeHttpClient.UnsafeUrlException;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.StudioService;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;

/** Staff manage where a studio's webhooks go, and look back at what was sent. */
@Service
public class WebhookEndpoints {

  static final int MAX_ENDPOINTS = 10;
  static final Duration ROTATION_OVERLAP = Duration.ofHours(24);
  static final Duration LOG_DAYS = Duration.ofDays(30);

  /** The most deliveries {@link #deliveries} returns. */
  static final int DELIVERY_LOG_LIMIT = 500;

  private final WebhookEndpointRepository endpoints;
  private final WebhookDeliveryRepository deliveryLog;
  private final EntityManager entityManager;
  private final TransactionOperations transactions;
  private final SafeHttpClient http;
  private final WebhookSecrets secrets;
  private final WebhookEvents events;
  private final WebhookSender sender;
  private final StudioService studios;
  private final Clock clock;

  WebhookEndpoints(
      WebhookEndpointRepository endpoints,
      WebhookDeliveryRepository deliveryLog,
      EntityManager entityManager,
      TransactionOperations transactions,
      SafeHttpClient http,
      WebhookSecrets secrets,
      WebhookEvents events,
      WebhookSender sender,
      StudioService studios,
      Clock clock) {
    this.endpoints = endpoints;
    this.deliveryLog = deliveryLog;
    this.entityManager = entityManager;
    this.transactions = transactions;
    this.http = http;
    this.secrets = secrets;
    this.events = events;
    this.sender = sender;
    this.studios = studios;
    this.clock = clock;
  }

  public record EndpointView(
      UUID id,
      String url,
      @Nullable String description,
      List<WebhookEventType> eventTypes,
      boolean enabled,
      @Nullable String disabledReason,
      int consecutiveFailures,
      @Nullable Instant previousSecretValidUntil,
      Instant createdAt) {}

  /** A new endpoint, with its secret: the only time the secret is shown. */
  public record CreatedEndpoint(EndpointView endpoint, String secret) {}

  public record RotatedSecret(String secret, Instant previousValidUntil) {}

  public record AttemptView(
      Instant attemptedAt,
      @Nullable Integer statusCode,
      @Nullable String error,
      int durationMs,
      @Nullable String responseExcerpt) {}

  public record DeliveryView(
      UUID id,
      String eventId,
      WebhookEventType eventType,
      String status,
      int attempts,
      Instant createdAt,
      @Nullable Instant nextAttemptAt,
      @Nullable Instant deliveredAt,
      List<AttemptView> attemptLog) {}

  @Transactional(readOnly = true)
  public List<EndpointView> list() {
    requireStaff();
    Instant now = clock.instant();
    return endpoints.findAllInOrder().stream().map(endpoint -> endpoint.toView(now)).toList();
  }

  @Transactional
  public CreatedEndpoint create(String url, @Nullable String description, List<String> eventTypes) {
    requireStaff();
    if (endpoints.count() >= MAX_ENDPOINTS) {
      throw new InvalidInputException(
          "A studio can have up to %d webhook endpoints. Delete one first."
              .formatted(MAX_ENDPOINTS));
    }
    String cleanUrl = checkUrl(url);
    // The secret is encrypted bound to the endpoint's id, so the id comes first
    UUID id = Uuids.v7();
    String secret = WebhookSecrets.newSecret();
    WebhookEndpoint endpoint =
        new WebhookEndpoint(
            id,
            cleanUrl,
            cleanDescription(description),
            types(eventTypes),
            secrets.encrypt(id, secret));
    // Persisted, not saved: an endpoint has no version, so save would look it up first
    entityManager.persist(endpoint);
    endpoints.flush();
    return new CreatedEndpoint(endpoint.toView(clock.instant()), secret);
  }

  /** Changes what's given; turning an endpoint back on clears its failures. */
  @Transactional
  public EndpointView update(
      UUID id,
      @Nullable String url,
      @Nullable String description,
      @Nullable List<String> eventTypes,
      @Nullable Boolean enabled) {
    requireStaff();
    WebhookEndpoint endpoint = find(id);
    endpoint.change(
        url == null ? endpoint.url() : checkUrl(url),
        description == null ? endpoint.description() : cleanDescription(description),
        eventTypes == null ? endpoint.eventTypes() : types(eventTypes),
        enabled == null ? endpoint.enabled() : enabled);
    endpoints.flush();
    return endpoint.toView(clock.instant());
  }

  @Transactional
  public void delete(UUID id) {
    requireStaff();
    endpoints.delete(find(id));
  }

  /** A new secret. The old one still signs, alongside it, for {@link #ROTATION_OVERLAP}. */
  @Transactional
  public RotatedSecret rotateSecret(UUID id) {
    requireStaff();
    WebhookEndpoint endpoint =
        endpoints
            .findAndLock(id)
            .orElseThrow(() -> new NotFoundException("No such webhook endpoint."));
    String secret = WebhookSecrets.newSecret();
    Instant until = clock.instant().plus(ROTATION_OVERLAP);
    endpoint.rotateSecret(secrets.encrypt(id, secret), until);
    return new RotatedSecret(secret, until);
  }

  /** Sends a {@code ping} now, even to a disabled endpoint's URL, and returns how it went. */
  public DeliveryView test(UUID id) {
    TenantContext caller = requireStaff();
    UUID deliveryId =
        Objects.requireNonNull(
            transactions.execute(tx -> {
              WebhookEndpoint endpoint = find(id);
              if (!endpoint.enabled()) {
                throw new InvalidInputException("Turn the endpoint on before sending a test.");
              }
              var studio = studios.current();
              Map<String, Object> data = new LinkedHashMap<>();
              data.put(
                  "message",
                  "A test from " + studio.name() + ". If you can see this, the webhook works.");
              return events
                  .record(WebhookEventType.PING, "ping:" + Uuids.v7(), data, List.of(id))
                  .orElseThrow()
                  .deliveryIds()
                  .getFirst();
            }));
    sender.attempt(caller, deliveryId);
    return Objects.requireNonNull(transactions.execute(tx -> delivery(deliveryId)));
  }

  /** The last 30 days of deliveries to an endpoint, newest first. */
  @Transactional(readOnly = true)
  public Capped<DeliveryView> deliveries(UUID endpointId) {
    requireStaff();
    find(endpointId);
    Capped<WebhookDelivery> found =
        Capped.fromFetched(
            deliveryLog.findLog(
                endpointId,
                clock.instant().minus(LOG_DAYS),
                Limit.of(Capped.fetchSize(DELIVERY_LOG_LIMIT))),
            DELIVERY_LOG_LIMIT);
    return found.withItems(withAttempts(found.items()));
  }

  /**
   * The same event again, as a new delivery, sent now. Receivers see the same {@code webhook-id}.
   */
  public DeliveryView resend(UUID deliveryId) {
    TenantContext caller = requireStaff();
    UUID newId =
        Objects.requireNonNull(
            transactions.execute(tx -> {
              WebhookDelivery original =
                  deliveryLog
                      .findById(deliveryId)
                      .orElseThrow(() -> new NotFoundException("No such delivery."));
              if (!find(original.endpointId()).enabled()) {
                throw new InvalidInputException("Turn the endpoint on before resending to it.");
              }
              return events.addDelivery(original.endpointId(), original.eventId(), clock.instant());
            }));
    sender.attempt(caller, newId);
    return Objects.requireNonNull(transactions.execute(tx -> delivery(newId)));
  }

  private DeliveryView delivery(UUID id) {
    WebhookDelivery found =
        deliveryLog.findWithEvent(id).orElseThrow(() -> new NotFoundException("No such delivery."));
    return withAttempts(List.of(found)).getFirst();
  }

  /** Each delivery with its attempts, newest first: one query for the attempts of them all. */
  private List<DeliveryView> withAttempts(List<WebhookDelivery> found) {
    if (found.isEmpty()) {
      return List.of();
    }
    Map<UUID, List<AttemptView>> attemptsByDelivery =
        deliveryLog.findAttempts(found.stream().map(WebhookDelivery::id).toList()).stream()
            .collect(
                Collectors.groupingBy(
                    WebhookAttempt::deliveryId,
                    Collectors.mapping(WebhookAttempt::toView, Collectors.toList())));
    List<DeliveryView> views = new ArrayList<>(found.size());
    for (WebhookDelivery delivery : found) {
      views.add(delivery.toView(attemptsByDelivery.getOrDefault(delivery.id(), List.of())));
    }
    return views;
  }

  private WebhookEndpoint find(UUID id) {
    return endpoints
        .findById(id)
        .orElseThrow(() -> new NotFoundException("No such webhook endpoint."));
  }

  private String checkUrl(@Nullable String url) {
    String clean = url == null ? "" : url.strip();
    if (clean.length() < 8 || clean.length() > 2000) {
      throw new InvalidInputException("Enter the full https:// address the webhook should go to.");
    }
    try {
      return http.check(clean).toString();
    } catch (UnsafeUrlException e) {
      throw new InvalidInputException(
          Objects.requireNonNullElse(e.getMessage(), "That address can't receive webhooks."));
    }
  }

  private static @Nullable String cleanDescription(@Nullable String description) {
    if (description == null || description.isBlank()) {
      return null;
    }
    String clean = description.strip();
    if (clean.length() > 200) {
      throw new InvalidInputException("Keep the description to 200 characters.");
    }
    return clean;
  }

  private static List<String> types(List<String> names) {
    Set<String> unique = new LinkedHashSet<>();
    for (String name : names) {
      WebhookEventType type =
          WebhookEventType.fromWireName(name)
              .filter(WebhookEventType::subscribable)
              .orElseThrow(() ->
                  new InvalidInputException(
                      "There's no webhook event called \"%s\".".formatted(name)));
      unique.add(type.wireName());
    }
    if (unique.isEmpty()) {
      throw new InvalidInputException("Choose at least one event to send.");
    }
    return List.copyOf(unique);
  }

  private static TenantContext requireStaff() {
    TenantContext caller = TenantContext.require();
    StudioRole role = caller.role();
    if (role == null || !role.includes(StudioRole.STAFF)) {
      throw new AccessDeniedException("Only staff can manage webhooks.");
    }
    return caller;
  }
}
