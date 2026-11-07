package io.github.aaronfilson.studiomanager.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.StudioRoles;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * A token holds what its person holds now and its scopes allow; MCP needs {@code mcp.use} and each
 * tool its own permission (plan 21, lane R5).
 */
@IntegrationTest
class TokenPermissionsTests {

  @Autowired MockMvcTester mvc;
  @Autowired StudioRoles studioRoles;
  @Autowired JdbcClient jdbc;
  @Autowired TransactionTemplate transactions;

  private final TestUser owner = TestUser.named("Olive");
  private final TestUser member = TestUser.named("Mia");
  private UUID studioId;
  private UUID ownerId;
  private UUID memberId;

  @BeforeEach
  void studio() {
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
    String code = read(post(owner, path("/invites"), "{\"role\": \"MEMBER\"}"), "$.code");
    assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(member.token()))
        .hasStatusOk();
    ownerId = accountOf(owner);
    memberId = accountOf(member);
  }

  @Test
  void aMemberWithoutApiTokensCantMakeOne() {
    assertThat(post(member, path("/api-tokens"), "{\"name\": \"Mine\", \"scopes\": [\"read\"]}"))
        .hasStatus(HttpStatus.FORBIDDEN);
  }

  @Test
  void aMemberGrantedTokensAndMcpUsesTheReadToolsTheirRoleAllows() {
    grantMembers(Permission.API_TOKENS_OWN, Permission.MCP_USE);
    String token = makeToken(member, "[\"read\"]");

    MvcTestResult kilns = call(token, "kilns");
    assertThat(kilns).hasStatusOk();
    assertThat(kilns).bodyJson().extractingPath("$.result.isError").isEqualTo(false);

    // Its REST twin needs maintenance.manage, which members don't hold
    MvcTestResult due = call(token, "maintenance_due");
    assertThat(due).bodyJson().extractingPath("$.result.isError").isEqualTo(true);
    assertThat(due)
        .bodyJson()
        .extractingPath("$.result.content[0].text")
        .asString()
        .contains("maintenance.manage");
  }

  @Test
  void scopesNarrowWhatATokenHolds() {
    grantMembers(Permission.API_TOKENS_OWN, Permission.MCP_USE);
    String token = makeToken(member, "[\"read\"]");

    List<String> held = permissionsOf(token);
    assertThat(held).contains("kilns.read", "mcp.use", "studio.read");
    // The person holds these; a read-only token doesn't
    assertThat(held).doesNotContain("pieces.intake_own", "api_tokens.own", "glazes.own.edit");

    // An owner's read token never holds money, access or anything owner-only
    String ownerToken = makeToken(owner, "[\"read\"]");
    assertThat(permissionsOf(ownerToken))
        .contains("pieces.move")
        .doesNotContain("money.read", "studio.export", "members.invite", "proposals.create");
  }

  @Test
  void aChangeToThePersonOrTheirRoleAppliesToTheTokenOnTheNextRequest() {
    grantMembers(Permission.API_TOKENS_OWN, Permission.MCP_USE);
    String token = makeToken(member, "[\"read\"]");
    assertThat(permissionsOf(token)).doesNotContain("pieces.move");

    // Made staff: the token sees what staff see, within its scope
    asOwner(() ->
        transactions.execute(tx ->
            jdbc.sql(
                    "update studio_membership set role = 'STAFF' where studio_id = :studio and account_id = :account")
                .param("studio", studioId)
                .param("account", memberId)
                .update()));
    assertThat(permissionsOf(token)).contains("pieces.move");

    // Back to member, and the member role loses mcp.use: MCP turns the token away
    asOwner(() ->
        transactions.execute(tx ->
            jdbc.sql(
                    "update studio_membership set role = 'MEMBER' where studio_id = :studio and account_id = :account")
                .param("studio", studioId)
                .param("account", memberId)
                .update()));
    grantMembers(Permission.API_TOKENS_OWN);
    assertThat(rpc(token, "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"ping\"}"))
        .hasStatus(HttpStatus.FORBIDDEN);

    // Without api_tokens.own the token stops working at all
    grantMembers();
    assertThat(mvc.get().uri(path("/kilns")).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .hasStatus(HttpStatus.FORBIDDEN);
  }

  /** Members hold their defaults plus {@code extra}; the cache drops on commit. */
  private void grantMembers(Permission... extra) {
    Set<Permission> held = EnumSet.copyOf(Permission.defaultsFor(StudioRole.MEMBER));
    held.addAll(List.of(extra));
    asOwner(() -> studioRoles.setPermissions(StudioRole.MEMBER, held));
  }

  private String makeToken(TestUser user, String scopes) {
    MvcTestResult created =
        post(
            user,
            path("/api-tokens"),
            "{\"name\": \"Assistant\", \"scopes\": %s}".formatted(scopes));
    assertThat(created).hasStatus2xxSuccessful();
    return read(created, "$.secret");
  }

  private List<String> permissionsOf(String token) {
    MvcTestResult result =
        mvc.get()
            .uri(path("/me/permissions"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .exchange();
    assertThat(result).hasStatusOk();
    return JsonPath.read(body(result), "$.permissions");
  }

  private MvcTestResult call(String token, String tool) {
    return rpc(
        token,
        """
        {"jsonrpc": "2.0", "id": 9, "method": "tools/call", "params": {"name": "%s", "arguments": {}}}
        """
            .formatted(tool));
  }

  private MvcTestResult rpc(String bearer, String message) {
    return mvc.post()
        .uri(path("/mcp"))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
        .content(message)
        .exchange();
  }

  private <T> T asOwner(ScopedValue.CallableOp<T, RuntimeException> operation) {
    return TenantContext.callAs(new TenantContext(ownerId, studioId, StudioRole.OWNER), operation);
  }

  private UUID accountOf(TestUser user) {
    return UUID.fromString(
        read(mvc.get().uri("/api/v1/me").with(user.token()).exchange(), "$.account.id"));
  }

  private MvcTestResult post(TestUser user, String uri, String json) {
    return mvc.post()
        .uri(uri)
        .with(user.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .exchange();
  }

  private String path(String suffix) {
    return "/api/v1/studios/" + studioId + suffix;
  }

  private static String body(MvcTestResult result) {
    try {
      return result.getResponse().getContentAsString();
    } catch (IOException unreadable) {
      throw new IllegalStateException(unreadable);
    }
  }

  private static String read(MvcTestResult result, String jsonPath) {
    return JsonPath.read(body(result), jsonPath).toString();
  }
}
