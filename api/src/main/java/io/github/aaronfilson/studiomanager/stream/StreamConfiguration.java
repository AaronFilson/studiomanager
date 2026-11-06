package io.github.aaronfilson.studiomanager.stream;

import java.time.Duration;
import java.util.Map;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.events.RoutingTarget;
import tools.jackson.core.JacksonException;

/**
 * Sends {@link StudioStreamEvent}s to one topic, keyed by studio (not ordered; see its TOPIC), with
 * the type in an {@code event-type} header so a consumer can skip what it doesn't read without
 * parsing it. Consumers that fail on an event retry with back-off, then park it on the dead-letter
 * topic.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty("studiomanager.stream.enabled")
class StreamConfiguration {

  static final String TYPE_HEADER = "event-type";

  /** Where an event goes after its retries run out, with the reason in Spring Kafka's headers. */
  static final String DEAD_LETTER_TOPIC = StudioStreamEvent.TOPIC + ".dlt";

  /** How long the topic keeps events. {@link ActivityProjection#REMEMBER_SEEN} must be longer. */
  static final Duration RETENTION = Duration.ofDays(7);

  @Bean
  EventExternalizationConfiguration streamExternalization() {
    return EventExternalizationConfiguration.externalizing()
        .selectByType(StudioStreamEvent.class)
        .route(
            StudioStreamEvent.class,
            event ->
                RoutingTarget.forTarget(StudioStreamEvent.TOPIC)
                    .andKey(event.studioId().toString()))
        .headers(StudioStreamEvent.class, event -> Map.of(TYPE_HEADER, event.type()))
        .build();
  }

  /** Created on startup if missing. Three partitions leave room for three consumers in a group. */
  @Bean
  NewTopic studioEvents() {
    return TopicBuilder.name(StudioStreamEvent.TOPIC)
        .partitions(3)
        .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(RETENTION.toMillis()))
        .build();
  }

  /** Kept for longer than the topic, so someone has time to look. */
  @Bean
  NewTopic studioEventsDeadLetters() {
    return TopicBuilder.name(DEAD_LETTER_TOPIC)
        .partitions(1)
        .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(30).toMillis()))
        .build();
  }

  /**
   * Retries a failing event 6 times over about a minute (1 s doubling to 30 s), which rides out a
   * database restart, then sends it to the dead-letter topic and moves on. Bad data (unreadable
   * JSON, a mislabelled studio, a malformed id) goes there at once, because retrying can't help.
   * Spring Boot hands this to every listener container.
   */
  @Bean
  DefaultErrorHandler streamErrorHandler(KafkaOperations<Object, Object> kafka) {
    DeadLetterPublishingRecoverer deadLetters =
        new DeadLetterPublishingRecoverer(
            kafka, (record, exception) -> new TopicPartition(DEAD_LETTER_TOPIC, -1));
    ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(6);
    backOff.setInitialInterval(1_000);
    backOff.setMultiplier(2);
    backOff.setMaxInterval(30_000);
    DefaultErrorHandler handler = new DefaultErrorHandler(deadLetters, backOff);
    handler.addNotRetryableExceptions(
        JacksonException.class,
        DeserializationException.class,
        IllegalArgumentException.class,
        ActivityConsumer.MislabelledEventException.class);
    return handler;
  }
}
