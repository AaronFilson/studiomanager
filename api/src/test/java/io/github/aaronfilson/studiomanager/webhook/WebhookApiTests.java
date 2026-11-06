package io.github.aaronfilson.studiomanager.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpServer;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/** Webhooks end to end, to a receiver on localhost (allowed in tests only). */
@IntegrationTest
class WebhookApiTests {

  private static final Duration WAIT = Duration.ofSeconds(10);

  @Autowired MockMvcTester mvc;

  @Autowired JdbcClient jdbc;

  @Autowired TransactionTemplate transactions;

  @Autowired WebhookEvents events;

  @Autowired WebhookSweep sweep;

  @Autowired JsonMapper json;

  private final TestUser owner = TestUser.named("Olive");
  private final TestUser member = TestUser.named("Mo");
  private UUID studioId;
  private UUID ownerId;
  private HttpServer receiver;
  private final ConcurrentLinkedQueue<Received> received = new ConcurrentLinkedQueue<>();
  private final AtomicInteger answerWith = new AtomicInteger(200);

  record Received(Map<String, String> headers, String body) {}

  @BeforeEach
  void setUp() throws Exception {
    receiver = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    receiver.createContext(
        "/hook",
        exchange -> {
          String body =
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          received.add(
              new Received(
                  Map.of(
                      "webhook-id", exchange.getRequestHeaders().getFirst("webhook-id"),
                      "webhook-timestamp",
                          exchange.getRequestHeaders().getFirst("webhook-timestamp"),
                      "webhook-signature",
                          exchange.getRequestHeaders().getFirst("webhook-signature")),
                  body));
          byte[] answer = "thanks".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(answerWith.get(), answer.length);
          exchange.getResponseBody().write(answer);
          exchange.close();
        });
    receiver.start();
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

  @AfterEach
  void stop() {
    receiver.stop(0);
  }

  @Test
  void aJoiningMemberIsPostedSignedWithTheSecretShownOnce() throws Exception {
    MvcTestResult created =
        post(
            owner,
            path("/webhooks/endpoints"),
            """
            {"url": "%s", "description": "Zapier", "eventTypes": ["member.joined"]}
            """
                .formatted(url()));
    assertThat(created).hasStatus(HttpStatus.CREATED);
    String secret = read(created, "$.secret");
    assertThat(secret).startsWith("whsec_");
    // Never shown again
    assertThat(mvc.get().uri(path("/webhooks/endpoints")).with(owner.token()))
        .hasStatusOk()
        .bodyText()
        .doesNotContain(secret);

    join(member);

    await().atMost(WAIT).until(() -> !received.isEmpty());
    Received hook = received.peek();
    assertThat(hook.headers().get("webhook-signature"))
        .isEqualTo(
            WebhookSigner.sign(
                secret,
                hook.headers().get("webhook-id"),
                Long.parseLong(hook.headers().get("webhook-timestamp")),
                hook.body()));
    assertThat((String) JsonPath.read(hook.body(), "$.type")).isEqualTo("member.joined");
    assertThat((String) JsonPath.read(hook.body(), "$.id"))
        .isEqualTo(hook.headers().get("webhook-id"));
    assertThat((String) JsonPath.read(hook.body(), "$.data.member.email"))
        .isEqualTo(member.email());
    assertThat((String) JsonPath.read(hook.body(), "$.studio.name")).isEqualTo("Clay Club");

    // The example on the Webhooks page has the same shape as the real thing
    String types =
        mvc.get()
            .uri(path("/webhooks/event-types"))
            .with(owner.token())
            .exchange()
            .getResponse()
            .getContentAsString();
    List<Map<String, Object>> examples =
        JsonPath.read(types, "$[?(@.type == 'member.joined')].example.data.member");
    Map<String, Object> real = JsonPath.read(hook.body(), "$.data.member");
    assertThat(examples).hasSize(1);
    assertThat(examples.getFirst()).containsOnlyKeys(real.keySet());

    String endpointId = read(created, "$.endpoint.id");
    await()
        .atMost(WAIT)
        .untilAsserted(() ->
            assertThat(
                    mvc.get()
                        .uri(path("/webhooks/endpoints/" + endpointId + "/deliveries"))
                        .with(owner.token()))
                .bodyJson()
                .extractingPath("$[0].status")
                .isEqualTo("DELIVERED"));
  }

  @Test
  void aSupplySaleIsPostedAsAFeeCharged() throws Exception {
    endpoint("fee.charged");
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

    await().atMost(WAIT).until(() -> !received.isEmpty());
    String body = received.peek().body();
    assertThat((String) JsonPath.read(body, "$.type")).isEqualTo("fee.charged");
    assertThat((String) JsonPath.read(body, "$.data.description")).isEqualTo("Speckled buff × 2");
    assertThat((String) JsonPath.read(body, "$.data.amount")).matches("\\d+\\.\\d{2}");
    assertThat((String) JsonPath.read(body, "$.data.currency")).isEqualTo("USD");
    assertThat((String) JsonPath.read(body, "$.data.member.id")).isEqualTo(memberId);
  }

  @Test
  void workThatRolledBackSendsNothing() {
    endpoint("member.joined");
    TenantContext.callAs(
        TenantContext.forAccount(ownerId).inStudio(studioId, StudioRole.OWNER),
        () ->
            transactions.execute(tx -> {
              events.publish(WebhookEventType.MEMBER_JOINED, "rolled-back", Map.of("x", 1));
              tx.setRollbackOnly();
              return null;
            }));
    Integer stored =
        jdbc.sql("select count(*) from webhook_event where dedupe_key = 'rolled-back'")
            .query(Integer.class)
            .single();
    assertThat(stored).isZero();
    assertThat(received).isEmpty();
  }

  @Test
  void aFailureIsRetriedBySweepUntilItArrives() throws Exception {
    String endpointId = endpoint("member.joined");
    answerWith.set(500);
    join(member);
    await().atMost(WAIT).until(() -> received.size() == 1);
    await()
        .atMost(WAIT)
        .untilAsserted(() ->
            assertThat(deliveries(endpointId))
                .bodyJson()
                .extractingPath("$[0].attempts")
                .isEqualTo(1));
    assertThat(deliveries(endpointId))
        .bodyJson()
        .extractingPath("$[0].status")
        .isEqualTo("PENDING");

    answerWith.set(204);
    // What the sweep does each minute, two minutes on, when the retry is due
    sweep.runAt(Instant.now().plus(Duration.ofMinutes(2)));
    assertThat(received).hasSize(2);
    // The same event both times, so a receiver can drop the duplicate
    assertThat(received.stream().map(r -> r.headers().get("webhook-id")).distinct()).hasSize(1);
    assertThat(deliveries(endpointId))
        .bodyJson()
        .extractingPath("$[0].status")
        .isEqualTo("DELIVERED");
    assertThat(mvc.get().uri(path("/webhooks/endpoints")).with(owner.token()))
        .bodyJson()
        .extractingPath("$[0].consecutiveFailures")
        .isEqualTo(0);
  }

  @Test
  void goneTurnsTheEndpointOffAndTellsStaff() throws Exception {
    String endpointId = endpoint("member.joined");
    answerWith.set(410);
    assertThat(post(owner, path("/webhooks/endpoints/" + endpointId + "/test"), ""))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo("FAILED");
    assertThat(mvc.get().uri(path("/webhooks/endpoints")).with(owner.token()))
        .bodyJson()
        .extractingPath("$[0].enabled")
        .isEqualTo(false);
    assertThat(mvc.get().uri(path("/notifications")).with(owner.token()))
        .bodyJson()
        .extractingPath("$[0].kind")
        .isEqualTo("WEBHOOK_DISABLED");

    // Back on, a test goes through
    answerWith.set(200);
    assertThat(patch(owner, path("/webhooks/endpoints/" + endpointId), "{\"enabled\": true}"))
        .hasStatusOk();
    assertThat(post(owner, path("/webhooks/endpoints/" + endpointId + "/test"), ""))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo("DELIVERED");
  }

  @Test
  void aRotatedSecretSignsAlongsideTheOldOneAndResendKeepsTheEventId() throws Exception {
    MvcTestResult created =
        post(
            owner,
            path("/webhooks/endpoints"),
            """
            {"url": "%s", "eventTypes": ["member.joined"]}
            """
                .formatted(url()));
    String endpointId = read(created, "$.endpoint.id");
    String oldSecret = read(created, "$.secret");
    String newSecret =
        read(
            post(owner, path("/webhooks/endpoints/" + endpointId + "/rotate-secret"), ""),
            "$.secret");

    String deliveryId =
        read(post(owner, path("/webhooks/endpoints/" + endpointId + "/test"), ""), "$.id");
    assertThat(post(owner, path("/webhooks/deliveries/" + deliveryId + "/resend"), ""))
        .hasStatusOk();

    assertThat(received).hasSize(2);
    Received hook = received.peek();
    long at = Long.parseLong(hook.headers().get("webhook-timestamp"));
    String id = hook.headers().get("webhook-id");
    assertThat(hook.headers().get("webhook-signature").split(" "))
        .containsExactly(
            WebhookSigner.sign(newSecret, id, at, hook.body()),
            WebhookSigner.sign(oldSecret, id, at, hook.body()));
    assertThat(received.stream().map(r -> r.headers().get("webhook-id")).distinct()).hasSize(1);
  }

  @Test
  void onlyStaffManageWebhooksAndStudiosDontSeeEachOthers() throws Exception {
    String endpointId = endpoint("member.joined");
    join(member);
    assertThat(mvc.get().uri(path("/webhooks/endpoints")).with(member.token()))
        .hasStatus(HttpStatus.FORBIDDEN);

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
                .uri("/api/v1/studios/" + otherStudio + "/webhooks/endpoints")
                .with(other.token()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$")
        .asArray()
        .isEmpty();
    assertThat(
            mvc.get()
                .uri(
                    "/api/v1/studios/"
                        + otherStudio
                        + "/webhooks/endpoints/"
                        + endpointId
                        + "/deliveries")
                .with(other.token()))
        .hasStatus(HttpStatus.NOT_FOUND);
  }

  @Test
  void refusesBadEndpoints() {
    assertThat(
            post(
                owner,
                path("/webhooks/endpoints"),
                """
                {"url": "ftp://example.com/x", "eventTypes": ["member.joined"]}
                """))
        .hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(
            post(
                owner,
                path("/webhooks/endpoints"),
                """
                {"url": "%s", "eventTypes": ["ping"]}
                """
                    .formatted(url())))
        .hasStatus(HttpStatus.BAD_REQUEST);
  }

  private String endpoint(String type) {
    return read(
        post(
            owner,
            path("/webhooks/endpoints"),
            """
            {"url": "%s", "eventTypes": ["%s"]}
            """
                .formatted(url(), type)),
        "$.endpoint.id");
  }

  /** Invites and accepts; returns the new member's account id. */
  private String join(TestUser who) throws Exception {
    String code = read(post(owner, path("/invites"), "{\"role\": \"MEMBER\"}"), "$.code");
    assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(who.token()))
        .hasStatusOk();
    return read(mvc.get().uri("/api/v1/me").with(who.token()).exchange(), "$.account.id");
  }

  private MvcTestResult deliveries(String endpointId) {
    return mvc.get()
        .uri(path("/webhooks/endpoints/" + endpointId + "/deliveries"))
        .with(owner.token())
        .exchange();
  }

  private String url() {
    return "http://127.0.0.1:" + receiver.getAddress().getPort() + "/hook";
  }

  private MvcTestResult post(TestUser user, String uri, String body) {
    return mvc.post()
        .uri(uri)
        .with(user.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .exchange();
  }

  private MvcTestResult patch(TestUser user, String uri, String body) {
    return mvc.patch()
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
