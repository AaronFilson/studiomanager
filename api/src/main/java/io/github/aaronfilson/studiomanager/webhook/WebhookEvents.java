package io.github.aaronfilson.studiomanager.webhook;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.StudioService;
import io.github.aaronfilson.studiomanager.studio.StudioView;

import org.springframework.jdbc.core.simple.JdbcClient;
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
    static final java.time.Duration FIRST_ATTEMPT_GRACE = java.time.Duration.ofMinutes(1);

    private final JdbcClient jdbc;
    private final StudioService studios;
    private final JsonMapper json;
    private final WebhookSender sender;

    WebhookEvents(JdbcClient jdbc, StudioService studios, JsonMapper json, WebhookSender sender) {
        this.jdbc = jdbc;
        this.studios = studios;
        this.json = json;
        this.sender = sender;
    }

    /** An event, sent to every enabled endpoint subscribed to its type. Nothing is stored if there are none. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void publish(WebhookEventType type, String dedupeKey, Map<String, Object> data) {
        List<UUID> endpoints = jdbc.sql("select id from webhook_endpoint where enabled and :type = any(event_types)")
                .param("type", type.wireName())
                .query(UUID.class).list();
        if (endpoints.isEmpty()) {
            return;
        }
        record(type, dedupeKey, data, endpoints).ifPresent(event -> {
            TenantContext caller = TenantContext.require();
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        event.deliveryIds().forEach(id -> sender.attemptSoon(caller, id));
                    }
                });
            }
        });
    }

    /**
     * Stores the event and a delivery to each of {@code endpointIds}, without sending. Returns
     * empty if an event with this key was already recorded.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    Optional<Recorded> record(WebhookEventType type, String dedupeKey, Map<String, Object> data,
            List<UUID> endpointIds) {
        StudioView studio = studios.current();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        UUID eventId = Uuids.v7();
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("id", wireId(eventId));
        envelope.put("type", type.wireName());
        envelope.put("version", 1);
        envelope.put("created_at", now.truncatedTo(ChronoUnit.SECONDS).toString());
        envelope.put("studio", Map.of("id", studio.id().toString(), "name", studio.name()));
        envelope.put("data", data);
        String body = json.writeValueAsString(envelope);
        Optional<UUID> stored = jdbc.sql("""
                insert into webhook_event (id, studio_id, type, dedupe_key, body, created_at)
                values (:id, :studioId, :type, :key, :body, :now)
                on conflict (studio_id, dedupe_key) do nothing
                returning id
                """)
                .param("id", eventId)
                .param("studioId", studio.id())
                .param("type", type.wireName())
                .param("key", dedupeKey)
                .param("body", body)
                .param("now", now.atOffset(ZoneOffset.UTC))
                .query(UUID.class).optional();
        if (stored.isEmpty()) {
            return Optional.empty();
        }
        List<UUID> deliveries = new ArrayList<>();
        for (UUID endpointId : endpointIds) {
            deliveries.add(addDelivery(studio.id(), endpointId, eventId, now));
        }
        return Optional.of(new Recorded(eventId, deliveries));
    }

    /** A new delivery of an event already stored, such as a resend. */
    UUID addDelivery(UUID studioId, UUID endpointId, UUID eventId, Instant now) {
        UUID id = Uuids.v7();
        jdbc.sql("""
                insert into webhook_delivery (id, studio_id, endpoint_id, event_id, status, next_attempt_at, created_at)
                values (:id, :studioId, :endpointId, :eventId, 'PENDING', :next, :now)
                """)
                .param("id", id)
                .param("studioId", studioId)
                .param("endpointId", endpointId)
                .param("eventId", eventId)
                .param("next", now.plus(FIRST_ATTEMPT_GRACE).atOffset(ZoneOffset.UTC))
                .param("now", now.atOffset(ZoneOffset.UTC))
                .update();
        return id;
    }

    /** The id receivers see, in the payload and the {@code webhook-id} header. */
    static String wireId(UUID eventId) {
        return "evt_" + eventId.toString().replace("-", "");
    }

    record Recorded(UUID eventId, List<UUID> deliveryIds) {
    }

}
