package io.github.aaronfilson.studiomanager.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.io.IOException;
import java.util.List;
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

/** The MCP server, spoken to the way a client such as Claude does, with a personal API token. */
@IntegrationTest
class McpApiTests {

  @Autowired MockMvcTester mvc;

  @Autowired JdbcClient jdbc;

  @Autowired TransactionTemplate transactions;

  private final TestUser owner = TestUser.named("Olive");
  private UUID studioId;
  private String token;

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
    token =
        read(
            post(owner, path("/api-tokens"), "{\"name\": \"Claude\", \"scopes\": [\"read\"]}"),
            "$.secret");
  }

  @Test
  void aClientConnectsAndListsTools() {
    MvcTestResult init =
        rpc(
            token,
            """
            {"jsonrpc": "2.0", "id": 1, "method": "initialize",
             "params": {"protocolVersion": "2025-06-18", "capabilities": {},
                        "clientInfo": {"name": "test", "version": "1"}}}
            """);
    assertThat(init).hasStatusOk();
    assertThat(init).bodyJson().extractingPath("$.id").isEqualTo(1);
    assertThat(init).bodyJson().extractingPath("$.result.protocolVersion").isEqualTo("2025-06-18");
    assertThat(init)
        .bodyJson()
        .extractingPath("$.result.serverInfo.name")
        .isEqualTo("studiomanager");
    assertThat(init).bodyJson().extractingPath("$.result.capabilities.tools").isNotNull();
    assertThat(init).bodyJson().extractingPath("$.result.capabilities.resources").isNotNull();

    assertThat(rpc(token, "{\"jsonrpc\": \"2.0\", \"method\": \"notifications/initialized\"}"))
        .hasStatus(HttpStatus.ACCEPTED);

    // Read-only token only sees read tools (per-caller tools/list filtering)
    MvcTestResult readOnlyList =
        rpc(token, "{\"jsonrpc\": \"2.0\", \"id\": \"2\", \"method\": \"tools/list\"}");
    assertThat(readOnlyList).bodyJson().extractingPath("$.id").isEqualTo("2");
    List<String> readOnlyNames = JsonPath.read(body(readOnlyList), "$.result.tools[*].name");
    assertThat(readOnlyNames)
        .contains(
            "studio_today",
            "find_pieces",
            "kilns",
            "kiln_queue",
            "member_lookup",
            "low_supplies",
            "maintenance_due",
            "glaze_batch")
        .doesNotContain(
            "propose_purchase_request",
            "propose_maintenance_task",
            "propose_notice",
            "log_glaze_mix");

    // Token with read + propose + write:glazes sees all non-destructive tools
    String fullToken =
        read(
            post(
                owner,
                path("/api-tokens"),
                "{\"name\": \"Full Assistant\", \"scopes\": [\"read\", \"propose\", \"write:glazes\"]}"),
            "$.secret");
    MvcTestResult list =
        rpc(fullToken, "{\"jsonrpc\": \"2.0\", \"id\": \"2b\", \"method\": \"tools/list\"}");
    List<String> names = JsonPath.read(body(list), "$.result.tools[*].name");
    List<Boolean> destructive =
        JsonPath.read(body(list), "$.result.tools[*].annotations.destructiveHint");
    assertThat(destructive).hasSameSizeAs(names).containsOnly(false);
    List<String> writing =
        JsonPath.read(body(list), "$.result.tools[?(@.annotations.readOnlyHint == false)].name");
    assertThat(writing)
        .containsExactlyInAnyOrder(
            "propose_purchase_request",
            "propose_maintenance_task",
            "propose_notice",
            "log_glaze_mix");
  }

  @Test
  void findPiecesKilnQueueMemberLookupAndProposeNoticeWorkEndToEnd() {
    // Seed a piece and a kiln
    assertThat(
            post(
                owner,
                path("/pieces"),
                """
                {"description": "Speckled ramen bowl", "clayBody": "B-Mix", "makerMark": "OL", "size": {}}
                """))
        .hasStatus(HttpStatus.CREATED);
    String kilnId =
        read(
            post(
                owner,
                path("/kilns"),
                "{\"name\": \"Skutt 1027\", \"kind\": \"ELECTRIC\", \"maxPiecesPerMember\": 4}"),
            "$.id");

    MvcTestResult found = call("find_pieces", "{\"query\": \"ramen\"}");
    assertThat(found).bodyJson().extractingPath("$.result.isError").isEqualTo(false);
    assertThat(found)
        .bodyJson()
        .extractingPath("$.result.structuredContent.result.totalMatches")
        .isEqualTo(1);
    assertThat(found)
        .bodyJson()
        .extractingPath("$.result.structuredContent.result.items[0].description")
        .isEqualTo("Speckled ramen bowl");

    MvcTestResult queue =
        call("kiln_queue", "{\"kilnId\": \"%s\", \"kind\": \"BISQUE\"}".formatted(kilnId));
    assertThat(queue).bodyJson().extractingPath("$.result.isError").isEqualTo(false);
    assertThat(queue)
        .bodyJson()
        .extractingPath("$.result.structuredContent.result.items")
        .isNotNull();

    MvcTestResult members = call("member_lookup", "{\"query\": \"olive\"}");
    assertThat(members).bodyJson().extractingPath("$.result.isError").isEqualTo(false);
    assertThat(members)
        .bodyJson()
        .extractingPath("$.result.structuredContent.result[0].displayName")
        .isEqualTo("Olive");

    // Propose a studio notice with a propose-scoped token, then approve it as the owner
    String proposeToken =
        read(
            post(
                owner,
                path("/api-tokens"),
                "{\"name\": \"Notice Bot\", \"scopes\": [\"read\", \"propose\"]}"),
            "$.secret");
    MvcTestResult proposed =
        rpc(
            proposeToken,
            """
            {"jsonrpc": "2.0", "id": 11, "method": "tools/call",
             "params": {"name": "propose_notice",
                        "arguments": {"title": "Kiln room closed Friday",
                                      "body": "Rewiring Skutt 1027 from 9am to 2pm.",
                                      "reason": "Scheduled maintenance window"}}}
            """);
    assertThat(proposed).bodyJson().extractingPath("$.result.isError").isEqualTo(false);
    String actionId = read(proposed, "$.result.structuredContent.result.suggestionId");

    MvcTestResult pending = mvc.get().uri(path("/pending-actions")).with(owner.token()).exchange();
    assertThat(pending).hasStatusOk();
    assertThat(pending).bodyJson().extractingPath("$.items[0].kind").isEqualTo("STUDIO_NOTICE");
    assertThat(pending)
        .bodyJson()
        .extractingPath("$.items[0].studioNotice.title")
        .isEqualTo("Kiln room closed Friday");
    int version = JsonPath.read(body(pending), "$.items[0].version");

    MvcTestResult approved =
        mvc.put()
            .uri(path("/pending-actions/" + actionId + "/decision"))
            .with(owner.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"decision\": \"APPROVED\", \"expectedVersion\": %d}".formatted(version))
            .exchange();
    assertThat(approved).hasStatusOk();
    assertThat(approved).bodyJson().extractingPath("$.status").isEqualTo("APPROVED");

    MvcTestResult inbox = mvc.get().uri(path("/notifications")).with(owner.token()).exchange();
    assertThat(inbox).hasStatusOk();
    assertThat(JsonPath.<List<String>>read(body(inbox), "$.items[*].title"))
        .contains("Kiln room closed Friday");
  }

  @Test
  void resourcesExposeStudioPoliciesAndGlazes() {
    String glazeId =
        read(
            post(
                owner,
                path("/glazes"),
                """
                {"ownerKind": "STUDIO", "name": "Shino", "cone": "10",
                 "lines": [{"kind": "BASE", "material": "Nepheline Syenite", "amount": "75", "unit": "PARTS"},
                           {"kind": "BASE", "material": "EPK", "amount": "25", "unit": "PARTS"}]}
                """),
            "$.id");

    MvcTestResult resourcesList =
        rpc(token, "{\"jsonrpc\": \"2.0\", \"id\": 20, \"method\": \"resources/list\"}");
    assertThat(resourcesList).hasStatusOk();
    assertThat(JsonPath.<List<String>>read(body(resourcesList), "$.result.resources[*].uri"))
        .contains("studio://policies");

    MvcTestResult templatesList =
        rpc(token, "{\"jsonrpc\": \"2.0\", \"id\": 21, \"method\": \"resources/templates/list\"}");
    assertThat(templatesList).hasStatusOk();
    assertThat(
            JsonPath.<List<String>>read(
                body(templatesList), "$.result.resourceTemplates[*].uriTemplate"))
        .contains("studio://glazes/{id}");

    MvcTestResult policiesRead =
        rpc(
            token,
            """
            {"jsonrpc": "2.0", "id": 22, "method": "resources/read",
             "params": {"uri": "studio://policies"}}
            """);
    assertThat(policiesRead).hasStatusOk();
    String policiesJson = read(policiesRead, "$.result.contents[0].text");
    assertThat(policiesJson).contains("Clay Club", "pickupHoldDays", "feeSchedule");

    MvcTestResult glazeRead =
        rpc(
            token,
            """
            {"jsonrpc": "2.0", "id": 23, "method": "resources/read",
             "params": {"uri": "studio://glazes/%s"}}
            """
                .formatted(glazeId));
    assertThat(glazeRead).hasStatusOk();
    String glazeJson = read(glazeRead, "$.result.contents[0].text");
    assertThat(glazeJson).contains("Shino", "Nepheline Syenite");

    MvcTestResult missingGlaze =
        rpc(
            token,
            """
            {"jsonrpc": "2.0", "id": 24, "method": "resources/read",
             "params": {"uri": "studio://glazes/%s"}}
            """
                .formatted(UUID.randomUUID()));
    assertThat(missingGlaze).hasStatusOk();
    assertThat(missingGlaze).bodyJson().extractingPath("$.error.code").isEqualTo(-32002);
  }

  @Test
  void toolsRunAsThePersonInTheirStudio() {
    MvcTestResult today = call("studio_today", "{}");
    assertThat(today).bodyJson().extractingPath("$.result.isError").isEqualTo(false);
    assertThat(today).bodyJson().extractingPath("$.result.structuredContent.result").isNotNull();

    String glazeId =
        read(
            post(
                owner,
                path("/glazes"),
                """
                {"ownerKind": "STUDIO", "name": "Celadon",
                 "lines": [{"kind": "BASE", "material": "Feldspar", "amount": "40", "unit": "PARTS"},
                           {"kind": "BASE", "material": "Silica", "amount": "60", "unit": "PARTS"}]}
                """),
            "$.id");
    MvcTestResult batch =
        call(
            "glaze_batch",
            """
            {"glazeId": "%s", "size": 5, "unit": "KILOGRAM"}\
            """
                .formatted(glazeId));
    assertThat(batch).bodyJson().extractingPath("$.result.isError").isEqualTo(false);
    assertThat(batch)
        .bodyJson()
        .extractingPath("$.result.structuredContent.result.lines[1].weight")
        .isEqualTo(3.0);

    // A tool's own failure is a result the model can read, not a protocol error
    MvcTestResult wrong =
        call("glaze_batch", "{\"glazeId\": \"nope\", \"size\": 5, \"unit\": \"KILOGRAM\"}");
    assertThat(wrong).bodyJson().extractingPath("$.result.isError").isEqualTo(true);
    assertThat(wrong)
        .bodyJson()
        .extractingPath("$.result.content[0].text")
        .isEqualTo("glazeId must be an id.");
  }

  @Test
  void protocolMistakesGetJsonRpcErrors() {
    assertThat(call("delete_everything", "{}"))
        .bodyJson()
        .extractingPath("$.error.code")
        .isEqualTo(-32602);
    assertThat(rpc(token, "{\"jsonrpc\": \"2.0\", \"id\": 3, \"method\": \"resources/write\"}"))
        .bodyJson()
        .extractingPath("$.error.code")
        .isEqualTo(-32601);
    assertThat(rpc(token, "{\"id\": 4, \"method\": \"ping\"}")).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(
            mvc.post()
                .uri(path("/mcp"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("MCP-Protocol-Version", "1999-01-01")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jsonrpc\": \"2.0\", \"id\": 5, \"method\": \"ping\"}"))
        .hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(mvc.get().uri(path("/mcp")).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .hasStatus(HttpStatus.METHOD_NOT_ALLOWED)
        .hasHeader(HttpHeaders.ALLOW, "POST");
    // Not JSON at all
    assertThat(rpc(token, "{not json")).hasStatus(HttpStatus.BAD_REQUEST);
    // Streamable HTTP requires a client to accept both JSON and an event stream
    assertThat(
            mvc.post()
                .uri(path("/mcp"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content("{\"jsonrpc\": \"2.0\", \"id\": 6, \"method\": \"ping\"}"))
        .hasStatus(HttpStatus.BAD_REQUEST);
  }

  @Test
  void eachToolCallIsLoggedWithItsToken() {
    assertThat(call("kilns", "{}")).bodyJson().extractingPath("$.result.isError").isEqualTo(false);
    // Listing tools is free, and isn't logged
    rpc(token, "{\"jsonrpc\": \"2.0\", \"id\": 2, \"method\": \"tools/list\"}");

    UUID ownerId =
        UUID.fromString(
            read(mvc.get().uri("/api/v1/me").with(owner.token()).exchange(), "$.account.id"));
    String tokenId =
        read(mvc.get().uri(path("/api-tokens")).with(owner.token()).exchange(), "$[0].id");
    // The tool ran with the token bound as "via", the same binding the histories record
    List<String> logged =
        TenantContext.callAs(
            new TenantContext(ownerId, studioId, StudioRole.OWNER),
            () ->
                transactions.execute(tx ->
                    jdbc.sql(
                            """
                            select target || ' ' || outcome || ' ' || token_id from api_call_log
                            where studio_id = :studioId and channel = 'MCP'
                            """)
                        .param("studioId", studioId)
                        .query(String.class)
                        .list()));
    assertThat(logged).containsExactly("kilns OK " + tokenId);
  }

  @Test
  void aBrowserOnAnotherSiteOrATokenWithoutReadIsTurnedAway() {
    assertThat(
            mvc.post()
                .uri(path("/mcp"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(HttpHeaders.ORIGIN, "https://evil.example")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"ping\"}"))
        .hasStatus(HttpStatus.FORBIDDEN);

    String writeOnly =
        read(
            post(
                owner,
                path("/api-tokens"),
                "{\"name\": \"Pieces only\", \"scopes\": [\"write:pieces\"]}"),
            "$.secret");
    assertThat(rpc(writeOnly, "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"ping\"}"))
        .hasStatus(HttpStatus.FORBIDDEN);
  }

  @Test
  void protectedResourceMetadataReturns404WhenMcpOAuthIsDisabled() {
    assertThat(mvc.get().uri("/.well-known/oauth-protected-resource"))
        .hasStatus(HttpStatus.NOT_FOUND);
    assertThat(
            mvc.get()
                .uri("/.well-known/oauth-protected-resource/api/v1/studios/{id}/mcp", studioId))
        .hasStatus(HttpStatus.NOT_FOUND);

    MvcTestResult unauthenticated =
        mvc.post()
            .uri(path("/mcp"))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
            .content("{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"ping\"}")
            .exchange();
    assertThat(unauthenticated).hasStatus(HttpStatus.UNAUTHORIZED);
    assertThat(unauthenticated.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE))
        .doesNotContain("resource_metadata");
  }

  @Test
  void logGlazeMixRequiresWriteGlazesScopeAndLogsStudioMixes() {
    String glazeId =
        read(
            post(
                owner,
                path("/glazes"),
                """
                {"ownerKind": "STUDIO", "name": "Studio Tenmoku", "targetSpecificGravity": "1.45",
                 "lines": [{"kind": "BASE", "material": "Custer Feldspar", "amount": "50", "unit": "PARTS"},
                           {"kind": "BASE", "material": "Silica", "amount": "50", "unit": "PARTS"}]}
                """),
            "$.id");

    // Read-only token is refused even for the studio owner
    MvcTestResult denied =
        call(
            "log_glaze_mix",
            """
            {"glazeId": "%s", "dryWeightG": 5000, "waterG": 3800, "specificGravity": 1.52}
            """
                .formatted(glazeId));
    assertThat(denied).bodyJson().extractingPath("$.result.isError").isEqualTo(true);
    assertThat(denied)
        .bodyJson()
        .extractingPath("$.result.content[0].text")
        .asString()
        .contains("write:glazes");

    // Token with read + write:glazes succeeds and returns the mix with its SG warning
    String writeGlazesToken =
        read(
            post(
                owner,
                path("/api-tokens"),
                "{\"name\": \"Glaze Helper\", \"scopes\": [\"read\", \"write:glazes\"]}"),
            "$.secret");
    MvcTestResult logged =
        rpc(
            writeGlazesToken,
            """
            {"jsonrpc": "2.0", "id": 10, "method": "tools/call",
             "params": {"name": "log_glaze_mix",
                        "arguments": {"glazeId": "%s", "dryWeightG": 5000, "waterG": 3800, "specificGravity": 1.52}}}
            """
                .formatted(glazeId));
    assertThat(logged).bodyJson().extractingPath("$.result.isError").isEqualTo(false);
    assertThat(logged)
        .bodyJson()
        .extractingPath("$.result.structuredContent.result.readings[0].offTarget")
        .isEqualTo(true);
  }

  private MvcTestResult call(String tool, String arguments) {
    return rpc(
        token,
        """
        {"jsonrpc": "2.0", "id": 9, "method": "tools/call", "params": {"name": "%s", "arguments": %s}}
        """
            .formatted(tool, arguments));
  }

  private MvcTestResult rpc(String bearer, String message) {
    return mvc.post()
        .uri(path("/mcp"))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer)
        .contentType(MediaType.APPLICATION_JSON)
        // Streamable HTTP: a client must accept both, even from a server that only answers JSON
        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
        .content(message)
        .exchange();
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
