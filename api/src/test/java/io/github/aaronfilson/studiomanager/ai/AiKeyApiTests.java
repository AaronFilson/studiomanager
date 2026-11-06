package io.github.aaronfilson.studiomanager.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpServer;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionTemplate;

/** The studio's own AI key, checked against a local stand-in for Anthropic's models endpoint. */
@IntegrationTest
class AiKeyApiTests {

  private static final String KEY = "sk-ant-api03-" + "a".repeat(40) + "Wxyz";
  private static final AtomicReference<String> seenKey = new AtomicReference<>();
  private static final HttpServer ANTHROPIC = start();

  @DynamicPropertySource
  static void anthropic(DynamicPropertyRegistry registry) {
    registry.add(
        "studiomanager.ai.anthropic-base-url",
        () -> "http://127.0.0.1:" + ANTHROPIC.getAddress().getPort());
  }

  @AfterAll
  static void stop() {
    ANTHROPIC.stop(0);
  }

  @Autowired MockMvcTester mvc;

  @Autowired JdbcClient jdbc;

  @Autowired TransactionTemplate transactions;

  private final TestUser owner = TestUser.named("Olive");
  private final TestUser sam = TestUser.named("Sam");
  private UUID studioId;

  @BeforeEach
  void setUp() {
    studioId =
        UUID.fromString(
            read(
                send(
                    owner,
                    "POST",
                    "/api/v1/studios",
                    """
                    {"name": "Clay Club", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
                    """),
                "$.id"));
  }

  @Test
  void theOwnerSetsAndChecksAKeyThatIsNeverShownAgain() {
    assertThat(mvc.get().uri(path("/ai-key")).with(owner.token())).hasStatus(HttpStatus.NOT_FOUND);

    MvcTestResult saved =
        send(
            owner,
            "PUT",
            path("/ai-key"),
            "{\"provider\": \"ANTHROPIC\", \"apiKey\": \"%s\", \"allowGoogleFallback\": true}"
                .formatted(KEY));
    assertThat(saved).hasStatusOk();
    assertThat(saved).bodyText().doesNotContain(KEY);
    assertThat(saved).bodyJson().extractingPath("$.lastFour").isEqualTo("Wxyz");
    assertThat(saved).bodyJson().extractingPath("$.status").isEqualTo("UNTESTED");
    // Encrypted at rest
    UUID ownerId =
        UUID.fromString(
            read(mvc.get().uri("/api/v1/me").with(owner.token()).exchange(), "$.account.id"));
    byte[] stored =
        TenantContext.callAs(
            new TenantContext(ownerId, studioId, StudioRole.OWNER),
            () ->
                transactions.execute(tx ->
                    jdbc.sql("select key_ciphertext from ai_provider_key")
                        .query(byte[].class)
                        .single()));
    assertThat(new String(stored, StandardCharsets.ISO_8859_1)).doesNotContain("sk-ant-");

    MvcTestResult tested = send(owner, "POST", path("/ai-key/test"), "");
    assertThat(tested).bodyJson().extractingPath("$.status").isEqualTo("WORKING");
    assertThat(seenKey.get()).isEqualTo(KEY);
    assertThat(mvc.get().uri(path("/ai-key")).with(owner.token())).bodyText().doesNotContain(KEY);
  }

  @Test
  void aKeyAnthropicRefusesIsMarkedRejected() {
    String refused = "sk-ant-api03-" + "b".repeat(40) + "Nope";
    send(
        owner,
        "PUT",
        path("/ai-key"),
        "{\"provider\": \"ANTHROPIC\", \"apiKey\": \"%s\"}".formatted(refused));

    MvcTestResult tested = send(owner, "POST", path("/ai-key/test"), "");

    assertThat(tested).bodyJson().extractingPath("$.status").isEqualTo("REJECTED");
    assertThat(tested).bodyJson().extractingPath("$.reason").asString().contains("401");
  }

  @Test
  void onlyTheOwnerAndNeverATokenTouchesTheKey() {
    String code = read(send(owner, "POST", path("/invites"), "{\"role\": \"STAFF\"}"), "$.code");
    assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(sam.token()))
        .hasStatusOk();
    assertThat(
            send(
                sam,
                "PUT",
                path("/ai-key"),
                "{\"provider\": \"ANTHROPIC\", \"apiKey\": \"%s\"}".formatted(KEY)))
        .hasStatus(HttpStatus.FORBIDDEN);
    assertThat(
            send(
                owner,
                "PUT",
                path("/ai-key"),
                "{\"provider\": \"ANTHROPIC\", \"apiKey\": \"not-a-key-at-all-xx\"}"))
        .hasStatus(HttpStatus.BAD_REQUEST);

    String token =
        read(
            send(owner, "POST", path("/api-tokens"), "{\"name\": \"t\", \"scopes\": [\"read\"]}"),
            "$.secret");
    assertThat(mvc.get().uri(path("/ai-key")).header("Authorization", "Bearer " + token))
        .hasStatus(HttpStatus.FORBIDDEN);
  }

  private static HttpServer start() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/v1/models",
          exchange -> {
            String key = exchange.getRequestHeaders().getFirst("x-api-key");
            String bearer = exchange.getRequestHeaders().getFirst("Authorization");
            if (key == null && bearer != null && bearer.startsWith("Bearer ")) {
              key = bearer.substring("Bearer ".length());
            }
            if (key == null) {
              key = "headers: " + exchange.getRequestHeaders().keySet();
            }
            seenKey.set(key);
            boolean good = key != null && key.endsWith("Wxyz");
            byte[] body =
                (good
                        ? """
                        {"data": [{"type": "model", "id": "claude-opus-5-5", "display_name": "Claude Opus 5.5",
                                   "created_at": "2026-09-01T00:00:00Z"}],
                         "has_more": false, "first_id": "claude-opus-5-5", "last_id": "claude-opus-5-5"}
                        """
                        : """
                        {"type": "error", "error": {"type": "authentication_error", "message": "invalid x-api-key"}}
                        """)
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(good ? 200 : 401, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
          });
      server.start();
      return server;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private MvcTestResult send(TestUser user, String method, String uri, String body) {
    return mvc.method(org.springframework.http.HttpMethod.valueOf(method))
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
