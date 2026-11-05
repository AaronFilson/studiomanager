package io.github.aaronfilson.studiomanager.stream;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the studio event stream into {@link ActivityProjection}. Its own consumer group, so it
 * reads every event whatever else consumes the topic, and a new group starts from the earliest
 * offset, rebuilding the totals from what the topic still holds.
 *
 * <p>Each event is applied inside its studio, bound before the transaction starts (RLS reads the
 * studio then). It acts as no one: {@link #NO_ACCOUNT} owns nothing, so it can reach only the
 * studio's rows. A failure is retried by Spring Kafka's default error handler, then logged and
 * skipped, so one bad event can't stop the stream.
 */
@Component
@ConditionalOnBooleanProperty("studiomanager.stream.enabled")
class ActivityConsumer {

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
        Header type = record.headers().lastHeader(StreamConfiguration.TYPE_HEADER);
        if (type == null || record.key() == null) {
            return;
        }
        UUID studioId = UUID.fromString(record.key());
        TenantContext as = TenantContext.forAccount(NO_ACCOUNT).inStudio(studioId, StudioRole.STAFF);
        TenantContext.callAs(as, () -> projection.apply(studioId,
                new String(type.value(), StandardCharsets.UTF_8), json.readTree(record.value())));
    }

}
