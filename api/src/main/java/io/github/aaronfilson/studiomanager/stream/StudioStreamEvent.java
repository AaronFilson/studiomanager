package io.github.aaronfilson.studiomanager.stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * The stream contract: what the studio publishes to Kafka. Each type is versioned, and changes to
 * the domain events behind them don't change this unless the version does.
 *
 * <p>Unlike webhooks, these carry ids only, never names or email addresses: a topic is kept for
 * days and read by anything with access to it. A consumer that needs a name asks the API. (A kiln's
 * name is the one label carried, beside its id, so totals read well without a lookup.)
 *
 * <p>{@code id} comes from what happened, not from when it was sent, so a retried or repeated event
 * has the same id and consumers can drop duplicates. {@code type} and {@code version} are fixed per
 * record, written to JSON but never read back from it, so an event can't claim to be something it
 * isn't.
 */
public sealed interface StudioStreamEvent {

  /**
   * The one topic, keyed by studio, so a studio's events share a partition and one consumer.
   * Arrival order isn't promised: each event is sent after its own commit, asynchronously, and a
   * retried one arrives late. Consumers order by {@code occurredAt}.
   */
  String TOPIC = "studiomanager.studio-events";

  UUID id();

  @JsonProperty
  String type();

  @JsonProperty
  int version();

  UUID studioId();

  Instant occurredAt();

  static UUID idFor(String dedupeKey) {
    return UUID.nameUUIDFromBytes(dedupeKey.getBytes(StandardCharsets.UTF_8));
  }

  /** A kiln was unloaded. Counts, not the pieces: a consumer that wants those asks the API. */
  @JsonIgnoreProperties(
      value = {"type", "version"},
      allowGetters = true)
  record FiringCompleted(
      UUID id,
      UUID studioId,
      Instant occurredAt,
      UUID firingId,
      String kind,
      UUID kilnId,
      String kiln,
      int pieces,
      int makers)
      implements StudioStreamEvent {

    public static final String TYPE = "firing.completed";

    FiringCompleted(
        UUID studioId,
        Instant occurredAt,
        UUID firingId,
        String kind,
        UUID kilnId,
        String kiln,
        int pieces,
        int makers) {
      this(
          idFor(TYPE + ":" + firingId),
          studioId,
          occurredAt,
          firingId,
          kind,
          kilnId,
          kiln,
          pieces,
          makers);
    }

    @Override
    public String type() {
      return TYPE;
    }

    @Override
    public int version() {
      return 1;
    }
  }

  /** A charge to a member's balance. {@code source} says what for: "piece" or "supply_sale". */
  @JsonIgnoreProperties(
      value = {"type", "version"},
      allowGetters = true)
  record FeeCharged(
      UUID id,
      UUID studioId,
      Instant occurredAt,
      UUID memberId,
      String source,
      UUID sourceId,
      String amount,
      String currency,
      @Nullable String taxCategory)
      implements StudioStreamEvent {

    public static final String TYPE = "fee.charged";

    FeeCharged(
        UUID studioId,
        Instant occurredAt,
        UUID memberId,
        String source,
        UUID sourceId,
        String amount,
        String currency,
        @Nullable String taxCategory) {
      this(
          idFor(TYPE + ":" + source + ":" + sourceId),
          studioId,
          occurredAt,
          memberId,
          source,
          sourceId,
          amount,
          currency,
          taxCategory);
    }

    @Override
    public String type() {
      return TYPE;
    }

    @Override
    public int version() {
      return 1;
    }
  }

  /** Someone joined. Raised again if they accept a second invite, with the same id. */
  @JsonIgnoreProperties(
      value = {"type", "version"},
      allowGetters = true)
  record MemberJoined(UUID id, UUID studioId, Instant occurredAt, UUID memberId)
      implements StudioStreamEvent {

    public static final String TYPE = "member.joined";

    MemberJoined(UUID studioId, Instant occurredAt, UUID memberId) {
      this(idFor(TYPE + ":" + studioId + ":" + memberId), studioId, occurredAt, memberId);
    }

    @Override
    public String type() {
      return TYPE;
    }

    @Override
    public int version() {
      return 1;
    }
  }
}
