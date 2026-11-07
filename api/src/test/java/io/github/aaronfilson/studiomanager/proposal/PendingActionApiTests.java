package io.github.aaronfilson.studiomanager.proposal;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.StatementCounts;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.Capped;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * An assistant proposes over MCP with a personal API token; a person approves or rejects it in the
 * app, and approving makes the change through the normal service, as them.
 */
@IntegrationTest
class PendingActionApiTests {

  @Autowired MockMvcTester mvc;
  @Autowired StatementCounts statements;
  @Autowired PendingActionReview review;

  private final TestUser owner = TestUser.named("Olive");
  private UUID studioId;
  private String proposingToken;

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
    proposingToken =
        read(
            post(
                owner,
                path("/api-tokens"),
                "{\"name\": \"Claude desktop\", \"scopes\": [\"read\", \"propose\"]}"),
            "$.secret");
  }

  @Test
  void anApprovedPurchaseRequestIsMadeAsTheApprover() {
    String itemId = clay("8");

    MvcTestResult proposed =
        call(
            proposingToken,
            "propose_purchase_request",
            """
            {"itemId": "%s", "reason": "B-Mix is under par and nothing is on order."}\
            """
                .formatted(itemId));
    assertThat(proposed).bodyJson().extractingPath("$.result.isError").isEqualTo(false);
    assertThat(proposed)
        .bodyJson()
        .extractingPath("$.result.structuredContent.result.status")
        .isEqualTo("PENDING");
    // Nothing is requested until a person approves
    assertThat(JsonPath.<List<Object>>read(body(get(path("/purchase-requests"))), "$[*]"))
        .isEmpty();

    MvcTestResult queue = get(path("/pending-actions"));
    assertThat(queue).hasStatusOk();
    assertThat(queue).bodyJson().extractingPath("$.items[0].kind").isEqualTo("PURCHASE_REQUEST");
    assertThat(queue).bodyJson().extractingPath("$.items[0].subjectName").isEqualTo("B-Mix");
    // The item's usual reorder quantity, fixed when it was proposed
    assertThat(queue)
        .bodyJson()
        .extractingPath("$.items[0].purchaseRequest.quantity")
        .isEqualTo("8");
    assertThat(queue).bodyJson().extractingPath("$.items[0].proposedByName").isEqualTo("Olive");
    assertThat(queue)
        .bodyJson()
        .extractingPath("$.items[0].proposedViaTokenName")
        .isEqualTo("Claude desktop");
    String actionId = read(queue, "$.items[0].id");

    MvcTestResult approved = decide(actionId, "APPROVED", 0, null);
    assertThat(approved).hasStatusOk();
    assertThat(approved).bodyJson().extractingPath("$.status").isEqualTo("APPROVED");
    assertThat(approved).bodyJson().extractingPath("$.decidedByName").isEqualTo("Olive");
    String requestId = read(approved, "$.resultId");

    MvcTestResult requests = get(path("/purchase-requests"));
    assertThat(requests).bodyJson().extractingPath("$[0].id").isEqualTo(requestId);
    assertThat(requests).bodyJson().extractingPath("$[0].status").isEqualTo("REQUESTED");
    assertThat(requests).bodyJson().extractingPath("$[0].quantity").isEqualTo("8");
    assertThat(JsonPath.<List<Object>>read(body(get(path("/pending-actions"))), "$.items"))
        .isEmpty();

    // Decided once: approving again is refused and makes nothing
    assertThat(decide(actionId, "APPROVED", 1, null)).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(decide(actionId, "APPROVED", 0, null)).hasStatus(HttpStatus.CONFLICT);
    assertThat(JsonPath.<List<Object>>read(body(get(path("/purchase-requests"))), "$[*]"))
        .hasSize(1);
  }

  @Test
  void aRejectedTaskChangesNothingAndAnApprovedOneIsAdded() {
    String assetId =
        read(
            post(
                owner,
                path("/maintenance/assets"),
                "{\"kind\": \"PUG_MILL\", \"name\": \"Peter Pugger\"}"),
            "$.id");

    // Only kilns count firings: an obviously wrong proposal goes back to the assistant
    MvcTestResult wrong =
        call(
            proposingToken,
            "propose_maintenance_task",
            """
            {"assetId": "%s", "title": "Clean the auger", "everyFirings": 20}\
            """
                .formatted(assetId));
    assertThat(wrong).bodyJson().extractingPath("$.result.isError").isEqualTo(true);
    assertThat(wrong)
        .bodyJson()
        .extractingPath("$.result.content[0].text")
        .isEqualTo("Only kilns count firings. Use days for this one.");

    assertThat(
            call(
                proposingToken,
                "propose_maintenance_task",
                """
                {"assetId": "%s", "title": "Clean the auger", "everyDays": 14}\
                """
                    .formatted(assetId)))
        .bodyJson()
        .extractingPath("$.result.isError")
        .isEqualTo(false);
    MvcTestResult queue = get(path("/pending-actions"));
    assertThat(queue)
        .bodyJson()
        .extractingPath("$.items[0].maintenanceTask.everyDays")
        .isEqualTo(14);
    String actionId = read(queue, "$.items[0].id");

    MvcTestResult rejected = decide(actionId, "REJECTED", 0, "We clean it after every use.");
    assertThat(rejected).hasStatusOk();
    assertThat(rejected).bodyJson().extractingPath("$.status").isEqualTo("REJECTED");
    assertThat(rejected)
        .bodyJson()
        .extractingPath("$.decisionNote")
        .isEqualTo("We clean it after every use.");
    assertThat(JsonPath.<List<Object>>read(body(get(path("/maintenance/assets"))), "$..tasks[*]"))
        .isEmpty();

    // Proposed again and approved, the task is added
    call(
        proposingToken,
        "propose_maintenance_task",
        """
        {"assetId": "%s", "title": "Clean the auger", "everyDays": 30}\
        """
            .formatted(assetId));
    String secondId = read(get(path("/pending-actions")), "$.items[0].id");
    MvcTestResult approved = decide(secondId, "APPROVED", 0, null);
    assertThat(approved).hasStatusOk();
    List<String> titles =
        JsonPath.read(body(get(path("/maintenance/assets"))), "$..tasks[*].title");
    assertThat(titles).containsExactly("Clean the auger");
  }

  @Test
  void proposingNeedsTheProposeScopeAndDecidingNeedsAPerson() {
    String readOnly =
        read(
            post(owner, path("/api-tokens"), "{\"name\": \"Reader\", \"scopes\": [\"read\"]}"),
            "$.secret");
    MvcTestResult refused =
        call(readOnly, "propose_purchase_request", "{\"itemId\": \"%s\"}".formatted(clay("8")));
    assertThat(refused).bodyJson().extractingPath("$.result.isError").isEqualTo(true);
    assertThat(JsonPath.<List<Object>>read(body(get(path("/pending-actions"))), "$.items"))
        .isEmpty();

    // A token can't approve its own suggestions, or even see the queue
    assertThat(
            mvc.get()
                .uri(path("/pending-actions"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + proposingToken)
                .exchange())
        .hasStatus(HttpStatus.FORBIDDEN);
  }

  @Test
  void anotherStudioCantSeeOrDecideTheSuggestions() {
    call(proposingToken, "propose_purchase_request", "{\"itemId\": \"%s\"}".formatted(clay("8")));
    String actionId = read(get(path("/pending-actions")), "$.items[0].id");

    TestUser stranger = TestUser.named("Sam");
    String otherStudio =
        read(
            post(
                stranger,
                "/api/v1/studios",
                """
                {"name": "Mud Hut", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
                """),
            "$.id");
    assertThat(
            mvc.put()
                .uri(
                    "/api/v1/studios/" + otherStudio + "/pending-actions/" + actionId + "/decision")
                .with(stranger.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\": \"APPROVED\", \"expectedVersion\": 0}"))
        .hasStatus(HttpStatus.NOT_FOUND);
  }

  @Test
  void theQueueTakesTheSameStatementsForOneSuggestionOrSeveral() throws Exception {
    UUID ownerId =
        UUID.fromString(
            read(mvc.get().uri("/api/v1/me").with(owner.token()).exchange(), "$.account.id"));
    TenantContext inStudio = new TenantContext(ownerId, studioId, StudioRole.OWNER);
    String itemId = clay("8");
    String propose = "{\"itemId\": \"%s\"}".formatted(itemId);

    call(proposingToken, "propose_purchase_request", propose);
    long forOne = statements.of(() -> TenantContext.callAs(inStudio, review::waiting)).statements();
    call(proposingToken, "propose_purchase_request", propose);
    call(proposingToken, "propose_purchase_request", propose);
    StatementCounts.Counted<Capped<PendingActionView>> forThree =
        statements.of(() -> TenantContext.callAs(inStudio, review::waiting));

    assertThat(forThree.result().items()).hasSize(3);
    assertThat(forThree.statements()).isEqualTo(forOne);
  }

  private String clay(String reorderQuantity) {
    return read(
        post(
            owner,
            path("/supplies"),
            """
            {"name": "B-Mix", "category": "CLAY", "unit": "25 lb bag", "parLevel": "4", "reorderQuantity": "%s"}
            """
                .formatted(reorderQuantity)),
        "$.id");
  }

  private MvcTestResult decide(
      String actionId, String decision, long expectedVersion, @Nullable String note) {
    String noteJson = note == null ? "" : ", \"note\": \"%s\"".formatted(note);
    return mvc.put()
        .uri(path("/pending-actions/" + actionId + "/decision"))
        .with(owner.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(
            "{\"decision\": \"%s\", \"expectedVersion\": %d%s}"
                .formatted(decision, expectedVersion, noteJson))
        .exchange();
  }

  private MvcTestResult call(String bearer, String tool, String arguments) {
    return mvc.post()
        .uri(path("/mcp"))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
        .content(
            """
            {"jsonrpc": "2.0", "id": 9, "method": "tools/call", "params": {"name": "%s", "arguments": %s}}
            """
                .formatted(tool, arguments))
        .exchange();
  }

  private MvcTestResult get(String uri) {
    return mvc.get().uri(uri).with(owner.token()).exchange();
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
