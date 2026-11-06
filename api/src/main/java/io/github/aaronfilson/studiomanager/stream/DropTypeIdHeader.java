package io.github.aaronfilson.studiomanager.stream;

import java.util.Map;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

/**
 * Removes the {@code __TypeId__} header that Modulith's Kafka externalizer adds. It holds a Java
 * class name, which would tie consumers to this codebase's packages; they read {@code event-type}
 * instead. Configured in application.yaml as a producer interceptor.
 */
public class DropTypeIdHeader implements ProducerInterceptor<Object, Object> {

  static final String TYPE_ID = "__TypeId__";

  @Override
  public ProducerRecord<Object, Object> onSend(ProducerRecord<Object, Object> record) {
    record.headers().remove(TYPE_ID);
    return record;
  }

  @Override
  public void onAcknowledgement(RecordMetadata metadata, Exception exception) {}

  @Override
  public void close() {}

  @Override
  public void configure(Map<String, ?> configs) {}
}
