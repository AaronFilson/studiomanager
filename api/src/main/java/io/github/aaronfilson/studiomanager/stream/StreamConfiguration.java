package io.github.aaronfilson.studiomanager.stream;

import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.events.RoutingTarget;

/**
 * Sends {@link StudioStreamEvent}s to one topic, keyed by studio (not ordered; see its TOPIC), with the type in an
 * {@code event-type} header so a consumer can skip what it doesn't read without parsing it.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty("studiomanager.stream.enabled")
class StreamConfiguration {

    static final String TYPE_HEADER = "event-type";

    @Bean
    EventExternalizationConfiguration streamExternalization() {
        return EventExternalizationConfiguration.externalizing()
                .selectByType(StudioStreamEvent.class)
                .route(StudioStreamEvent.class,
                        event -> RoutingTarget.forTarget(StudioStreamEvent.TOPIC).andKey(event.studioId().toString()))
                .headers(StudioStreamEvent.class, event -> Map.of(TYPE_HEADER, event.type()))
                .build();
    }

    /** Created on startup if missing. Three partitions leave room for three consumers in a group. */
    @Bean
    NewTopic studioEvents() {
        return TopicBuilder.name(StudioStreamEvent.TOPIC).partitions(3).build();
    }

}
