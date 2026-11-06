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
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
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

  private final JdbcClient jdbc;
  private final TransactionOperations transactions;
  private final SafeHttpClient http;
  private final WebhookSecrets secrets;
  private final WebhookEvents events;
  private final WebhookSender sender;
  private final StudioService studios;

  WebhookEndpoints(
      JdbcClient jdbc,
      TransactionOperations transactions,
      SafeHttpClient http,
      WebhookSecrets secrets,
      WebhookEvents events,
      WebhookSender sender,
      StudioService studios) {
    this.jdbc = jdbc;
    this.transactions = transactions;
    this.http = http;
    this.secrets = secrets;
    this.events = events;
    this.sender = sender;
    this.studios = studios;
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
    return jdbc.sql(SELECT_ENDPOINT + " order by created_at")
        .query(WebhookEndpoints::endpoint)
        .list();
  }

  @Transactional
  public CreatedEndpoint create(String url, @Nullable String description, List<String> eventTypes) {
    TenantContext caller = requireStaff();
    Integer count = jdbc.sql("select count(*) from webhook_endpoint").query(Integer.class).single();
    if (count >= MAX_ENDPOINTS) {
      throw new InvalidInputException(
          "A studio can have up to %d webhook endpoints. Delete one first."
              .formatted(MAX_ENDPOINTS));
    }
    String cleanUrl = checkUrl(url);
    UUID id = Uuids.v7();
    String secret = WebhookSecrets.newSecret();
    jdbc.sql(
            """
            insert into webhook_endpoint (id, studio_id, url, description, event_types, secret, created_by, created_at)
            values (:id, :studioId, :url, :description, :types, :secret, :by, :now)
            """)
        .param("id", id)
        .param("studioId", caller.studioId())
        .param("url", cleanUrl)
        .param("description", cleanDescription(description))
        .param("types", types(eventTypes))
        .param("secret", secrets.encrypt(id, secret))
        .param("by", caller.accountId())
        .param("now", now().atOffset(ZoneOffset.UTC))
        .update();
    return new CreatedEndpoint(get(id), secret);
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
    EndpointView current = get(id);
    jdbc.sql(
            """
            update webhook_endpoint set url = :url, description = :description, event_types = :types,
                   enabled = :enabled,
                   consecutive_failures = case when :reenable then 0 else consecutive_failures end,
                   failing_since = case when :reenable then null else failing_since end,
                   disabled_reason = case when :enabled then null else disabled_reason end
            where id = :id
            """)
        .param("url", url == null ? current.url() : checkUrl(url))
        .param(
            "description",
            description == null ? current.description() : cleanDescription(description))
        .param(
            "types",
            eventTypes == null
                ? current.eventTypes().stream()
                    .map(WebhookEventType::wireName)
                    .toArray(String[]::new)
                : types(eventTypes))
        .param("enabled", enabled == null ? current.enabled() : enabled)
        .param("reenable", enabled != null && enabled && !current.enabled())
        .param("id", id)
        .update();
    return get(id);
  }

  @Transactional
  public void delete(UUID id) {
    requireStaff();
    if (jdbc.sql("delete from webhook_endpoint where id = :id").param("id", id).update() == 0) {
      throw new NotFoundException("No such webhook endpoint.");
    }
  }

  /** A new secret. The old one still signs, alongside it, for {@link #ROTATION_OVERLAP}. */
  @Transactional
  public RotatedSecret rotateSecret(UUID id) {
    requireStaff();
    byte[] old =
        jdbc.sql("select secret from webhook_endpoint where id = :id for update")
            .param("id", id)
            .query(byte[].class)
            .optional()
            .orElseThrow(() -> new NotFoundException("No such webhook endpoint."));
    String secret = WebhookSecrets.newSecret();
    Instant until = now().plus(ROTATION_OVERLAP);
    jdbc.sql(
            """
            update webhook_endpoint set secret = :secret, previous_secret = :old, previous_secret_until = :until
            where id = :id
            """)
        .param("secret", secrets.encrypt(id, secret))
        .param("old", old)
        .param("until", until.atOffset(ZoneOffset.UTC))
        .param("id", id)
        .update();
    return new RotatedSecret(secret, until);
  }

  /** Sends a {@code ping} now, even to a disabled endpoint's URL, and returns how it went. */
  public DeliveryView test(UUID id) {
    TenantContext caller = requireStaff();
    UUID deliveryId =
        transactions.execute(tx -> {
          EndpointView endpoint = get(id);
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
        });
    sender.attempt(caller, deliveryId);
    return transactions.execute(tx -> delivery(deliveryId));
  }

  /** The last 30 days of deliveries to an endpoint, newest first. */
  @Transactional(readOnly = true)
  public Capped<DeliveryView> deliveries(UUID endpointId) {
    requireStaff();
    get(endpointId);
    Capped<UUID> ids =
        Capped.fromFetched(
            jdbc.sql(
                    """
                    select id from webhook_delivery where endpoint_id = :id and created_at > :since
                    order by created_at desc limit :fetchSize
                    """)
                .param("id", endpointId)
                .param("since", now().minus(LOG_DAYS).atOffset(ZoneOffset.UTC))
                .param("fetchSize", Capped.fetchSize(DELIVERY_LOG_LIMIT))
                .query(UUID.class)
                .list(),
            DELIVERY_LOG_LIMIT);
    return ids.map(this::delivery);
  }

  /**
   * The same event again, as a new delivery, sent now. Receivers see the same {@code webhook-id}.
   */
  public DeliveryView resend(UUID deliveryId) {
    TenantContext caller = requireStaff();
    UUID newId =
        transactions.execute(tx -> {
          Original original =
              jdbc.sql(
                      """
                      select d.endpoint_id, d.event_id, e.enabled from webhook_delivery d
                      join webhook_endpoint e on e.id = d.endpoint_id where d.id = :id
                      """)
                  .param("id", deliveryId)
                  .query(Original.class)
                  .optional()
                  .orElseThrow(() -> new NotFoundException("No such delivery."));
          if (!original.enabled()) {
            throw new InvalidInputException("Turn the endpoint on before resending to it.");
          }
          return events.addDelivery(
              caller.studioId(), original.endpointId(), original.eventId(), now());
        });
    sender.attempt(caller, newId);
    return transactions.execute(tx -> delivery(newId));
  }

  private record Original(UUID endpointId, UUID eventId, boolean enabled) {}

  private DeliveryView delivery(UUID id) {
    List<AttemptView> attempts =
        jdbc.sql(
                """
                select attempted_at, status_code, error, duration_ms, response_excerpt from webhook_attempt
                where delivery_id = :id order by attempted_at desc
                """)
            .param("id", id)
            .query((rs, row) ->
                new AttemptView(
                    instant(rs, "attempted_at"),
                    (Integer) rs.getObject("status_code"),
                    rs.getString("error"),
                    rs.getInt("duration_ms"),
                    rs.getString("response_excerpt")))
            .list();
    return jdbc.sql(
            """
            select d.id, ev.id as event_id, ev.type, d.status, d.attempts, d.created_at, d.next_attempt_at,
                   d.delivered_at
            from webhook_delivery d join webhook_event ev on ev.id = d.event_id
            where d.id = :id
            """)
        .param("id", id)
        .query((rs, row) ->
            new DeliveryView(
                rs.getObject("id", UUID.class),
                WebhookEvents.wireId(rs.getObject("event_id", UUID.class)),
                WebhookEventType.fromWireName(rs.getString("type")).orElseThrow(),
                rs.getString("status"),
                rs.getInt("attempts"),
                instant(rs, "created_at"),
                instantOrNull(rs, "next_attempt_at"),
                instantOrNull(rs, "delivered_at"),
                attempts))
        .optional()
        .orElseThrow(() -> new NotFoundException("No such delivery."));
  }

  private EndpointView get(UUID id) {
    return jdbc.sql(SELECT_ENDPOINT + " where id = :id")
        .param("id", id)
        .query(WebhookEndpoints::endpoint)
        .optional()
        .orElseThrow(() -> new NotFoundException("No such webhook endpoint."));
  }

  private static final String SELECT_ENDPOINT =
      """
      select id, url, description, event_types, enabled, disabled_reason, consecutive_failures,
             case when previous_secret_until > now() then previous_secret_until end as previous_secret_until,
             created_at
      from webhook_endpoint
      """;

  private static EndpointView endpoint(ResultSet rs, int row) throws SQLException {
    Array array = rs.getArray("event_types");
    List<WebhookEventType> types =
        Arrays.stream((String[]) array.getArray())
            .map(name -> WebhookEventType.fromWireName(name).orElse(null))
            .filter(java.util.Objects::nonNull)
            .toList();
    return new EndpointView(
        rs.getObject("id", UUID.class),
        rs.getString("url"),
        rs.getString("description"),
        types,
        rs.getBoolean("enabled"),
        rs.getString("disabled_reason"),
        rs.getInt("consecutive_failures"),
        instantOrNull(rs, "previous_secret_until"),
        instant(rs, "created_at"));
  }

  private String checkUrl(String url) {
    String clean = url == null ? "" : url.strip();
    if (clean.length() < 8 || clean.length() > 2000) {
      throw new InvalidInputException("Enter the full https:// address the webhook should go to.");
    }
    try {
      return http.check(clean).toString();
    } catch (UnsafeUrlException e) {
      throw new InvalidInputException(e.getMessage());
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

  private static String[] types(List<String> names) {
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
    return unique.toArray(String[]::new);
  }

  private static TenantContext requireStaff() {
    TenantContext caller = TenantContext.require();
    if (caller.role() == null || !caller.role().includes(StudioRole.STAFF)) {
      throw new AccessDeniedException("Only staff can manage webhooks.");
    }
    return caller;
  }

  private static Instant instant(ResultSet rs, String column) throws SQLException {
    return rs.getObject(column, OffsetDateTime.class).toInstant();
  }

  private static @Nullable Instant instantOrNull(ResultSet rs, String column) throws SQLException {
    OffsetDateTime at = rs.getObject(column, OffsetDateTime.class);
    return at == null ? null : at.toInstant();
  }

  private static Instant now() {
    return Instant.now().truncatedTo(ChronoUnit.MICROS);
  }
}
