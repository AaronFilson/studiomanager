package io.github.aaronfilson.studiomanager.stream;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the studio event stream into {@link ActivityProjection}, in its own consumer group, so it
 * reads every event whatever else consumes the topic.
 *
 * <p>Each event is applied inside its studio, bound before the transaction starts (RLS reads the
 * studio then). The studio comes from the record's key, and must match the {@code studioId} in the
 * body, or the event is refused: a mislabelled record must not land in another studio's totals.
 *
 * <p>Failures are retried with back-off, then sent to the dead-letter topic (see
 * {@link StreamConfiguration}), so a database restart delays events rather than losing them.
 *
 * <p>It acts as {@link #NO_ACCOUNT}, which owns nothing, so it can reach only the studio's rows. A
 * first-class system principal replaces this stand-in (docs/planning/12-system-principal-plan.md).
 */
@Component
@ConditionalOnBooleanProperty("studiomanager.stream.enabled")
class ActivityConsumer {

    private static final Logger log = LoggerFactory.getLogger(ActivityConsumer.class);

    static final String GROUP = "studiomanager-activity";

    /** The account the consumer acts as: none. No row belongs to it. */
    static final UUID NO_ACCOUNT = new UUID(0, 0);

    private final ActivityProjection projection;
    private final JsonMapper json;

    ActivityConsumer(ActivityProjection projection, JsonMapper json) {
        this.projection = projection;
        this.json = json;
    }

    @KafkaListener(topics = StudioStreamEvent.TOPIC, groupId = GROUP)
    void on(ConsumerRecord<String, byte[]> record) {
        Header typeHeader = record.headers().lastHeader(StreamConfiguration.TYPE_HEADER);
        if (typeHeader == null || record.key() == null) {
            log.warn("Skipping a record at {}-{}@{} with no key or event-type", record.topic(), record.partition(),
                    record.offset());
            return;
        }
        String type = new String(typeHeader.value(), StandardCharsets.UTF_8);
        if (!ActivityProjection.TYPES.contains(type)) {
            return;
        }
        UUID studioId = UUID.fromString(record.key());
        JsonNode event = json.readTree(record.value());
        JsonNode bodyStudio = event.get("studioId");
        if (bodyStudio == null || !studioId.toString().equals(bodyStudio.asString())) {
            // Not retried: it would be wrong every time. The error handler sends it to the dead-letter topic.
            throw new MislabelledEventException("Record key %s doesn't match the event's studioId %s"
                    .formatted(studioId, bodyStudio));
        }
        TenantContext as = TenantContext.forAccount(NO_ACCOUNT).inStudio(studioId, StudioRole.STAFF);
        TenantContext.callAs(as, () -> projection.apply(studioId, type, event));
    }

    /** A record whose key and body name different studios. */
    static class MislabelledEventException extends RuntimeException {

        MislabelledEventException(String message) {
            super(message);
        }

    }

}
