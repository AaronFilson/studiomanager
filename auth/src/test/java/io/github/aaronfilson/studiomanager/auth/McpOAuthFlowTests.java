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
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * End-to-end MCP OAuth 2.1 flow on the bridge: RFC 8414 / OIDC discovery, RFC 7591 dynamic client
 * registration, PKCE + RFC 8707 resource-bound authorization with explicit consent, token minting
 * with {@code aud}/{@code scope}/{@code studio_id}, and zero regression on Entra federation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class McpOAuthFlowTests {

  private static final String ENTRA_CLIENT_ID = "entra-external-id";
  private static final String ENTRA_CLIENT_SECRET = "entra-client-secret-for-tests";
  private static final String ENTRA_REDIRECT =
      "https://studiomanagerdev.ciamlogin.com/aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa/federation/oauth2";
  private static final String ALLOWED_API_RESOURCE = "https://api.studiomanager.test/mcp";
  private static final UUID STUDIO_A = UUID.fromString("11111111-1111-4111-8111-111111111111");
  private static final UUID STUDIO_B = UUID.fromString("22222222-2222-4222-8222-222222222222");
  private static final String MCP_REDIRECT = "http://127.0.0.1:4321/callback";
  private static final String PRE_REGISTERED_ID = "mcp-preregistered";
  private static final String PRE_REGISTERED_REDIRECT = "https://claude.ai/api/mcp/auth_callback";

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
    registry.add("studiomanager.bridge.signing-keys", () -> TestKeys.pem(TestKeys.ACCEPTED_BITS));
    registry.add("studiomanager.bridge.github-emails-uri", () -> github + "/user/emails");
    registry.add("studiomanager.bridge.mcp.enabled", () -> "true");
    registry.add("studiomanager.bridge.mcp.allowed-resources", () -> ALLOWED_API_RESOURCE);
    registry.add("studiomanager.bridge.mcp.scopes", () -> "read,propose");
    registry.add("studiomanager.bridge.mcp.pre-registered-client-id", () -> PRE_REGISTERED_ID);
    registry.add(
        "studiomanager.bridge.mcp.pre-registered-redirect-uris", () -> PRE_REGISTERED_REDIRECT);
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
    browser =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(5))
            .build();
  }

  @Test
  void discoveryAdvertisesPkceScopesAndDynamicClientRegistration() throws Exception {
    HttpResponse<String> oauthMeta =
        get(URI.create(BRIDGE + "/.well-known/oauth-authorization-server"));
    assertThat(oauthMeta.statusCode()).isEqualTo(200);
    String body = oauthMeta.body();
    assertThat(JsonPath.<String>read(body, "$.issuer")).isEqualTo(BRIDGE);
    assertThat(JsonPath.<String>read(body, "$.registration_endpoint"))
        .isEqualTo(BRIDGE + "/oauth2/register");
    assertThat(JsonPath.<List<String>>read(body, "$.code_challenge_methods_supported"))
        .containsExactly("S256");
    assertThat(JsonPath.<List<String>>read(body, "$.scopes_supported"))
        .containsExactlyInAnyOrder("read", "propose");
    assertThat(JsonPath.<List<String>>read(body, "$.token_endpoint_auth_methods_supported"))
        .contains("none");

    HttpResponse<String> oidcMeta = get(URI.create(BRIDGE + "/.well-known/openid-configuration"));
    assertThat(oidcMeta.statusCode()).isEqualTo(200);
    assertThat(JsonPath.<String>read(oidcMeta.body(), "$.registration_endpoint"))
        .isEqualTo(BRIDGE + "/oauth2/register");
    assertThat(JsonPath.<List<String>>read(oidcMeta.body(), "$.code_challenge_methods_supported"))
        .containsExactly("S256");
    assertThat(JsonPath.<List<String>>read(oidcMeta.body(), "$.scopes_supported"))
        .contains("openid", "read", "propose");
  }

  @Test
  void dynamicClientRegistrationCreatesPublicPkceClientAndRejectsUnsafeMetadata() throws Exception {
    HttpResponse<String> created =
        registerClient(
            """
            {
              "client_name": "Claude Desktop",
              "redirect_uris": ["%s"],
              "token_endpoint_auth_method": "none",
              "grant_types": ["authorization_code"],
              "response_types": ["code"],
              "scope": "read propose"
            }
            """
                .formatted(MCP_REDIRECT));
    assertThat(created.statusCode()).isEqualTo(201);
    assertThat(JsonPath.<String>read(created.body(), "$.client_id")).startsWith("mcp_");
    assertThat(JsonPath.<String>read(created.body(), "$.client_name")).isEqualTo("Claude Desktop");
    assertThat(JsonPath.<String>read(created.body(), "$.token_endpoint_auth_method"))
        .isEqualTo("none");
    assertThat(JsonPath.<String>read(created.body(), "$.scope")).isEqualTo("read propose");

    HttpResponse<String> badRedirect =
        registerClient("{\"redirect_uris\": [\"http://evil.example/cb\"]}");
    assertThat(badRedirect.statusCode()).isEqualTo(400);
    assertThat(JsonPath.<String>read(badRedirect.body(), "$.error"))
        .isEqualTo("invalid_redirect_uri");

    HttpResponse<String> badScope =
        registerClient(
            "{\"redirect_uris\": [\"" + MCP_REDIRECT + "\"], \"scope\": \"read openid\"}");
    assertThat(badScope.statusCode()).isEqualTo(400);
    assertThat(JsonPath.<String>read(badScope.body(), "$.error"))
        .isEqualTo("invalid_client_metadata");

    HttpResponse<String> badAuth =
        registerClient(
            "{\"redirect_uris\": [\""
                + MCP_REDIRECT
                + "\"], \"token_endpoint_auth_method\": \"client_secret_basic\"}");
    assertThat(badAuth.statusCode()).isEqualTo(400);
    assertThat(JsonPath.<String>read(badAuth.body(), "$.error"))
        .isEqualTo("invalid_client_metadata");
  }

  @Test
  void fullPkceFlowWithConsentMintsResourceBoundStudioScopedJwt() throws Exception {
    String clientId = registerMcpClient("Claude Desktop", MCP_REDIRECT);
    String resource = "https://api.studiomanager.test/api/v1/studios/" + STUDIO_A + "/mcp";
    String verifier = "mcp-pkce-verifier-that-is-long-enough-for-rfc-7636-0123456789";

    URI consentUri =
        authorizeThroughGitHub(
            clientId, MCP_REDIRECT, "read propose", "mcp-state-1", challenge(verifier), resource);
    assertThat(consentUri.getPath()).isEqualTo("/oauth2/consent");

    HttpResponse<String> consentPage = get(consentUri);
    assertThat(consentPage.statusCode()).isEqualTo(200);
    assertThat(consentPage.headers().firstValue("Content-Security-Policy").orElse(""))
        .contains("default-src 'none'")
        .contains("frame-ancestors 'none'")
        .doesNotContain("unsafe-inline");
    assertThat(consentPage.body())
        .contains("Claude Desktop")
        .contains(STUDIO_A.toString())
        .contains(resource)
        .contains("read")
        .contains("propose");

    URI callback =
        submitConsent(
            consentPage.body(),
            clientId,
            query(consentUri).get("state"),
            List.of("read", "propose"));
    assertThat(callback.toString()).startsWith(MCP_REDIRECT + "?");
    Map<String, String> params = query(callback);
    assertThat(params).containsEntry("state", "mcp-state-1").containsKey("code");

    HttpResponse<String> tokens =
        exchangeMcpCode(clientId, MCP_REDIRECT, params.get("code"), verifier, resource);
    assertThat(tokens.statusCode()).isEqualTo(200);
    String accessToken = JsonPath.read(tokens.body(), "$.access_token");

    Jwt jwt = NimbusJwtDecoder.withJwkSetUri(BRIDGE + "/oauth2/jwks").build().decode(accessToken);
    assertThat(jwt.getIssuer()).hasToString(BRIDGE);
    assertThat(jwt.getSubject()).isEqualTo("github:4242");
    assertThat(jwt.getAudience()).containsExactly(resource);
    assertThat(jwt.getClaimAsString("studio_id")).isEqualTo(STUDIO_A.toString());
    assertThat(jwt.getClaimAsString("scope")).isEqualTo("propose read");
    assertThat(jwt.getClaimAsString("client_id")).isEqualTo(clientId);
    assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()))
        .isEqualTo(Duration.ofHours(1));

    // Re-authorizing the same client for Studio B still prompts explicitly (no cached cross-studio
    // consent!)
    String resourceB = "https://api.studiomanager.test/api/v1/studios/" + STUDIO_B + "/mcp";
    HttpResponse<String> secondAuthorize =
        get(
            authorizeUri(
                clientId, MCP_REDIRECT, "read", "mcp-state-2", challenge(verifier), resourceB));
    assertThat(secondAuthorize.statusCode()).isEqualTo(302);
    URI secondConsent = location(URI.create(BRIDGE), secondAuthorize);
    assertThat(secondConsent.getPath()).isEqualTo("/oauth2/consent");
    assertThat(get(secondConsent).body()).contains(STUDIO_B.toString());
  }

  @Test
  void denyingConsentReturnsAccessDeniedAndIssuesNoCode() throws Exception {
    String clientId = registerMcpClient("Claude Code", MCP_REDIRECT);
    String resource = "https://api.studiomanager.test/api/v1/studios/" + STUDIO_A + "/mcp";
    String verifier = "deny-flow-verifier-that-is-long-enough-for-rfc-7636-0123456789";

    URI consentUri =
        authorizeThroughGitHub(
            clientId, MCP_REDIRECT, "read", "deny-state", challenge(verifier), resource);
    HttpResponse<String> consentPage = get(consentUri);
    assertThat(consentPage.statusCode()).isEqualTo(200);

    URI callback =
        submitConsent(consentPage.body(), clientId, query(consentUri).get("state"), List.of());
    assertThat(callback.toString()).startsWith(MCP_REDIRECT + "?");
    Map<String, String> params = query(callback);
    assertThat(params)
        .containsEntry("error", "access_denied")
        .containsEntry("state", "deny-state")
        .doesNotContainKey("code");
  }

  @Test
  void rejectsMissingPkceDisallowedResourceAndMismatchedTokenResource() throws Exception {
    String clientId = registerMcpClient("Strict Client", MCP_REDIRECT);
    String resourceA = "https://api.studiomanager.test/api/v1/studios/" + STUDIO_A + "/mcp";
    String resourceB = "https://api.studiomanager.test/api/v1/studios/" + STUDIO_B + "/mcp";

    // Sign in via a valid flow, then verify mismatched token resource, missing PKCE, and
    // disallowed resource while authenticated
    String verifier = "mismatch-verifier-that-is-long-enough-for-rfc-7636-0123456789";
    URI consentUri =
        authorizeThroughGitHub(
            clientId, MCP_REDIRECT, "read", "ok-state", challenge(verifier), resourceA);
    HttpResponse<String> consentPage = get(consentUri);
    URI callback =
        submitConsent(
            consentPage.body(), clientId, query(consentUri).get("state"), List.of("read"));
    String code = query(callback).get("code");

    // Token exchange with a different resource -> 400 invalid_target (RFC 8707)
    HttpResponse<String> mismatchedToken =
        exchangeMcpCode(clientId, MCP_REDIRECT, code, verifier, resourceB);
    assertThat(mismatchedToken.statusCode()).isEqualTo(400);
    assertThat(JsonPath.<String>read(mismatchedToken.body(), "$.error"))
        .isEqualTo("invalid_target");

    // Now authenticated: missing PKCE challenge is rejected
    URI noPkce =
        URI.create(
            BRIDGE
                + "/oauth2/authorize?response_type=code&client_id="
                + encode(clientId)
                + "&scope=read&redirect_uri="
                + encode(MCP_REDIRECT)
                + "&state=nopkce&resource="
                + encode(resourceA));
    HttpResponse<String> noPkceResponse = get(noPkce);
    assertThat(noPkceResponse.statusCode()).isEqualTo(302);
    assertThat(query(location(noPkce, noPkceResponse)))
        .containsEntry("error", "invalid_request")
        .doesNotContainKey("code");

    // Disallowed resource while authenticated -> invalid_target
    HttpResponse<String> disallowed =
        get(
            authorizeUri(
                clientId,
                MCP_REDIRECT,
                "read",
                "badres",
                challenge("x".repeat(50)),
                "https://evil.example/api/v1/studios/" + STUDIO_A + "/mcp"));
    assertThat(disallowed.statusCode()).isEqualTo(302);
    assertThat(query(location(URI.create(BRIDGE), disallowed)))
        .containsEntry("error", "invalid_target")
        .doesNotContainKey("code");
  }

  @Test
  void preRegisteredMcpClientAndEntraFederationBothWorkAlongsideDynamicClients() throws Exception {
    // 1. Pre-registered MCP client works with base resource + explicit studio_id
    String mcpVerifier = "preregistered-verifier-long-enough-for-rfc-7636-0123456789";
    URI authorize =
        URI.create(
            BRIDGE
                + "/oauth2/authorize?response_type=code&client_id="
                + encode(PRE_REGISTERED_ID)
                + "&scope=read&redirect_uri="
                + encode(PRE_REGISTERED_REDIRECT)
                + "&state=pre-1&code_challenge="
                + challenge(mcpVerifier)
                + "&code_challenge_method=S256&resource="
                + encode(ALLOWED_API_RESOURCE)
                + "&studio_id="
                + STUDIO_A);
    URI consentUri = bounceThroughGitHubToAuthorize(authorize);
    assertThat(consentUri.getPath()).isEqualTo("/oauth2/consent");
    HttpResponse<String> consentPage = get(consentUri);
    URI callback =
        submitConsent(
            consentPage.body(), PRE_REGISTERED_ID, query(consentUri).get("state"), List.of("read"));
    HttpResponse<String> mcpTokens =
        exchangeMcpCode(
            PRE_REGISTERED_ID,
            PRE_REGISTERED_REDIRECT,
            query(callback).get("code"),
            mcpVerifier,
            ALLOWED_API_RESOURCE);
    assertThat(mcpTokens.statusCode()).isEqualTo(200);
    Jwt mcpJwt =
        NimbusJwtDecoder.withJwkSetUri(BRIDGE + "/oauth2/jwks")
            .build()
            .decode(JsonPath.read(mcpTokens.body(), "$.access_token"));
    assertThat(mcpJwt.getAudience()).containsExactly(ALLOWED_API_RESOURCE);
    assertThat(mcpJwt.getClaimAsString("studio_id")).isEqualTo(STUDIO_A.toString());

    // 2. Existing Entra federation still completes with NO consent screen and a 5-min ID token
    freshBrowser();
    String entraVerifier = "entra-verifier-that-is-long-enough-for-rfc-7636-0123456789";
    URI entraAuthorize =
        URI.create(
            BRIDGE
                + "/oauth2/authorize?response_type=code&client_id="
                + ENTRA_CLIENT_ID
                + "&scope="
                + encode("openid profile email")
                + "&redirect_uri="
                + encode(ENTRA_REDIRECT)
                + "&state=entra-s&nonce=entra-n&code_challenge="
                + challenge(entraVerifier)
                + "&code_challenge_method=S256");
    URI entraCallback = bounceThroughGitHubToAuthorize(entraAuthorize);
    assertThat(entraCallback.toString()).startsWith(ENTRA_REDIRECT + "?");
    String entraCode = query(entraCallback).get("code");

    String form =
        formBody(
            List.of(
                Map.entry("grant_type", "authorization_code"),
                Map.entry("code", entraCode),
                Map.entry("redirect_uri", ENTRA_REDIRECT),
                Map.entry("client_id", ENTRA_CLIENT_ID),
                Map.entry("client_secret", ENTRA_CLIENT_SECRET),
                Map.entry("code_verifier", entraVerifier)));
    HttpResponse<String> entraTokens =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI.create(BRIDGE + "/oauth2/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build(),
                HttpResponse.BodyHandlers.ofString());
    assertThat(entraTokens.statusCode()).isEqualTo(200);
    Jwt idToken =
        NimbusJwtDecoder.withJwkSetUri(BRIDGE + "/oauth2/jwks")
            .build()
            .decode(JsonPath.read(entraTokens.body(), "$.id_token"));
    assertThat(idToken.getAudience()).containsExactly(ENTRA_CLIENT_ID);
    assertThat(idToken.getSubject()).isEqualTo("github:4242");
    assertThat(Duration.between(idToken.getIssuedAt(), idToken.getExpiresAt()))
        .isEqualTo(Duration.ofMinutes(5));
  }

  private String registerMcpClient(String name, String redirectUri) throws Exception {
    HttpResponse<String> response =
        registerClient(
            """
            {"client_name": "%s", "redirect_uris": ["%s"], "token_endpoint_auth_method": "none"}
            """
                .formatted(name, redirectUri));
    assertThat(response.statusCode()).isEqualTo(201);
    return JsonPath.read(response.body(), "$.client_id");
  }

  private HttpResponse<String> registerClient(String json) throws Exception {
    return HttpClient.newHttpClient()
        .send(
            HttpRequest.newBuilder(URI.create(BRIDGE + "/oauth2/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build(),
            HttpResponse.BodyHandlers.ofString());
  }

  private URI authorizeThroughGitHub(
      String clientId,
      String redirectUri,
      String scope,
      String state,
      String codeChallenge,
      String resource)
      throws Exception {
    return bounceThroughGitHubToAuthorize(
        authorizeUri(clientId, redirectUri, scope, state, codeChallenge, resource));
  }

  private URI bounceThroughGitHubToAuthorize(URI authorize) throws Exception {
    HttpResponse<String> toSignIn = get(authorize);
    assertThat(toSignIn.statusCode()).isEqualTo(302);
    URI signIn = location(authorize, toSignIn);
    assertThat(signIn.getPath()).isEqualTo(BridgeSecurity.GITHUB_SIGN_IN);

    HttpResponse<String> toGitHub = get(signIn);
    assertThat(toGitHub.statusCode()).isEqualTo(302);
    String gitHubState = query(location(signIn, toGitHub)).get("state");

    URI callback =
        URI.create(BRIDGE + "/login/oauth2/code/github?code=gh-code&state=" + encode(gitHubState));
    HttpResponse<String> backToAuthorize = get(callback);
    assertThat(backToAuthorize.statusCode()).isEqualTo(302);
    URI resumed = location(callback, backToAuthorize);
    assertThat(resumed.getPath()).isEqualTo("/oauth2/authorize");

    HttpResponse<String> afterResume = get(resumed);
    assertThat(afterResume.statusCode()).isEqualTo(302);
    return location(resumed, afterResume);
  }

  private URI submitConsent(String consentHtml, String clientId, String state, List<String> scopes)
      throws Exception {
    List<Map.Entry<String, String>> fields = new ArrayList<>();
    fields.add(Map.entry("client_id", clientId));
    fields.add(Map.entry("state", state));
    Matcher csrf = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(consentHtml);
    if (csrf.find()) {
      fields.add(Map.entry("_csrf", csrf.group(1)));
    }
    for (String scope : scopes) {
      fields.add(Map.entry("scope", scope));
    }
    HttpResponse<String> response =
        browser.send(
            HttpRequest.newBuilder(URI.create(BRIDGE + "/oauth2/authorize"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "text/html")
                .POST(HttpRequest.BodyPublishers.ofString(formBody(fields)))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(302);
    return location(URI.create(BRIDGE + "/oauth2/authorize"), response);
  }

  private HttpResponse<String> exchangeMcpCode(
      String clientId, String redirectUri, String code, String verifier, String resource)
      throws Exception {
    String form =
        formBody(
            List.of(
                Map.entry("grant_type", "authorization_code"),
                Map.entry("code", code),
                Map.entry("redirect_uri", redirectUri),
                Map.entry("client_id", clientId),
                Map.entry("code_verifier", verifier),
                Map.entry("resource", resource)));
    return HttpClient.newHttpClient()
        .send(
            HttpRequest.newBuilder(URI.create(BRIDGE + "/oauth2/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build(),
            HttpResponse.BodyHandlers.ofString());
  }

  private static URI authorizeUri(
      String clientId,
      String redirectUri,
      String scope,
      String state,
      String codeChallenge,
      String resource) {
    return URI.create(
        BRIDGE
            + "/oauth2/authorize?response_type=code&client_id="
            + encode(clientId)
            + "&scope="
            + encode(scope)
            + "&redirect_uri="
            + encode(redirectUri)
            + "&state="
            + encode(state)
            + "&code_challenge="
            + codeChallenge
            + "&code_challenge_method=S256&resource="
            + encode(resource));
  }

  private HttpResponse<String> get(URI uri) throws Exception {
    return browser.send(
        HttpRequest.newBuilder(uri).header("Accept", "text/html").GET().build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private static String formBody(List<Map.Entry<String, String>> fields) {
    return fields.stream()
        .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
        .collect(Collectors.joining("&"));
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
                  "{\"access_token\":\"gho_stub\",\"token_type\":\"bearer\",\"scope\":\"read:user,user:email\"}"));
      server.createContext(
          "/user/emails",
          exchange ->
              respond(
                  exchange,
                  "[{\"email\":\"pat@example.com\",\"primary\":true,\"verified\":true}]"));
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
