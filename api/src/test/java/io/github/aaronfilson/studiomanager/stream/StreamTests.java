package io.github.aaronfilson.studiomanager.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.SystemActor;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.json.JsonMapper;

/** The stream end to end: real work in the API, read back off a real Kafka topic. */
@IntegrationTest
@Import(StreamTests.Broker.class)
@TestPropertySource(properties = "studiomanager.stream.enabled=true")
class StreamTests {

  private static final Duration WAIT = Duration.ofSeconds(20);

  @TestConfiguration(proxyBeanMethods = false)
  static class Broker {

    @Bean
    KafkaContainer kafka() {
      return new KafkaContainer(DockerImageName.parse("apache/kafka-native:4.1.0"))
          .withStartupTimeout(Duration.ofMinutes(3));
    }

    @Bean
    DynamicPropertyRegistrar kafkaServers(KafkaContainer kafka) {
      return registry -> registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
  }

  @Autowired MockMvcTester mvc;

  @Autowired JdbcClient jdbc;

  @Autowired TransactionTemplate transactions;

  @Autowired ApplicationEventPublisher events;

  @Autowired ActivityProjection projection;

  @Autowired JsonMapper json;

  @Autowired KafkaTemplate<Object, Object> kafka;

  @Autowired MeterRegistry meters;

  @Value("${spring.kafka.bootstrap-servers}")
  String bootstrapServers;

  private final TestUser owner = TestUser.named("Olive");
  private final TestUser member = TestUser.named("Mo");
  private UUID studioId;
  private UUID ownerId;

  @BeforeEach
  void setUp() {
    studioId =
        UUID.fromString(
            read(
                post(
                    owner,
                    "/api/v1/studios",
                    """
                    {"name": "Clay Club", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
                    """),
                "$.id"));
    ownerId =
        UUID.fromString(
            read(mvc.get().uri("/api/v1/me").with(owner.token()).exchange(), "$.account.id"));
  }

  @Test
  void aJoinAndASaleArriveKeyedByStudioWithIdsOnly() throws Exception {
    String memberId = join(member);
    String clay =
        read(
            post(
                owner,
                path("/supplies"),
                """
                {"name": "Speckled buff", "category": "CLAY", "unit": "25 lb bag", "salePrice": "30.00",
                 "taxCategory": "RETAIL"}
                """),
            "$.id");
    assertThat(
            post(
                owner,
                path("/supplies/" + clay + "/movements"),
                "{\"kind\": \"RECEIVED\", \"quantity\": \"10\"}"))
        .hasStatusOk();
    assertThat(
            post(
                owner,
                path("/supplies/" + clay + "/sales"),
                "{\"memberId\": \"%s\", \"quantity\": \"2\"}".formatted(memberId)))
        .hasStatusOk();

    List<ConsumerRecord<String, String>> mine = readStudio(2);
    assertThat(mine).extracting(ConsumerRecord::key).containsOnly(studioId.toString());
    // Each is sent asynchronously after its own commit, so arrival order isn't promised, even
    // within a studio's partition. Consumers order by occurredAt and dedupe on id.
    assertThat(mine)
        .extracting(StreamTests::typeHeader)
        .containsExactlyInAnyOrder("member.joined", "fee.charged");
    mine.sort(java.util.Comparator.comparing(StreamTests::typeHeader).reversed());

    String joined = mine.get(0).value();
    assertThat((String) JsonPath.read(joined, "$.memberId")).isEqualTo(memberId);
    assertThat((Integer) JsonPath.read(joined, "$.version")).isEqualTo(1);

    String fee = mine.get(1).value();
    assertThat((String) JsonPath.read(fee, "$.type")).isEqualTo("fee.charged");
    assertThat((String) JsonPath.read(fee, "$.source")).isEqualTo("supply_sale");
    assertThat((String) JsonPath.read(fee, "$.currency")).isEqualTo("USD");
    assertThat((String) JsonPath.read(fee, "$.amount")).matches("\\d+\\.\\d{2}");
    // No names or addresses on the stream, unlike webhooks
    assertThat(joined + fee).doesNotContain(member.email()).doesNotContain("Mo");
    // Nor Java class names
    assertThat(mine.get(0).headers().lastHeader("__TypeId__")).isNull();
  }

  @Test
  void theConsumerTotalsTheWeekForStaffOnly() throws Exception {
    String memberId = join(member);
    String clay =
        read(
            post(
                owner,
                path("/supplies"),
                """
                {"name": "Speckled buff", "category": "CLAY", "unit": "25 lb bag", "salePrice": "30.00"}
                """),
            "$.id");
    assertThat(
            post(
                owner,
                path("/supplies/" + clay + "/movements"),
                "{\"kind\": \"RECEIVED\", \"quantity\": \"10\"}"))
        .hasStatusOk();
    assertThat(
            post(
                owner,
                path("/supplies/" + clay + "/sales"),
                "{\"memberId\": \"%s\", \"quantity\": \"2\"}".formatted(memberId)))
        .hasStatusOk();

    // Both events, whichever arrives first, and whichever week each falls in
    await()
        .atMost(WAIT)
        .untilAsserted(() -> {
          String body = activity();
          assertThat(sum(body, "$.weeks[*].membersJoined")).isEqualTo(1);
          assertThat(sum(body, "$.weeks[*].fees[*].count")).isEqualTo(1);
        });
    String body = activity();
    assertThat((Boolean) JsonPath.read(body, "$.streaming")).isTrue();
    assertThat(JsonPath.<List<String>>read(body, "$.weeks[*].fees[*].amount"))
        .containsExactly("60.00");
    assertThat(JsonPath.<List<String>>read(body, "$.weeks[*].fees[*].currency"))
        .containsExactly("USD");

    assertThat(mvc.get().uri(path("/activity/weekly")).with(member.token()))
        .hasStatus(HttpStatus.FORBIDDEN);
  }

  @Test
  void anEventDeliveredTwiceCountsOnceAndARenamedKilnStaysOneKiln() throws Exception {
    UUID kilnId = UUID.randomUUID();
    Instant at = Instant.now();
    var first =
        new StudioStreamEvent.FiringCompleted(
            studioId, at, UUID.randomUUID(), "bisque", kilnId, "Big Skutt", 24, 5);
    var renamed =
        new StudioStreamEvent.FiringCompleted(
            studioId, at, UUID.randomUUID(), "glaze", kilnId, "Skutt 1027", 10, 2);
    assertThat(applyAsConsumer(first)).isTrue();
    assertThat(applyAsConsumer(first)).isFalse();
    assertThat(applyAsConsumer(renamed)).isTrue();

    String body = activity();
    assertThat((Integer) JsonPath.read(body, "$.weeks[0].firings")).isEqualTo(2);
    assertThat((Integer) JsonPath.read(body, "$.weeks[0].piecesFired")).isEqualTo(34);
    assertThat(JsonPath.<List<String>>read(body, "$.weeks[0].kilns[*].kilnId"))
        .containsExactly(kilnId.toString());
    assertThat((String) JsonPath.read(body, "$.weeks[0].kilns[0].kiln")).isEqualTo("Skutt 1027");
  }

  @Test
  void aTypeItDoesntReadIsLeftForLater() throws Exception {
    UUID eventId = UUID.randomUUID();
    var unknown =
        json.readTree(
            """
            {"id": "%s", "studioId": "%s", "occurredAt": "%s"}
            """
                .formatted(eventId, studioId, Instant.now()));
    TenantContext consumer = TenantContext.system(SystemActor.ACTIVITY_CONSUMER, studioId);
    assertThat(
            TenantContext.callAs(
                consumer, () -> projection.apply(studioId, "pieces.ready", unknown)))
        .isFalse();
    Integer seen =
        jdbc.sql("select count(*) from stream_event_seen where event_id = ?")
            .param(eventId)
            .query(Integer.class)
            .single();
    assertThat(seen).isZero();
  }

  @Test
  void aRecordWhoseKeyAndBodyDisagreeGoesToTheDeadLetterTopic() throws Exception {
    TestUser other = TestUser.named("Ozzie");
    UUID otherStudio =
        UUID.fromString(
            read(
                post(
                    other,
                    "/api/v1/studios",
                    """
                    {"name": "Other Clay", "unitSystem": "METRIC", "timeZone": "Europe/London", "country": "GB"}
                    """),
                "$.id"));
    var event = new StudioStreamEvent.MemberJoined(studioId, Instant.now(), UUID.randomUUID());
    var record =
        new ProducerRecord<Object, Object>(
            StudioStreamEvent.TOPIC, otherStudio.toString(), json.writeValueAsBytes(event));
    record
        .headers()
        .add(StreamConfiguration.TYPE_HEADER, event.type().getBytes(StandardCharsets.UTF_8));
    kafka.send(record).get();

    List<ConsumerRecord<String, String>> parked =
        read(StreamConfiguration.DEAD_LETTER_TOPIC, r -> otherStudio.toString().equals(r.key()), 1);
    assertThat(parked.getFirst().value()).contains(event.id().toString());
    // Counted nowhere
    assertThat(
            mvc.get()
                .uri("/api/v1/studios/" + otherStudio + "/activity/weekly")
                .with(other.token()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.weeks")
        .asArray()
        .isEmpty();
    assertThat(sum(activity(), "$.weeks[*].membersJoined")).isZero();
  }

  @Test
  void anEventReadsBackAsItself() throws Exception {
    // The outbox stores each event as JSON and reads it back to resend it
    var event =
        new StudioStreamEvent.FeeCharged(
            studioId,
            Instant.now(),
            UUID.randomUUID(),
            "piece",
            UUID.randomUUID(),
            "12.50",
            "USD",
            null);
    byte[] written = json.writeValueAsBytes(event);
    assertThat(JsonPath.<String>read(new String(written, StandardCharsets.UTF_8), "$.type"))
        .isEqualTo("fee.charged");
    assertThat(json.readValue(written, StudioStreamEvent.FeeCharged.class)).isEqualTo(event);
  }

  @Test
  void staffInMoreThanOneStudioSeeTheirStudiosActivity() {
    // RLS lets a person read every studio they belong to; the projection must still pick one
    assertThat(
            post(
                owner,
                "/api/v1/studios",
                """
                {"name": "Second Clay", "unitSystem": "METRIC", "timeZone": "Europe/London", "country": "GB"}
                """))
        .hasStatus2xxSuccessful();
    assertThat(mvc.get().uri(path("/activity/weekly")).with(owner.token())).hasStatusOk();
  }

  @Test
  void studiosStayApart() throws Exception {
    assertThat(
            applyAsConsumer(
                new StudioStreamEvent.MemberJoined(studioId, Instant.now(), UUID.randomUUID())))
        .isTrue();
    TestUser other = TestUser.named("Ozzie");
    String otherStudio =
        read(
            post(
                other,
                "/api/v1/studios",
                """
                {"name": "Other Clay", "unitSystem": "METRIC", "timeZone": "Europe/London", "country": "GB"}
                """),
            "$.id");
    assertThat(
            mvc.get()
                .uri("/api/v1/studios/" + otherStudio + "/activity/weekly")
                .with(other.token()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.weeks")
        .asArray()
        .isEmpty();
  }

  @Test
  void workThatRolledBackQueuesNothing() {
    UUID ghost = UUID.randomUUID();
    TenantContext.callAs(
        TenantContext.forAccount(ownerId).inStudio(studioId, StudioRole.OWNER),
        () ->
            transactions.execute(tx -> {
              events.publishEvent(
                  new StudioStreamEvent.MemberJoined(studioId, Instant.now(), ghost));
              tx.setRollbackOnly();
              return null;
            }));
    Integer queued =
        jdbc.sql("select count(*) from event_publication where serialized_event like ?")
            .param("%" + ghost + "%")
            .query(Integer.class)
            .single();
    assertThat(queued).isZero();
  }

  @Test
  void theConsumersLagIsMeasured() {
    // Spring Boot binds the listener's Kafka client metrics to Micrometer; lag is the one to watch
    await()
        .atMost(WAIT)
        .untilAsserted(() ->
            assertThat(meters.find("kafka.consumer.fetch.manager.records.lag.max").meters())
                .isNotEmpty());
  }

  @Test
  void theSameHappeningHasTheSameId() {
    UUID memberId = UUID.randomUUID();
    Instant at = Instant.now();
    assertThat(new StudioStreamEvent.MemberJoined(studioId, at, memberId).id())
        .isEqualTo(new StudioStreamEvent.MemberJoined(studioId, at.plusSeconds(60), memberId).id())
        .isNotEqualTo(new StudioStreamEvent.MemberJoined(UUID.randomUUID(), at, memberId).id());
  }

  /** Reads the topic from the start until this studio's first {@code count} events are in. */
  private List<ConsumerRecord<String, String>> readStudio(int count) {
    return read(StudioStreamEvent.TOPIC, record -> studioId.toString().equals(record.key()), count);
  }

  /** Reads a topic from the start until {@code count} records that match are in. */
  private List<ConsumerRecord<String, String>> read(
      String topic, Predicate<ConsumerRecord<String, String>> match, int count) {
    Map<String, Object> config =
        Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
            bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG,
            "test-" + UUID.randomUUID(),
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
            "earliest");
    List<ConsumerRecord<String, String>> found = new ArrayList<>();
    try (KafkaConsumer<String, String> consumer =
        new KafkaConsumer<>(config, new StringDeserializer(), new StringDeserializer())) {
      consumer.subscribe(List.of(topic));
      Instant until = Instant.now().plus(WAIT);
      while (found.size() < count && Instant.now().isBefore(until)) {
        consumer
            .poll(Duration.ofMillis(500))
            .forEach(record -> {
              if (match.test(record)) {
                found.add(record);
              }
            });
      }
    }
    assertThat(found).as("records on %s", topic).hasSize(count);
    return found;
  }

  /** Applies an event the way the consumer does, bound to its studio. */
  private boolean applyAsConsumer(StudioStreamEvent event) throws Exception {
    var tree = json.readTree(json.writeValueAsBytes(event));
    TenantContext consumer = TenantContext.system(SystemActor.ACTIVITY_CONSUMER, event.studioId());
    return TenantContext.callAs(
        consumer, () -> projection.apply(event.studioId(), event.type(), tree));
  }

  private String activity() throws Exception {
    MvcTestResult result = mvc.get().uri(path("/activity/weekly")).with(owner.token()).exchange();
    assertThat(result).hasStatusOk();
    return result.getResponse().getContentAsString();
  }

  private static int sum(String body, String jsonPath) {
    List<Integer> values = JsonPath.read(body, jsonPath);
    return values.stream().mapToInt(Integer::intValue).sum();
  }

  private static String typeHeader(ConsumerRecord<String, String> record) {
    var header = record.headers().lastHeader(StreamConfiguration.TYPE_HEADER);
    return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
  }

  private String join(TestUser who) throws Exception {
    String code = read(post(owner, path("/invites"), "{\"role\": \"MEMBER\"}"), "$.code");
    assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(who.token()))
        .hasStatusOk();
    return read(mvc.get().uri("/api/v1/me").with(who.token()).exchange(), "$.account.id");
  }

  private MvcTestResult post(TestUser user, String uri, String body) {
    return mvc.post()
        .uri(uri)
        .with(user.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .exchange();
  }

  private String path(String suffix) {
    return "/api/v1/studios/" + studioId + suffix;
  }

  private static String read(MvcTestResult result, String jsonPath) {
    try {
      return JsonPath.read(result.getResponse().getContentAsString(), jsonPath).toString();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
