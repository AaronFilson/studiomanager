package io.github.aaronfilson.studiomanager.ratelimit;

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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionTemplate;

/** The MCP and token limits, with small buckets that don't refill during the test. */
@IntegrationTest
@TestPropertySource(
    properties = {
      "studiomanager.rate-limit.mcp.burst=2",
      "studiomanager.rate-limit.mcp.refill-period=1h",
      "studiomanager.rate-limit.token.burst=3",
      "studiomanager.rate-limit.token.refill-period=1h"
    })
class RateLimitApiTests {

  @Autowired MockMvcTester mvc;

  @Autowired JdbcClient jdbc;

  @Autowired TransactionTemplate transactions;

  private final TestUser owner = TestUser.named("Olive");
  private UUID studioId;
  private UUID ownerId;

  @BeforeEach
  void setUp() {
    studioId =
        UUID.fromString(
            read(
                post(
                    "/api/v1/studios",
                    """
                    {"name": "Clay Club", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
                    """),
                "$.id"));
    ownerId =
        UUID.fromString(
            read(mvc.get().uri("/api/v1/me").with(owner.token()).exchange(), "$.account.id"));
  }

  @Test
  void restCallsWithATokenGet429OnceTheBucketIsEmpty() {
    String token = token("Sync script");
    for (int call = 0; call < 3; call++) {
      assertThat(withToken(token, path("/kilns"))).hasStatusOk();
    }
    MvcTestResult refused = withToken(token, path("/kilns"));
    assertThat(refused).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(refused).headers().containsHeader(HttpHeaders.RETRY_AFTER);
    // A whole number of seconds, at least one
    assertThat(refused.getResponse().getHeader(HttpHeaders.RETRY_AFTER)).matches("[1-9][0-9]*");

    // A second token of the same person shares the bucket
    assertThat(withToken(token("Second script"), path("/kilns")))
        .hasStatus(HttpStatus.TOO_MANY_REQUESTS);
    // Signed-in use of the apps isn't limited
    assertThat(mvc.get().uri(path("/kilns")).with(owner.token())).hasStatusOk();

    assertThat(logged("TOKEN")).filteredOn(row -> row.startsWith("LIMITED")).hasSize(2);
  }

  @Test
  void mcpCountsOnlyToolCallsAndAnswersARefusalAsAToolError() {
    String token = token("Claude");
    for (int call = 0; call < 2; call++) {
      assertThat(toolCall(token)).bodyJson().extractingPath("$.result.isError").isEqualTo(false);
    }
    // Free: these don't take from the bucket
    assertThat(rpc(token, "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"ping\"}")).hasStatusOk();
    assertThat(rpc(token, "{\"jsonrpc\": \"2.0\", \"id\": 2, \"method\": \"tools/list\"}"))
        .hasStatusOk();

    MvcTestResult refused = toolCall(token);
    assertThat(refused).hasStatusOk();
    assertThat(refused).bodyJson().extractingPath("$.result.isError").isEqualTo(true);
    assertThat(refused)
        .bodyJson()
        .extractingPath("$.result.content[0].text")
        .asString()
        .startsWith("Too many tool calls for now. Wait ");

    // MCP calls count only against MCP, so REST calls with the same token still go through
    assertThat(withToken(token, path("/kilns"))).hasStatusOk();
    assertThat(logged("MCP")).containsExactly("OK", "OK", "LIMITED");
  }

  private MvcTestResult toolCall(String token) {
    return rpc(
        token,
        """
        {"jsonrpc": "2.0", "id": 9, "method": "tools/call", "params": {"name": "kilns", "arguments": {}}}
        """);
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

  private List<String> logged(String channel) {
    return TenantContext.callAs(
        new TenantContext(ownerId, studioId, StudioRole.OWNER),
        () ->
            transactions.execute(tx ->
                jdbc.sql(
                        """
                        select outcome from api_call_log
                        where studio_id = :studioId and channel = :channel order by created_at, id
                        """)
                    .param("studioId", studioId)
                    .param("channel", channel)
                    .query(String.class)
                    .list()));
  }

  private String token(String name) {
    return read(
        post(path("/api-tokens"), "{\"name\": \"%s\", \"scopes\": [\"read\"]}".formatted(name)),
        "$.secret");
  }

  private MvcTestResult withToken(String secret, String uri) {
    return mvc.get().uri(uri).header(HttpHeaders.AUTHORIZATION, "Bearer " + secret).exchange();
  }

  private MvcTestResult post(String uri, String body) {
    return mvc.post()
        .uri(uri)
        .with(owner.token())
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
