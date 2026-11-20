package io.github.aaronfilson.studiomanager.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.SystemActor;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.StudioRoles;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * End-to-end Phase 2 MCP OAuth integration tests (plan 17 step 5, plan 18 step 3, ADR 26): RFC 9728
 * metadata, {@code WWW-Authenticate} discovery headers, real RS256 JWKS verification, strict
 * MCP-only route isolation, cross-studio denial, and live permission/scope enforcement.
 */
@IntegrationTest
class McpOAuthTests {

  private static final String MCP_ISSUER = "https://auth.studiomanager.test";
  private static final String MCP_AUDIENCE = "https://api.studiomanager.test/mcp";

  private static final RSAKey TRUSTED_KEY = generateRsaKey("mcp-key-1");
  private static final RSAKey UNTRUSTED_KEY = generateRsaKey("rogue-key");
  private static final HttpServer JWKS_SERVER = startJwksServer(TRUSTED_KEY);

  @DynamicPropertySource
  static void mcpOAuth(DynamicPropertyRegistry registry) {
    String jwksUri = "http://127.0.0.1:" + JWKS_SERVER.getAddress().getPort() + "/oauth2/jwks";
    registry.add("studiomanager.mcp.oauth.issuer", () -> MCP_ISSUER);
    registry.add("studiomanager.mcp.oauth.jwk-set-uri", () -> jwksUri);
    registry.add("studiomanager.mcp.oauth.audience", () -> MCP_AUDIENCE);
  }

  @AfterAll
  static void stopJwksServer() {
    JWKS_SERVER.stop(0);
  }

  @Autowired MockMvcTester mvc;
  @Autowired StudioRoles studioRoles;
  @Autowired JdbcClient jdbc;
  @Autowired TransactionTemplate transactions;

  private final TestUser owner = TestUser.named("Olive");
  private final TestUser staff = TestUser.named("Sam");
  private UUID studioA;
  private UUID studioB;
  private UUID ownerId;
  private UUID staffId;

  @BeforeEach
  void setUp() {
    studioA = createStudio(owner, "Studio A");
    studioB = createStudio(owner, "Studio B");
    ownerId = accountOf(owner);
    staffId = join(staff, "STAFF");
  }

  @Test
  void rfc9728MetadataAndWwwAuthenticateHeaderAdvertiseAuthorizationServer() {
    MvcTestResult root = mvc.get().uri("/.well-known/oauth-protected-resource").exchange();
    assertThat(root).hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
    assertThat(root).bodyJson().extractingPath("$.resource").isEqualTo(MCP_AUDIENCE);
    assertThat(root).bodyJson().extractingPath("$.authorization_servers[0]").isEqualTo(MCP_ISSUER);
    assertThat(JsonPath.<List<String>>read(body(root), "$.scopes_supported"))
        .containsExactly("read", "propose");
    assertThat(JsonPath.<List<String>>read(body(root), "$.bearer_methods_supported"))
        .containsExactly("header");

    MvcTestResult perStudio =
        mvc.get()
            .uri("/.well-known/oauth-protected-resource/api/v1/studios/{id}/mcp", studioA)
            .exchange();
    assertThat(perStudio).hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
    assertThat(perStudio)
        .bodyJson()
        .extractingPath("$.resource")
        .isEqualTo("https://api.studiomanager.test/api/v1/studios/" + studioA + "/mcp");
    assertThat(perStudio)
        .bodyJson()
        .extractingPath("$.authorization_servers[0]")
        .isEqualTo(MCP_ISSUER);

    // Malformed studio id is rejected rather than reflected into metadata
    assertThat(mvc.get().uri("/.well-known/oauth-protected-resource/api/v1/studios/not-a-uuid/mcp"))
        .hasStatus(HttpStatus.BAD_REQUEST);

    // Unauthenticated MCP request returns 401 with WWW-Authenticate resource_metadata
    MvcTestResult unauthenticated =
        mvc.post()
            .uri(mcpPath(studioA))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
            .content("{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"ping\"}")
            .exchange();
    assertThat(unauthenticated).hasStatus(HttpStatus.UNAUTHORIZED);
    String expectedMetadataUrl =
        "https://api.studiomanager.test/.well-known/oauth-protected-resource/api/v1/studios/"
            + studioA
            + "/mcp";
    assertThat(unauthenticated.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE))
        .isEqualTo("Bearer resource_metadata=\"" + expectedMetadataUrl + "\"");
  }

  @Test
  void validMcpOAuthJwtSucceedsOnMcpAndEnforcesScopesAndLivePermissions() throws Exception {
    // Works with the Entra subject AND per-studio RFC 8707 audience
    String perStudioAudience = "https://api.studiomanager.test/api/v1/studios/" + studioA + "/mcp";
    String readOnlyJwt = mintJwt(TRUSTED_KEY, owner.subject(), studioA, "read", perStudioAudience);

    MvcTestResult today = callTool(studioA, readOnlyJwt, "studio_today", "{}");
    assertThat(today).hasStatusOk();
    assertThat(today).bodyJson().extractingPath("$.result.isError").isEqualTo(false);

    // Also works with account UUID in sub AND base MCP audience
    String accountSubJwt = mintJwt(TRUSTED_KEY, ownerId.toString(), studioA, "read", MCP_AUDIENCE);
    assertThat(callTool(studioA, accountSubJwt, "kilns", "{}"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.result.isError")
        .isEqualTo(false);

    // Read-only MCP OAuth token cannot propose changes
    String itemId = createSupplyItem(studioA, "Cone 6 B-Mix");
    MvcTestResult refusedPropose =
        callTool(
            studioA,
            readOnlyJwt,
            "propose_purchase_request",
            "{\"itemId\": \"%s\", \"quantity\": 5}".formatted(itemId));
    assertThat(refusedPropose).hasStatusOk();
    assertThat(refusedPropose).bodyJson().extractingPath("$.result.isError").isEqualTo(true);
    assertThat(refusedPropose)
        .bodyJson()
        .extractingPath("$.result.content[0].text")
        .asString()
        .contains("propose");

    // Token with read + propose creates a pending action for human approval
    String proposeJwt =
        mintJwt(TRUSTED_KEY, owner.subject(), studioA, "read propose", perStudioAudience);
    MvcTestResult proposed =
        callTool(
            studioA,
            proposeJwt,
            "propose_purchase_request",
            "{\"itemId\": \"%s\", \"quantity\": 5, \"reason\": \"Running low\"}".formatted(itemId));
    assertThat(proposed).hasStatusOk();
    assertThat(proposed).bodyJson().extractingPath("$.result.isError").isEqualTo(false);
    assertThat(proposed)
        .bodyJson()
        .extractingPath("$.result.structuredContent.result.status")
        .isEqualTo("PENDING");

    // Token without read scope is refused on MCP
    String proposeOnly = mintJwt(TRUSTED_KEY, owner.subject(), studioA, "propose", MCP_AUDIENCE);
    assertThat(rpc(studioA, proposeOnly, "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"ping\"}"))
        .hasStatus(HttpStatus.FORBIDDEN);

    // Regular Entra tokens and smpat_ personal tokens continue working unchanged
    String smpat =
        read(
            post(
                owner,
                "/api/v1/studios/" + studioA + "/api-tokens",
                "{\"name\": \"Personal\", \"scopes\": [\"read\"]}"),
            "$.secret");
    assertThat(callTool(studioA, smpat, "kilns", "{}"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.result.isError")
        .isEqualTo(false);
    assertThat(
            mvc.post()
                .uri(mcpPath(studioA))
                .with(owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                .content("{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"ping\"}"))
        .hasStatusOk();
  }

  @Test
  void mcpOAuthJwtForStudioAGets403OnStudioBAndInvalidTokensGet401() throws Exception {
    String studioABaseAud = mintJwt(TRUSTED_KEY, owner.subject(), studioA, "read", MCP_AUDIENCE);
    assertThat(
            rpc(studioB, studioABaseAud, "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"ping\"}"))
        .hasStatus(HttpStatus.FORBIDDEN);

    String studioAPerStudioAud =
        mintJwt(
            TRUSTED_KEY,
            owner.subject(),
            studioA,
            "read",
            "https://api.studiomanager.test/api/v1/studios/" + studioA + "/mcp");
    assertThat(
            rpc(
                studioB,
                studioAPerStudioAud,
                "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"ping\"}"))
        .hasStatus(HttpStatus.FORBIDDEN);

    // Wrong audience -> 401
    String wrongAudience =
        mintJwt(TRUSTED_KEY, owner.subject(), studioA, "read", "https://other.example/mcp");
    assertThat(
            rpc(studioA, wrongAudience, "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"ping\"}"))
        .hasStatus(HttpStatus.UNAUTHORIZED);

    // Mismatched per-studio audience (studio B's URL inside a token claiming studio A) -> 401
    String mismatchedAudience =
        mintJwt(
            TRUSTED_KEY,
            owner.subject(),
            studioA,
            "read",
            "https://api.studiomanager.test/api/v1/studios/" + studioB + "/mcp");
    assertThat(
            rpc(
                studioA,
                mismatchedAudience,
                "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"ping\"}"))
        .hasStatus(HttpStatus.UNAUTHORIZED);

    // Signed by an untrusted key -> 401
    String rogueSigned = mintJwt(UNTRUSTED_KEY, owner.subject(), studioA, "read", MCP_AUDIENCE);
    assertThat(rpc(studioA, rogueSigned, "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"ping\"}"))
        .hasStatus(HttpStatus.UNAUTHORIZED);

    // Expired token -> 401 with WWW-Authenticate resource_metadata
    String expired =
        mintJwtWithExpiry(
            TRUSTED_KEY,
            owner.subject(),
            studioA,
            "read",
            MCP_AUDIENCE,
            Instant.now().minusSeconds(600));
    MvcTestResult expiredResult =
        rpc(studioA, expired, "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"ping\"}");
    assertThat(expiredResult).hasStatus(HttpStatus.UNAUTHORIZED);
    assertThat(expiredResult.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE))
        .contains("resource_metadata=");
  }

  @Test
  void mcpOAuthJwtIsRefusedOnAllNormalRestApiEndpoints() throws Exception {
    String mcpJwt = mintJwt(TRUSTED_KEY, owner.subject(), studioA, "read propose", MCP_AUDIENCE);

    // Normal REST endpoints, money, access, tokens, webhooks, imports, invoices, export,
    // pending-action approvals, /me, and /admin MUST ALL refuse MCP OAuth JWTs!
    List<String> refusedGets =
        List.of(
            "/api/v1/me",
            "/api/v1/studios/" + studioA + "/pieces",
            "/api/v1/studios/" + studioA + "/kilns",
            "/api/v1/studios/" + studioA + "/balances",
            "/api/v1/studios/" + studioA + "/ledger/journal?from=2026-01-01&to=2026-02-01",
            "/api/v1/studios/" + studioA + "/api-tokens",
            "/api/v1/studios/" + studioA + "/webhooks",
            "/api/v1/studios/" + studioA + "/invoices",
            "/api/v1/studios/" + studioA + "/export",
            "/api/v1/studios/" + studioA + "/pending-actions",
            "/api/v1/studios/" + studioA + "/roles",
            "/api/v1/admin/rate-limits");
    for (String uri : refusedGets) {
      assertThat(mvc.get().uri(uri).header(HttpHeaders.AUTHORIZATION, "Bearer " + mcpJwt))
          .as("GET %s must refuse MCP OAuth JWT", uri)
          .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    assertThat(
            mvc.put()
                .uri(
                    "/api/v1/studios/{studioId}/pending-actions/{id}/decision",
                    studioA,
                    UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + mcpJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\": \"APPROVED\", \"expectedVersion\": 0}"))
        .hasStatus(HttpStatus.UNAUTHORIZED);

    // System actor UUID in sub can never authenticate via MCP OAuth
    String systemSubJwt =
        mintJwt(
            TRUSTED_KEY,
            SystemActor.MAINTENANCE_REMINDERS.accountId().toString(),
            studioA,
            "read",
            MCP_AUDIENCE);
    assertThat(
            rpc(studioA, systemSubJwt, "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"ping\"}"))
        .hasStatus(HttpStatus.FORBIDDEN);
  }

  @Test
  void demotingUserOrRevokingMcpUseImmediatelyCutsOffMcpOAuthToken() throws Exception {
    String staffJwt = mintJwt(TRUSTED_KEY, staff.subject(), studioA, "read", MCP_AUDIENCE);

    // As STAFF, Sam holds mcp.use and can call read tools
    assertThat(callTool(studioA, staffJwt, "kilns", "{}"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.result.isError")
        .isEqualTo(false);

    // Demote Sam to MEMBER -> default MEMBER lacks mcp.use -> immediately 403!
    asOwner(() ->
        transactions.execute(tx ->
            jdbc.sql(
                    "update studio_membership set role = 'MEMBER' where studio_id = :studio and account_id = :account")
                .param("studio", studioA)
                .param("account", staffId)
                .update()));
    assertThat(rpc(studioA, staffJwt, "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"ping\"}"))
        .hasStatus(HttpStatus.FORBIDDEN);

    // Grant mcp.use to MEMBER -> Sam can call kilns (kilns.read), NOT maintenance_due
    // (maintenance.manage)
    Set<Permission> memberWithMcp = EnumSet.copyOf(Permission.defaultsFor(StudioRole.MEMBER));
    memberWithMcp.add(Permission.MCP_USE);
    asOwner(() -> studioRoles.setPermissions(StudioRole.MEMBER, memberWithMcp));

    assertThat(callTool(studioA, staffJwt, "kilns", "{}"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.result.isError")
        .isEqualTo(false);
    MvcTestResult deniedTool = callTool(studioA, staffJwt, "maintenance_due", "{}");
    assertThat(deniedTool).hasStatusOk();
    assertThat(deniedTool).bodyJson().extractingPath("$.result.isError").isEqualTo(true);
    assertThat(deniedTool)
        .bodyJson()
        .extractingPath("$.result.content[0].text")
        .asString()
        .contains("maintenance.manage");

    // Revoke mcp.use from MEMBER -> the exact same token is immediately cut off again!
    asOwner(() ->
        studioRoles.setPermissions(
            StudioRole.MEMBER, EnumSet.copyOf(Permission.defaultsFor(StudioRole.MEMBER))));
    assertThat(rpc(studioA, staffJwt, "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"ping\"}"))
        .hasStatus(HttpStatus.FORBIDDEN);
  }

  private MvcTestResult callTool(UUID studioId, String bearer, String tool, String arguments) {
    return rpc(
        studioId,
        bearer,
        """
        {"jsonrpc": "2.0", "id": 9, "method": "tools/call", "params": {"name": "%s", "arguments": %s}}
        """
            .formatted(tool, arguments));
  }

  private MvcTestResult rpc(UUID studioId, String bearer, String message) {
    return mvc.post()
        .uri(mcpPath(studioId))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
        .content(message)
        .exchange();
  }

  private String createSupplyItem(UUID studioId, String name) {
    MvcTestResult created =
        post(
            owner,
            "/api/v1/studios/" + studioId + "/supplies",
            """
            {"name": "%s", "category": "CLAY", "unit": "bag", "parLevel": "10", "reorderQuantity": "20"}
            """
                .formatted(name));
    assertThat(created).hasStatus(HttpStatus.CREATED);
    return read(created, "$.id");
  }

  private UUID createStudio(TestUser user, String name) {
    MvcTestResult created =
        post(
            user,
            "/api/v1/studios",
            """
            {"name": "%s", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
            """
                .formatted(name));
    assertThat(created).hasStatus(HttpStatus.CREATED);
    return UUID.fromString(read(created, "$.id"));
  }

  private UUID join(TestUser user, String role) {
    String code =
        read(
            post(owner, "/api/v1/studios/" + studioA + "/invites", "{\"role\": \"" + role + "\"}"),
            "$.code");
    assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(user.token()))
        .hasStatusOk();
    return accountOf(user);
  }

  private UUID accountOf(TestUser user) {
    return UUID.fromString(
        read(mvc.get().uri("/api/v1/me").with(user.token()).exchange(), "$.account.id"));
  }

  private <T> T asOwner(ScopedValue.CallableOp<T, RuntimeException> operation) {
    return TenantContext.callAs(new TenantContext(ownerId, studioA, StudioRole.OWNER), operation);
  }

  private MvcTestResult post(TestUser user, String uri, String json) {
    return mvc.post()
        .uri(uri)
        .with(user.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .exchange();
  }

  private static String mcpPath(UUID studioId) {
    return "/api/v1/studios/" + studioId + "/mcp";
  }

  private static String mintJwt(
      RSAKey signingKey, String subject, UUID studioId, String scope, String audience)
      throws Exception {
    return mintJwtWithExpiry(
        signingKey, subject, studioId, scope, audience, Instant.now().plusSeconds(3600));
  }

  private static String mintJwtWithExpiry(
      RSAKey signingKey,
      String subject,
      UUID studioId,
      String scope,
      String audience,
      Instant expiresAt)
      throws Exception {
    Instant issuedAt = expiresAt.minusSeconds(3600);
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .issuer(MCP_ISSUER)
            .subject(subject)
            .audience(audience)
            .issueTime(Date.from(issuedAt))
            .expirationTime(Date.from(expiresAt))
            .claim("studio_id", studioId.toString())
            .claim("scope", scope)
            .claim("client_id", "mcp_test_client")
            .build();
    SignedJWT signed =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(), claims);
    signed.sign(new RSASSASigner(signingKey));
    return signed.serialize();
  }

  private static RSAKey generateRsaKey(String keyId) {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      KeyPair pair = generator.generateKeyPair();
      return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
          .privateKey((RSAPrivateKey) pair.getPrivate())
          .keyUse(KeyUse.SIGNATURE)
          .algorithm(JWSAlgorithm.RS256)
          .keyID(keyId)
          .build();
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static HttpServer startJwksServer(RSAKey key) {
    try {
      byte[] jwksJson = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/oauth2/jwks",
          exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jwksJson.length);
            exchange.getResponseBody().write(jwksJson);
            exchange.close();
          });
      server.start();
      return server;
    } catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static String body(MvcTestResult result) {
    try {
      return result.getResponse().getContentAsString();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String read(MvcTestResult result, String jsonPath) {
    return JsonPath.read(body(result), jsonPath).toString();
  }
}
