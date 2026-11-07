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

    assertThat(rpc(token, "{\"jsonrpc\": \"2.0\", \"method\": \"notifications/initialized\"}"))
        .hasStatus(HttpStatus.ACCEPTED);

    MvcTestResult list =
        rpc(token, "{\"jsonrpc\": \"2.0\", \"id\": \"2\", \"method\": \"tools/list\"}");
    assertThat(list).bodyJson().extractingPath("$.id").isEqualTo("2");
    List<String> names = JsonPath.read(body(list), "$.result.tools[*].name");
    assertThat(names)
        .contains("studio_today", "kilns", "low_supplies", "maintenance_due", "glaze_batch");
    // Nothing an assistant can call here is destructive. Only the propose tools aren't read-only,
    // and all they do is record a suggestion for staff
    List<Boolean> destructive =
        JsonPath.read(body(list), "$.result.tools[*].annotations.destructiveHint");
    assertThat(destructive).hasSameSizeAs(names).containsOnly(false);
    List<String> writing =
        JsonPath.read(body(list), "$.result.tools[?(@.annotations.readOnlyHint == false)].name");
    assertThat(writing)
        .containsExactlyInAnyOrder("propose_purchase_request", "propose_maintenance_task");
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
    List<String> logged =
        TenantContext.callAs(
            new TenantContext(ownerId, studioId, StudioRole.OWNER),
            () ->
                transactions.execute(tx ->
                    jdbc.sql(
                            """
                            select target || ' ' || outcome || ' ' || (token_id is not null) from api_call_log
                            where studio_id = :studioId and channel = 'MCP'
                            """)
                        .param("studioId", studioId)
                        .query(String.class)
                        .list()));
    assertThat(logged).containsExactly("kilns OK true");
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
