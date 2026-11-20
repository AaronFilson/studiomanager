package io.github.aaronfilson.studiomanager.glaze;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpServer;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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

/**
 * End-to-end tests for live Glazecalc share-link import and PKCE OAuth connect/callback/sync
 * against a local JDK {@link HttpServer} emulating Glazecalc's endpoints.
 */
@IntegrationTest
class GlazecalcIntegrationTests {

  private static HttpServer stubServer;
  private static final AtomicReference<String> EXPECTED_CHALLENGE = new AtomicReference<>();
  private static final String ACCESS_TOKEN = "gc_tok_secret_987654321";

  @Autowired MockMvcTester mvc;
  @Autowired JdbcClient jdbc;
  @Autowired TransactionTemplate transactions;

  private final TestUser owner = TestUser.named("Olive");
  private final TestUser mo = TestUser.named("Mo");
  private final TestUser pat = TestUser.named("Pat");
  private UUID studioId;
  private UUID moAccountId;

  @BeforeAll
  static void startStub() throws Exception {
    stubServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    stubServer.createContext(
        "/api/shared/recipe",
        exchange -> {
          String secret = exchange.getRequestHeaders().getFirst("X-Share-Secret");
          if (!"share-secret-xyz".equals(secret)) {
            writeJson(exchange, 404, "{\"error\": \"not found\"}");
            return;
          }
          writeJson(
              exchange,
              200,
              """
              {
                "recipe": {
                  "_id": "shared-shino-1",
                  "title": "Carbon Trap Shino",
                  "firedAt": "Cone 10 reduction",
                  "foodSurface": true,
                  "materials": [
                    {"name": "Nepheline Syenite", "amount": "75",
                     "analysis": {"SiO2": 60.7, "Al2O3": 23.3, "Na2O": 9.8, "K2O": 4.6}, "loi": 0.7},
                    {"name": "OM4 Ball Clay", "amount": "20"},
                    {"name": "Soda Ash", "amount": "5"}
                  ]
                }
              }
              """);
        });
    stubServer.createContext(
        "/oauth/token",
        exchange -> {
          String raw = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          Map<String, String> form = parseForm(raw);
          if (!"auth-code-123".equals(form.get("code"))) {
            writeJson(exchange, 400, "{\"error\": \"invalid_grant\"}");
            return;
          }
          String verifier = form.get("code_verifier");
          String expected = EXPECTED_CHALLENGE.get();
          if (verifier == null || (expected != null && !expected.equals(sha256B64Url(verifier)))) {
            writeJson(exchange, 400, "{\"error\": \"pkce_mismatch\"}");
            return;
          }
          writeJson(
              exchange,
              200,
              """
              {
                "access_token": "%s",
                "refresh_token": "gc_ref_token_abc",
                "expires_in": 3600,
                "email": "mo@glazecalc.example"
              }
              """
                  .formatted(ACCESS_TOKEN));
        });
    stubServer.createContext(
        "/api/export",
        exchange -> {
          String auth = exchange.getRequestHeaders().getFirst("Authorization");
          if (!("Bearer " + ACCESS_TOKEN).equals(auth)) {
            writeJson(exchange, 401, "{\"error\": \"unauthorized\"}");
            return;
          }
          writeJson(
              exchange,
              200,
              """
              {
                "format": "glazecalc-export",
                "version": 1,
                "account": {"email": "mo@glazecalc.example"},
                "records": {
                  "recipes": [
                    {
                      "_id": "oauth-rec-1",
                      "title": "Oribe Green",
                      "rev": 2,
                      "firedAt": "Cone 6 oxidation",
                      "sgTarget": 1.45,
                      "materials": [
                        {"name": "Custer Feldspar", "amount": "35"},
                        {"name": "Whiting", "amount": "20"},
                        {"name": "Silica", "amount": "30"},
                        {"name": "EPK", "amount": "15"}
                      ],
                      "additives": [
                        {"name": "Copper Carbonate", "amount": "4", "unit": "percent"}
                      ]
                    }
                  ]
                }
              }
              """);
        });
    stubServer.start();
  }

  @AfterAll
  static void stopStub() {
    if (stubServer != null) {
      stubServer.stop(0);
    }
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add(
        "studiomanager.glazecalc.base-url",
        () -> "http://127.0.0.1:" + stubServer.getAddress().getPort());
    registry.add("studiomanager.glazecalc.client-id", () -> "studiomanager-test");
    registry.add("studiomanager.glazecalc.client-secret", () -> "test-secret");
    registry.add(
        "studiomanager.glazecalc.redirect-uri",
        () -> "http://localhost:5173/studios/callback");
    registry.add("studiomanager.glazecalc.allow-private-networks", () -> "true");
  }

  @BeforeEach
  void setUp() {
    EXPECTED_CHALLENGE.set(null);
    studioId =
        UUID.fromString(
            read(
                post(
                    owner,
                    "/api/v1/studios",
                    """
                    {"name": "Sync Studio", "unitSystem": "METRIC", "timeZone": "America/Chicago", "country": "US"}
                    """),
                "$.id"));
    join(mo);
    join(pat);
    moAccountId =
        UUID.fromString(
            read(mvc.get().uri("/api/v1/me").with(mo.token()).exchange(), "$.account.id"));
  }

  @Test
  void shareLinkImportAllowlistsHostAndImportsRecipe() {
    // Rejects arbitrary foreign / SSRF URLs
    assertThat(
            post(
                mo,
                path("/glazes/import-share"),
                """
                {"shareUrlOrSecret": "https://evil.example/shared#share-secret-xyz", "dryRun": true}
                """))
        .hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(
            post(
                mo,
                path("/glazes/import-share"),
                """
                {"shareUrlOrSecret": "http://169.254.169.254/latest/meta-data#x", "dryRun": true}
                """))
        .hasStatus(HttpStatus.BAD_REQUEST);

    String validShareUrl =
        "http://127.0.0.1:" + stubServer.getAddress().getPort() + "/shared#share-secret-xyz";
    MvcTestResult preview =
        post(
            mo,
            path("/glazes/import-share"),
            "{\"shareUrlOrSecret\": \"%s\", \"dryRun\": true}".formatted(validShareUrl));
    assertThat(preview).hasStatusOk();
    assertThat(preview).bodyJson().extractingPath("$.committed").isEqualTo(false);
    assertThat(preview).bodyJson().extractingPath("$.rows[0].name").isEqualTo("Carbon Trap Shino");

    MvcTestResult imported =
        post(
            mo,
            path("/glazes/import-share"),
            "{\"shareUrlOrSecret\": \"%s\", \"dryRun\": false}".formatted(validShareUrl));
    assertThat(imported).hasStatusOk();
    assertThat(imported).bodyJson().extractingPath("$.committed").isEqualTo(true);
    String id = read(imported, "$.rows[0].recipeId");
    assertThat(mvc.get().uri(path("/glazes/" + id)).with(mo.token()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.lines[0].chemistry.analysis.SiO2")
        .isEqualTo(60.7);
  }

  @Test
  void oauthConnectCallbackEncryptsTokensAndSyncsRecipesWithOwnerRls() {
    assertThat(mvc.get().uri(path("/glazes/glazecalc/status")).with(mo.token()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.configured")
        .isEqualTo(true);

    MvcTestResult connect = post(mo, path("/glazes/glazecalc/connect"), "{}");
    assertThat(connect).hasStatusOk();
    String authUrl = read(connect, "$.authorizationUrl");
    String state = read(connect, "$.state");
    Map<String, String> query = parseForm(URI.create(authUrl).getRawQuery());
    assertThat(query.get("code_challenge_method")).isEqualTo("S256");
    assertThat(query.get("code_challenge")).isNotBlank();
    EXPECTED_CHALLENGE.set(query.get("code_challenge"));

    // Another member cannot hijack Mo's OAuth state
    assertThat(
            post(
                pat,
                path("/glazes/glazecalc/callback"),
                "{\"code\": \"auth-code-123\", \"state\": \"%s\"}".formatted(state)))
        .hasStatus(HttpStatus.BAD_REQUEST);

    // Mo completes the callback; stub verifies PKCE S256 challenge
    MvcTestResult connected =
        post(
            mo,
            path("/glazes/glazecalc/callback"),
            "{\"code\": \"auth-code-123\", \"state\": \"%s\"}".formatted(state));
    assertThat(connected).hasStatusOk();
    assertThat(connected).bodyJson().extractingPath("$.connected").isEqualTo(true);
    assertThat(connected)
        .bodyJson()
        .extractingPath("$.remoteAccountLabel")
        .isEqualTo("mo@glazecalc.example");

    // Tokens stored in Postgres are encrypted ciphertext, never plaintext
    byte[] rawCiphertext =
        TenantContext.callAs(
            new TenantContext(moAccountId, studioId, StudioRole.MEMBER),
            () ->
                transactions.execute(
                    tx ->
                        jdbc.sql(
                                "select access_token_enc from glazecalc_connection where account_id = :id")
                            .param("id", moAccountId)
                            .query(byte[].class)
                            .single()));
    assertThat(new String(rawCiphertext, StandardCharsets.ISO_8859_1))
        .doesNotContain(ACCESS_TOKEN);

    // Another member in the same studio sees connected = false (owner RLS)
    assertThat(mvc.get().uri(path("/glazes/glazecalc/status")).with(pat.token()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.connected")
        .isEqualTo(false);

    // Sync pulls /api/export and runs through the exact same importer + deduper
    MvcTestResult synced = post(mo, path("/glazes/glazecalc/sync"), "");
    assertThat(synced).hasStatusOk();
    assertThat(synced).bodyJson().extractingPath("$.committed").isEqualTo(true);
    assertThat(synced).bodyJson().extractingPath("$.createdCount").isEqualTo(1);
    assertThat(synced).bodyJson().extractingPath("$.rows[0].name").isEqualTo("Oribe Green");

    // Second sync is idempotent
    MvcTestResult resynced = post(mo, path("/glazes/glazecalc/sync"), "");
    assertThat(resynced).hasStatusOk();
    assertThat(resynced).bodyJson().extractingPath("$.unchangedCount").isEqualTo(1);

    // Status now shows lastSyncedAt
    assertThat(mvc.get().uri(path("/glazes/glazecalc/status")).with(mo.token()))
        .bodyJson()
        .extractingPath("$.lastSyncedAt")
        .isNotNull();

    // Disconnect removes the connection
    assertThat(mvc.delete().uri(path("/glazes/glazecalc/connection")).with(mo.token()))
        .hasStatus(HttpStatus.NO_CONTENT);
    assertThat(mvc.get().uri(path("/glazes/glazecalc/status")).with(mo.token()))
        .bodyJson()
        .extractingPath("$.connected")
        .isEqualTo(false);
  }

  private void join(TestUser who) {
    String code = read(post(owner, path("/invites"), "{\"role\": \"MEMBER\"}"), "$.code");
    assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(who.token()))
        .hasStatusOk();
  }

  private MvcTestResult post(TestUser user, String uri, String body) {
    return body.isEmpty()
        ? mvc.post().uri(uri).with(user.token()).exchange()
        : mvc.post()
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

  private static void writeJson(
      com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }

  private static Map<String, String> parseForm(String raw) {
    Map<String, String> out = new HashMap<>();
    if (raw == null || raw.isBlank()) {
      return out;
    }
    for (String pair : raw.split("&", -1)) {
      int eq = pair.indexOf('=');
      if (eq > 0) {
        out.put(
            URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
            URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
      }
    }
    return out;
  }

  private static String sha256B64Url(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
