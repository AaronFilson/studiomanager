package io.github.aaronfilson.studiomanager.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The whole code flow as Entra drives it, against a stub GitHub on the JDK's HttpServer: Entra's
 * browser redirect to /oauth2/authorize, the bounce through GitHub, the callback, the code back to
 * Entra, and Entra's token request.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class SignInFlowTests {

  private static final String ENTRA_CLIENT_ID = "entra-external-id";
  private static final String ENTRA_CLIENT_SECRET = "entra-client-secret-for-tests";
  private static final String ENTRA_REDIRECT =
      "https://studiomanagerdev.ciamlogin.com/aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa/federation/oauth2";
  private static final String GITHUB_ACCESS_TOKEN = "gho_stub_access_token";

  private static final AtomicBoolean emailVerified = new AtomicBoolean(true);
  private static final AtomicReference<String> emailsAuthorization = new AtomicReference<>();
  private static final HttpServer GITHUB = startGitHub();
  private static final int BRIDGE_PORT = freePort();
  private static final String BRIDGE = "http://localhost:" + BRIDGE_PORT;

  @DynamicPropertySource
  static void bridge(DynamicPropertyRegistry registry) {
    String github = "http://127.0.0.1:" + GITHUB.getAddress().getPort();
    registry.add("server.port", () -> BRIDGE_PORT);
    registry.add("server.servlet.session.cookie.secure", () -> "false");
    registry.add("studiomanager.bridge.issuer", () -> BRIDGE);
    registry.add("studiomanager.bridge.entra-client-id", () -> ENTRA_CLIENT_ID);
    registry.add("studiomanager.bridge.entra-client-secret", () -> ENTRA_CLIENT_SECRET);
    registry.add("studiomanager.bridge.entra-redirect-uris", () -> ENTRA_REDIRECT);
    registry.add("studiomanager.bridge.signing-keys", () -> TestKeys.pem(2048));
    registry.add("studiomanager.bridge.github-emails-uri", () -> github + "/user/emails");
    registry.add("spring.security.oauth2.client.registration.github.client-id", () -> "gh-client");
    registry.add(
        "spring.security.oauth2.client.registration.github.client-secret", () -> "gh-secret");
    registry.add(
        "spring.security.oauth2.client.provider.github.authorization-uri",
        () -> github + "/login/oauth/authorize");
    registry.add(
        "spring.security.oauth2.client.provider.github.token-uri",
        () -> github + "/login/oauth/access_token");
    registry.add(
        "spring.security.oauth2.client.provider.github.user-info-uri", () -> github + "/user");
    registry.add("spring.security.oauth2.client.provider.github.user-name-attribute", () -> "id");
  }

  @AfterAll
  static void stopGitHub() {
    GITHUB.stop(0);
  }

  private HttpClient browser;

  @BeforeEach
  void freshBrowser() {
    emailVerified.set(true);
    emailsAuthorization.set(null);
    browser =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(5))
            .build();
  }

  @Test
  void discoveryPublishesTheEndpointsEntraNeeds() throws Exception {
    HttpResponse<String> discovery = get(URI.create(BRIDGE + "/.well-known/openid-configuration"));

    assertThat(discovery.statusCode()).isEqualTo(200);
    String body = discovery.body();
    assertThat(JsonPath.<String>read(body, "$.issuer")).isEqualTo(BRIDGE);
    assertThat(JsonPath.<String>read(body, "$.authorization_endpoint"))
        .isEqualTo(BRIDGE + "/oauth2/authorize");
    assertThat(JsonPath.<String>read(body, "$.token_endpoint")).isEqualTo(BRIDGE + "/oauth2/token");
    assertThat(JsonPath.<String>read(body, "$.jwks_uri")).isEqualTo(BRIDGE + "/oauth2/jwks");
    assertThat(JsonPath.<List<String>>read(body, "$.token_endpoint_auth_methods_supported"))
        .contains("client_secret_post");
    assertThat(JsonPath.<List<String>>read(body, "$.response_types_supported")).contains("code");
    assertThat(JsonPath.<List<String>>read(body, "$.subject_types_supported")).isNotEmpty();
  }

  @Test
  void aVerifiedGitHubUserGetsASignedIdTokenForEntra() throws Exception {
    String verifier = "a-pkce-verifier-that-is-long-enough-for-rfc-7636-0123456789";
    URI entraCallback = signInThroughGitHub("entra-state", "entra-nonce", challenge(verifier));

    Map<String, String> callback = query(entraCallback);
    assertThat(entraCallback.toString()).startsWith(ENTRA_REDIRECT + "?");
    assertThat(callback).containsEntry("state", "entra-state");

    HttpResponse<String> tokens = redeem(callback.get("code"), verifier);
    assertThat(tokens.statusCode()).isEqualTo(200);

    Jwt idToken =
        NimbusJwtDecoder.withJwkSetUri(BRIDGE + "/oauth2/jwks")
            .build()
            .decode(JsonPath.read(tokens.body(), "$.id_token"));
    assertThat(idToken.getIssuer()).hasToString(BRIDGE);
    assertThat(idToken.getAudience()).containsExactly(ENTRA_CLIENT_ID);
    assertThat(idToken.getSubject()).isEqualTo("github:4242");
    assertThat(idToken.getClaimAsString("email")).isEqualTo("pat@example.com");
    assertThat(idToken.getClaimAsBoolean("email_verified")).isTrue();
    assertThat(idToken.getClaimAsString("name")).isEqualTo("Pat Potter");
    assertThat(idToken.getClaimAsString("picture")).isEqualTo("https://avatars.example/4242");
    assertThat(idToken.getClaimAsString("nonce")).isEqualTo("entra-nonce");
    assertThat(Duration.between(idToken.getIssuedAt(), idToken.getExpiresAt()))
        .isEqualTo(Duration.ofMinutes(5));
    // The bridge read the emails with GitHub's token, and the token went no further
    assertThat(emailsAuthorization.get()).isEqualTo("Bearer " + GITHUB_ACCESS_TOKEN);
  }

  @Test
  void aCodeIsRedeemedOnlyOnce() throws Exception {
    String verifier = "another-pkce-verifier-that-is-long-enough-for-rfc-7636-012345";
    String code =
        query(signInThroughGitHub("state-two", "nonce-two", challenge(verifier))).get("code");

    assertThat(redeem(code, verifier).statusCode()).isEqualTo(200);

    HttpResponse<String> replay = redeem(code, verifier);
    assertThat(replay.statusCode()).isEqualTo(400);
    assertThat(JsonPath.<String>read(replay.body(), "$.error")).isEqualTo("invalid_grant");
  }

  @Test
  void aWrongPkceVerifierIsRefused() throws Exception {
    String code =
        query(
                signInThroughGitHub(
                    "state-three",
                    "nonce-three",
                    challenge("the-real-verifier-0123456789-0123456789-0123")))
            .get("code");

    HttpResponse<String> tokens = redeem(code, "a-different-verifier-0123456789-0123456789-01");
    assertThat(tokens.statusCode()).isEqualTo(400);
  }

  @Test
  void aCallbackWithTheWrongStateIssuesNoCode() throws Exception {
    URI gitHubAuthorize = startSignIn("state-four", "nonce-four", challenge("v".repeat(50)));
    assertThat(query(gitHubAuthorize)).containsKey("state");

    HttpResponse<String> callback =
        get(URI.create(BRIDGE + "/login/oauth2/code/github?code=gh-code&state=forged-state"));

    assertThat(callback.statusCode()).isEqualTo(403);
    assertThat(callback.headers().firstValue("Location")).isEmpty();
    // Nothing was asked of GitHub
    assertThat(emailsAuthorization.get()).isNull();
  }

  @Test
  void aGitHubAccountWithoutAVerifiedEmailIsRefused() throws Exception {
    emailVerified.set(false);
    URI gitHubAuthorize = startSignIn("state-five", "nonce-five", challenge("w".repeat(50)));
    String gitHubState = query(gitHubAuthorize).get("state");

    HttpResponse<String> callback =
        get(
            URI.create(
                BRIDGE + "/login/oauth2/code/github?code=gh-code&state=" + encode(gitHubState)));

    assertThat(callback.statusCode()).isEqualTo(403);
    assertThat(callback.body()).contains("verified primary email");
    assertThat(callback.headers().firstValue("Location")).isEmpty();
  }

  @Test
  void anUnregisteredRedirectUriIsRefused() throws Exception {
    HttpResponse<String> response =
        get(
            URI.create(
                BRIDGE
                    + "/oauth2/authorize?response_type=code&client_id="
                    + ENTRA_CLIENT_ID
                    + "&scope=openid&state=s&redirect_uri="
                    + encode("https://attacker.example/callback")));

    assertThat(response.headers().firstValue("Location").orElse(""))
        .doesNotStartWith("https://attacker.example");
  }

  /** Entra's redirect to /oauth2/authorize, and the bridge's to GitHub. */
  private URI startSignIn(String state, String nonce, String codeChallenge) throws Exception {
    URI authorize =
        URI.create(
            BRIDGE
                + "/oauth2/authorize?response_type=code&client_id="
                + ENTRA_CLIENT_ID
                + "&scope="
                + encode("openid profile email")
                + "&redirect_uri="
                + encode(ENTRA_REDIRECT)
                + "&state="
                + encode(state)
                + "&nonce="
                + encode(nonce)
                + "&code_challenge="
                + codeChallenge
                + "&code_challenge_method=S256");
    HttpResponse<String> toSignIn = get(authorize);
    assertThat(toSignIn.statusCode()).isEqualTo(302);
    URI signIn = location(authorize, toSignIn);
    assertThat(signIn.getPath()).isEqualTo(BridgeSecurity.GITHUB_SIGN_IN);

    HttpResponse<String> toGitHub = get(signIn);
    assertThat(toGitHub.statusCode()).isEqualTo(302);
    URI gitHubAuthorize = location(signIn, toGitHub);
    assertThat(gitHubAuthorize.getPath()).isEqualTo("/login/oauth/authorize");
    Map<String, String> parameters = query(gitHubAuthorize);
    assertThat(parameters).containsEntry("client_id", "gh-client");
    assertThat(parameters).containsEntry("code_challenge_method", "S256");
    assertThat(parameters.get("scope")).contains("read:user").contains("user:email");
    return gitHubAuthorize;
  }

  /** The whole browser leg, ending at the redirect back to Entra with a code. */
  private URI signInThroughGitHub(String state, String nonce, String codeChallenge)
      throws Exception {
    String gitHubState = query(startSignIn(state, nonce, codeChallenge)).get("state");

    URI callback =
        URI.create(BRIDGE + "/login/oauth2/code/github?code=gh-code&state=" + encode(gitHubState));
    HttpResponse<String> backToAuthorize = get(callback);
    assertThat(backToAuthorize.statusCode()).isEqualTo(302);
    URI resumed = location(callback, backToAuthorize);
    assertThat(resumed.getPath()).isEqualTo("/oauth2/authorize");

    HttpResponse<String> toEntra = get(resumed);
    assertThat(toEntra.statusCode()).isEqualTo(302);
    return location(resumed, toEntra);
  }

  private HttpResponse<String> redeem(String code, String verifier) throws Exception {
    String form =
        Map.of(
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", ENTRA_REDIRECT,
                "client_id", ENTRA_CLIENT_ID,
                "client_secret", ENTRA_CLIENT_SECRET,
                "code_verifier", verifier)
            .entrySet()
            .stream()
            .map(entry -> entry.getKey() + "=" + encode(entry.getValue()))
            .collect(Collectors.joining("&"));
    // A server-to-server call: a fresh client, without the browser's cookies
    return HttpClient.newHttpClient()
        .send(
            HttpRequest.newBuilder(URI.create(BRIDGE + "/oauth2/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build(),
            HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> get(URI uri) throws Exception {
    return browser.send(
        HttpRequest.newBuilder(uri).header("Accept", "text/html").GET().build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private static URI location(URI from, HttpResponse<String> response) {
    return from.resolve(response.headers().firstValue("Location").orElseThrow());
  }

  private static Map<String, String> query(URI uri) {
    Map<String, String> parameters = new HashMap<>();
    String raw = uri.getRawQuery();
    if (raw == null) {
      return parameters;
    }
    for (String pair : raw.split("&")) {
      int equals = pair.indexOf('=');
      if (equals > 0) {
        parameters.put(
            URLDecoder.decode(pair.substring(0, equals), StandardCharsets.UTF_8),
            URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8));
      }
    }
    return parameters;
  }

  private static String challenge(String verifier) throws NoSuchAlgorithmException {
    byte[] digest =
        MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private static int freePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static HttpServer startGitHub() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/login/oauth/access_token",
          exchange ->
              respond(
                  exchange,
                  "{\"access_token\":\""
                      + GITHUB_ACCESS_TOKEN
                      + "\",\"token_type\":\"bearer\",\"scope\":\"read:user,user:email\"}"));
      server.createContext(
          "/user/emails",
          exchange -> {
            emailsAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(
                exchange,
                "[{\"email\":\"old@example.com\",\"primary\":false,\"verified\":true},"
                    + "{\"email\":\"pat@example.com\",\"primary\":true,\"verified\":"
                    + emailVerified.get()
                    + "}]");
          });
      server.createContext(
          "/user",
          exchange ->
              respond(
                  exchange,
                  "{\"id\":4242,\"login\":\"potter\",\"name\":\"Pat Potter\","
                      + "\"avatar_url\":\"https://avatars.example/4242\"}"));
      server.start();
      return server;
    } catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static void respond(HttpExchange exchange, String json) throws IOException {
    byte[] body = json.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, body.length);
    exchange.getResponseBody().write(body);
    exchange.close();
  }
}
