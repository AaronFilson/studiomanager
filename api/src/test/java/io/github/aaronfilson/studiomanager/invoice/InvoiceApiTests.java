package io.github.aaronfilson.studiomanager.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpServer;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.StatementCounts;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.json.JsonMapper;

/**
 * The supplies inbox end to end: upload to a real Azurite, read by a local stand-in for Anthropic's
 * Messages API answering with a recorded eval answer, matched, checked, and confirmed into stock.
 */
@IntegrationTest
class InvoiceApiTests {

  private static final String KEY = "sk-ant-api03-" + "a".repeat(40) + "Wxyz";
  private static final String REFUSED_KEY = "sk-ant-api03-" + "b".repeat(40) + "Nope";
  private static final byte[] PDF = "%PDF-1.7\n% an invoice\n".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0};
  private static final AtomicInteger messagesCalls = new AtomicInteger();
  private static final AtomicReference<String> lastRequest = new AtomicReference<>("");
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
  @Autowired StatementCounts statements;
  @Autowired InvoiceInbox inbox;

  private final HttpClient http = HttpClient.newHttpClient();
  private final TestUser owner = TestUser.named("Olive");
  private final TestUser member = TestUser.named("Mo");
  private UUID studioId;
  private String bMix;

  @BeforeEach
  void aStudioWithSupplies() {
    studioId = UUID.fromString(read(studio(owner, "Clay Club"), "$.id"));
    bMix = item("B-Mix 5", "50 lb box");
    item("Speckled Buff", "25 lb bag");
  }

  @Test
  void anInvoiceIsReadMatchedAndConfirmedIntoStock() throws Exception {
    setKey(KEY);
    String invoice = upload("PDF", PDF);

    MvcTestResult read = awaitRead(invoice);
    assertThat(read).bodyJson().extractingPath("$.status").isEqualTo("READY");
    assertThat(read).bodyJson().extractingPath("$.reader").isEqualTo("CLAUDE");
    assertThat(read).bodyJson().extractingPath("$.total").isEqualTo("265.03");
    assertThat(read).bodyJson().extractingPath("$.lines[0].supplyItemId").isEqualTo(bMix);
    assertThat(read).bodyJson().extractingPath("$.lines[0].matchSource").isEqualTo("NAME");
    assertThat(read).bodyJson().extractingPath("$.problems").asArray().isEmpty();
    assertThat(read).bodyJson().extractingPath("$.fileUrl").asString().contains("invoices%2F");
    // The model got the file and the instructions, and nothing about the studio's people
    assertThat(lastRequest.get()).contains("\"type\":\"document\"").doesNotContain("Olive");

    assertThat(post(owner, path("/invoices/" + invoice + "/confirm"), "{\"expectedVersion\": 0}"))
        .hasStatus(HttpStatus.CONFLICT);
    long version = version(read);
    assertThat(
            post(
                owner,
                path("/invoices/" + invoice + "/confirm"),
                "{\"expectedVersion\": %d}".formatted(version)))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo("CONFIRMED");
    // There's no single-item read; the item comes from the supplies list
    List<String> onHand =
        JsonPath.read(
            get(owner, path("/supplies")).getResponse().getContentAsString(),
            "$[?(@.id == '%s')].onHand".formatted(bMix));
    assertThat(onHand).singleElement().asString().startsWith("5");

    // The vendor's next invoice is matched by what staff confirmed: its SKU
    String next = upload("PDF", PDF);
    assertThat(awaitRead(next))
        .bodyJson()
        .extractingPath("$.lines[0].matchSource")
        .isEqualTo("ALIAS_SKU");
  }

  @Test
  void staffCorrectTheDraftAndAMismatchNeedsALook() throws Exception {
    setKey(KEY);
    String invoice = upload("PDF", PDF);
    long version = version(awaitRead(invoice));

    MvcTestResult saved =
        send(
            owner,
            HttpMethod.PUT,
            path("/invoices/" + invoice),
            """
            {"vendorName": "Riverbend", "total": "999.00", "expectedVersion": %d,
             "lines": [{"description": "Pallet", "quantity": "1", "unitPrice": "10", "lineTotal": "10"}]}
            """
                .formatted(version));

    assertThat(saved).hasStatusOk();
    assertThat(saved).bodyJson().extractingPath("$.status").isEqualTo("NEEDS_LOOK");
    assertThat(saved)
        .bodyJson()
        .extractingPath("$.problems[0]")
        .isEqualTo("The subtotal, tax and shipping add up to 10.00, but the total is 999.00.");
    assertThat(saved).bodyJson().extractingPath("$.lines[0].supplyItemId").isNull();
  }

  @Test
  void withoutAKeyTheInvoiceIsTypedInByHand() throws Exception {
    int before = messagesCalls.get();
    String invoice = upload("PNG", PNG);

    MvcTestResult read = awaitRead(invoice);
    assertThat(read).bodyJson().extractingPath("$.status").isEqualTo("NEEDS_LOOK");
    assertThat(read).bodyJson().extractingPath("$.readNote").asString().contains("No reader");
    assertThat(messagesCalls.get()).isEqualTo(before);
  }

  @Test
  void compareReadersRunsOnlyTheReadersTheStudioMayUse() throws Exception {
    String invoice = upload("PNG", PNG);
    awaitRead(invoice);

    // No key, and the platform's readers are off by default: nobody may read
    MvcTestResult compared = post(owner, path("/invoices/" + invoice + "/compare-readers"), "");

    assertThat(compared).hasStatusOk();
    assertThat(compared).bodyJson().extractingPath("$.results.length()").isEqualTo(0);
  }

  @Test
  void aKeyAnthropicRefusesIsMarkedAndTheInvoiceIsTypedIn() throws Exception {
    setKey(REFUSED_KEY);
    String invoice = upload("PDF", PDF);

    assertThat(awaitRead(invoice))
        .bodyJson()
        .extractingPath("$.readNote")
        .asString()
        .contains("refused the key");
    assertThat(get(owner, path("/ai-key")))
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo("REJECTED");
  }

  @Test
  void aFileThatIsNotWhatItSaidIsRefused() throws Exception {
    MvcTestResult started = post(owner, path("/invoices"), "{\"fileType\": \"PDF\"}");
    String invoice = read(started, "$.invoiceId");
    assertThat(post(owner, path("/invoices/" + invoice + "/complete"), ""))
        .hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(put(read(started, "$.uploadUrl"), PNG)).isEqualTo(201);
    assertThat(post(owner, path("/invoices/" + invoice + "/complete"), ""))
        .hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(send(owner, HttpMethod.DELETE, path("/invoices/" + invoice), ""))
        .hasStatus(HttpStatus.NO_CONTENT);
  }

  @Test
  void membersAndTokensCannotReachTheInbox() {
    String code = read(post(owner, path("/invites"), "{\"role\": \"MEMBER\"}"), "$.code");
    assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(member.token()))
        .hasStatusOk();
    assertThat(get(member, path("/invoices"))).hasStatus(HttpStatus.FORBIDDEN);

    String token =
        read(
            post(owner, path("/api-tokens"), "{\"name\": \"t\", \"scopes\": [\"read\"]}"),
            "$.secret");
    assertThat(mvc.get().uri(path("/invoices")).header("Authorization", "Bearer " + token))
        .hasStatus(HttpStatus.FORBIDDEN);
  }

  @Test
  void theInboxIsTheSameNumberOfQueriesForOneInvoiceOrMany() throws Exception {
    post(owner, path("/invoices"), "{\"fileType\": \"PDF\"}");
    long forOne = countListing();

    post(owner, path("/invoices"), "{\"fileType\": \"JPEG\"}");
    post(owner, path("/invoices"), "{\"fileType\": \"PNG\"}");

    assertThat(countListing()).isEqualTo(forOne);
  }

  private long countListing() throws Exception {
    UUID ownerId =
        UUID.fromString(
            read(mvc.get().uri("/api/v1/me").with(owner.token()).exchange(), "$.account.id"));
    TenantContext bound = new TenantContext(ownerId, studioId, StudioRole.OWNER);
    return statements
        .of(() -> TenantContext.callAs(bound, () -> inbox.list(studioId)))
        .statements();
  }

  private String upload(String fileType, byte[] content) throws Exception {
    MvcTestResult started =
        post(owner, path("/invoices"), "{\"fileType\": \"%s\"}".formatted(fileType));
    assertThat(started).hasStatus(HttpStatus.CREATED);
    assertThat(put(read(started, "$.uploadUrl"), content)).isEqualTo(201);
    String invoice = read(started, "$.invoiceId");
    MvcTestResult completed = post(owner, path("/invoices/" + invoice + "/complete"), "");
    assertThat(completed).hasStatus(HttpStatus.ACCEPTED);
    return invoice;
  }

  private MvcTestResult awaitRead(String invoice) {
    AtomicReference<MvcTestResult> latest = new AtomicReference<>();
    await()
        .atMost(Duration.ofSeconds(30))
        .until(() -> {
          latest.set(get(owner, path("/invoices/" + invoice)));
          return !List.of("UPLOADING", "READING").contains(read(latest.get(), "$.status"));
        });
    return latest.get();
  }

  private void setKey(String key) {
    assertThat(
            send(
                owner,
                HttpMethod.PUT,
                path("/ai-key"),
                "{\"provider\": \"ANTHROPIC\", \"apiKey\": \"%s\"}".formatted(key)))
        .hasStatusOk();
  }

  private String item(String name, String unit) {
    return read(
        post(
            owner,
            path("/supplies"),
            "{\"name\": \"%s\", \"category\": \"CLAY\", \"unit\": \"%s\"}".formatted(name, unit)),
        "$.id");
  }

  private int put(String url, byte[] body) throws Exception {
    return http.send(
            HttpRequest.newBuilder(URI.create(url))
                .header("x-ms-blob-type", "BlockBlob")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(body))
                .build(),
            HttpResponse.BodyHandlers.discarding())
        .statusCode();
  }

  /** Answers every read with the recorded eval answer for the clay order; refuses one key. */
  private static HttpServer start() {
    try {
      String answer =
          Files.readString(
              InvoiceEvalTests.CASES.resolve("synthetic-clay-order").resolve("claude.json"));
      JsonMapper json = JsonMapper.builder().build();
      byte[] message =
          json.writeValueAsBytes(
              Map.of(
                  "id", "msg_test",
                  "type", "message",
                  "role", "assistant",
                  "model", "claude-opus-5-5",
                  "content", List.of(Map.of("type", "text", "text", answer)),
                  "stop_reason", "end_turn",
                  "usage", Map.of("input_tokens", 1200, "output_tokens", 300)));
      byte[] refused =
          """
          {"type": "error", "error": {"type": "authentication_error", "message": "invalid x-api-key"}}
          """
              .getBytes(StandardCharsets.UTF_8);
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/v1/messages",
          exchange -> {
            messagesCalls.incrementAndGet();
            lastRequest.set(
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String key = exchange.getRequestHeaders().getFirst("x-api-key");
            String bearer = exchange.getRequestHeaders().getFirst("Authorization");
            boolean good =
                (key != null && key.endsWith("Wxyz"))
                    || (bearer != null && bearer.endsWith("Wxyz"));
            byte[] body = good ? message : refused;
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

  private MvcTestResult studio(TestUser user, String name) {
    return post(
        user,
        "/api/v1/studios",
        """
        {"name": "%s", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
        """
            .formatted(name));
  }

  private MvcTestResult get(TestUser user, String uri) {
    return mvc.get().uri(uri).with(user.token()).exchange();
  }

  private MvcTestResult post(TestUser user, String uri, String body) {
    return send(user, HttpMethod.POST, uri, body);
  }

  private MvcTestResult send(TestUser user, HttpMethod method, String uri, String body) {
    return mvc.method(method)
        .uri(uri)
        .with(user.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .exchange();
  }

  private String path(String suffix) {
    return "/api/v1/studios/" + studioId + suffix;
  }

  private static long version(MvcTestResult result) {
    try {
      Number version = JsonPath.read(result.getResponse().getContentAsString(), "$.version");
      return version.longValue();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String read(MvcTestResult result, String jsonPath) {
    try {
      Object value = JsonPath.read(result.getResponse().getContentAsString(), jsonPath);
      return String.valueOf(value);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
