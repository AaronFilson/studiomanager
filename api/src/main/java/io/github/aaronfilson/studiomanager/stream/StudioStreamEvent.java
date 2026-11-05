package io.github.aaronfilson.studiomanager.stream;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * The stream contract: what the studio publishes to Kafka. Each type is versioned, and changes to
 * the domain events behind them don't change this unless the version does.
 *
 * <p>Unlike webhooks, these carry ids only, never names or email addresses: a topic is kept for
 * days and read by anything with access to it. A consumer that needs a name asks the API.
 *
 * <p>{@code id} comes from what happened, not from when it was sent, so a retried or repeated
 * event has the same id and consumers can drop duplicates.
 */
public sealed interface StudioStreamEvent {

    /**
     * The one topic, keyed by studio, so a studio's events share a partition and one consumer.
     * Arrival order isn't promised: each event is sent after its own commit, asynchronously, and a
     * retried one arrives late. Consumers order by {@code occurredAt}.
     */
    String TOPIC = "studiomanager.studio-events";

    UUID id();

    String type();

    int version();

    UUID studioId();

    Instant occurredAt();

    static UUID idFor(String dedupeKey) {
        return UUID.nameUUIDFromBytes(dedupeKey.getBytes(StandardCharsets.UTF_8));
    }

    /** A kiln was unloaded. Counts, not the pieces: a consumer that wants those asks the API. */
    record FiringCompleted(UUID id, String type, int version, UUID studioId, Instant occurredAt, UUID firingId,
            String kind, String kiln, int pieces, int makers) implements StudioStreamEvent {

        public static final String TYPE = "firing.completed";

        FiringCompleted(UUID studioId, Instant occurredAt, UUID firingId, String kind, String kiln, int pieces,
                int makers) {
            this(idFor(TYPE + ":" + firingId), TYPE, 1, studioId, occurredAt, firingId, kind, kiln, pieces, makers);
        }
    }

    /** A charge to a member's balance. {@code source} says what for: "piece" or "supply_sale". */
    record FeeCharged(UUID id, String type, int version, UUID studioId, Instant occurredAt, UUID memberId,
            String source, UUID sourceId, String amount, String currency, @Nullable String taxCategory)
            implements StudioStreamEvent {

        public static final String TYPE = "fee.charged";

        FeeCharged(UUID studioId, Instant occurredAt, UUID memberId, String source, UUID sourceId, String amount,
                String currency, @Nullable String taxCategory) {
            this(idFor(TYPE + ":" + source + ":" + sourceId), TYPE, 1, studioId, occurredAt, memberId, source, sourceId,
                    amount, currency, taxCategory);
        }
    }

    /** Someone joined. Raised again if they accept a second invite, with the same id. */
    record MemberJoined(UUID id, String type, int version, UUID studioId, Instant occurredAt, UUID memberId)
            implements StudioStreamEvent {

        public static final String TYPE = "member.joined";

        MemberJoined(UUID studioId, Instant occurredAt, UUID memberId) {
            this(idFor(TYPE + ":" + studioId + ":" + memberId), TYPE, 1, studioId, occurredAt, memberId);
        }
    }

}
