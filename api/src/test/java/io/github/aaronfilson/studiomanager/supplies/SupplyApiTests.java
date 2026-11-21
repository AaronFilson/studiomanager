package io.github.aaronfilson.studiomanager.supplies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * M5 through the API: par levels, purchase requests with approval, counts, and clay sold to
 * members.
 */
@IntegrationTest
class SupplyApiTests {

  @Autowired MockMvcTester mvc;
  @Autowired SupplyService supplies;

  private final TestUser owner = TestUser.named("Olive");
  private final TestUser tech = TestUser.named("Tam");
  private final TestUser maker = TestUser.named("Mo");
  private UUID studioId;
  private String makerId;

  @BeforeEach
  void studio() throws Exception {
    studioId =
        UUID.fromString(
            read(
                post(
                    owner,
                    "/api/v1/studios",
                    """
                    {"name": "Supply Clay", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
                    """),
                "$.id"));
    join(tech, "STAFF");
    join(maker, "MEMBER");
    makerId = read(mvc.get().uri("/api/v1/me").with(maker.token()).exchange(), "$.account.id");
  }

  @Test
  void lowStockIsFlaggedUntilAnApprovedOrderArrives() throws Exception {
    String clay =
        read(
            post(
                tech,
                path("/supplies"),
                """
                {"name": "B-Mix 5", "category": "CLAY", "unit": "25 lb bag", "supplier": "Laguna",
                 "leadTimeDays": 7, "parLevel": "5", "reorderQuantity": "10"}
                """),
            "$.id");
    assertThat(move(tech, clay, "RECEIVED", "6"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.low")
        .isEqualTo(false);
    assertThat(move(tech, clay, "USED", "2"))
        .hasStatusOk()
        .bodyJson()
        .hasPathSatisfying("$.onHand", n -> assertThat(n).isEqualTo("4"))
        .hasPathSatisfying("$.needsOrdering", n -> assertThat(n).isEqualTo(true));
    assertThat(mvc.get().uri(path("/today")).with(owner.token()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.lowStock[0].name")
        .isEqualTo("B-Mix 5");

    MvcTestResult request =
        post(tech, path("/purchase-requests"), "{\"itemId\": \"%s\"}".formatted(clay));
    assertThat(request)
        .hasStatus(HttpStatus.CREATED)
        .bodyJson()
        .extractingPath("$.quantity")
        .isEqualTo("10");
    String id = read(request, "$.id");

    // Only the owner approves; then staff carry it through to received
    assertThat(decide(tech, id, "APPROVED", 0)).hasStatus(HttpStatus.FORBIDDEN);
    assertThat(decide(owner, id, "APPROVED", 0)).hasStatusOk();
    assertThat(mvc.get().uri(path("/supplies")).with(tech.token()))
        .hasStatusOk()
        .bodyJson()
        .hasPathSatisfying("$[0].low", l -> assertThat(l).isEqualTo(true))
        .hasPathSatisfying("$[0].needsOrdering", n -> assertThat(n).isEqualTo(false));
    assertThat(decide(tech, id, "ORDERED", 1)).hasStatusOk();
    assertThat(decide(tech, id, "RECEIVED", 2))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo("RECEIVED");

    assertThat(mvc.get().uri(path("/supplies")).with(maker.token()))
        .hasStatusOk()
        .bodyJson()
        .hasPathSatisfying("$[0].onHand", n -> assertThat(n).isEqualTo("14"))
        .hasPathSatisfying("$[0].low", l -> assertThat(l).isEqualTo(false));
    assertThat(
            post(
                maker,
                path("/supplies"),
                "{\"name\": \"x\", \"category\": \"CLAY\", \"unit\": \"bag\"}"))
        .hasStatus(HttpStatus.FORBIDDEN);
  }

  @Test
  void claySoldToAMemberGoesOnTheirBalanceWithTaxAndACountCorrectsTheBooks() throws Exception {
    assertThat(
            mvc.put()
                .uri(path("/tax-rates"))
                .with(owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"taxCategory\": \"RETAIL\", \"rate\": \"0.05\", \"effectiveFrom\": \"2026-01-01\"}"))
        .hasStatusOk();
    String clay =
        read(
            post(
                owner,
                path("/supplies"),
                """
                {"name": "Speckled buff", "category": "CLAY", "unit": "25 lb bag", "salePrice": "30.00",
                 "taxCategory": "RETAIL", "substitute": "Use B-Mix 5 while we wait"}
                """),
            "$.id");
    move(owner, clay, "RECEIVED", "10");

    assertThat(
            post(
                owner,
                path("/supplies/" + clay + "/sales"),
                """
                {"memberId": "%s", "quantity": "2"}
                """
                    .formatted(makerId)))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.onHand")
        .isEqualTo("8");
    assertThat(mvc.get().uri(path("/members/" + makerId + "/statement")).with(maker.token()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.owed")
        .isEqualTo("63.00");

    assertThat(move(owner, clay, "COUNTED", "7"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.onHand")
        .isEqualTo("7");
    assertThat(mvc.get().uri(path("/supplies/" + clay + "/movements")).with(owner.token()))
        .hasStatusOk()
        .bodyJson()
        .hasPathSatisfying("$.items[0].kind", k -> assertThat(k).isEqualTo("COUNTED"))
        .hasPathSatisfying("$.items[0].quantity", q -> assertThat(q).isEqualTo("-1"))
        .hasPathSatisfying("$.items[1].memberName", m -> assertThat(m).isEqualTo("Mo"));
    assertThat(move(owner, clay, "SOLD", "1")).hasStatus(HttpStatus.BAD_REQUEST);
  }

  @Test
  void runningOutTellsMembersWhatToUseInsteadOncePerStockOut() throws Exception {
    String clay =
        read(
            post(
                owner,
                path("/supplies"),
                """
                {"name": "Speckled buff", "category": "CLAY", "unit": "25 lb bag",
                 "substitute": "Use B-Mix 5 while we wait"}
                """),
            "$.id");
    move(owner, clay, "RECEIVED", "2");
    move(owner, clay, "USED", "1");
    assertThat(outNotices()).isEmpty();

    move(owner, clay, "USED", "1");
    assertThat(outNotices()).containsExactly("Speckled buff is out");
    // A count that finds the shelf still empty isn't news
    move(owner, clay, "COUNTED", "0");
    assertThat(outNotices()).hasSize(1);

    // Back in, and out again: a new stock-out, so a new notice
    move(owner, clay, "RECEIVED", "1");
    move(owner, clay, "COUNTED", "0");
    assertThat(outNotices()).hasSize(2);
  }

  @Test
  void recordingPurchasePriceUpdatesTheSupplyItemAndRejectsNegativePrices() throws Exception {
    UUID clayId =
        UUID.fromString(
            read(
                post(
                    owner,
                    path("/supplies"),
                    "{\"name\": \"B-Mix 5\", \"category\": \"CLAY\", \"unit\": \"50 lb box\"}"),
                "$.id"));
    UUID ownerId =
        UUID.fromString(
            read(mvc.get().uri("/api/v1/me").with(owner.token()).exchange(), "$.account.id"));
    TenantContext asOwner = new TenantContext(ownerId, studioId, StudioRole.OWNER);
    Instant boughtAt = Instant.parse("2026-10-14T15:30:00Z");

    TenantContext.callAs(
        asOwner,
        () -> {
          supplies.recordPurchasePrice(clayId, new BigDecimal("32.505"), boughtAt);
          return null;
        });

    assertThat(mvc.get().uri(path("/supplies")).with(owner.token()))
        .hasStatusOk()
        .bodyJson()
        .hasPathSatisfying("$[0].lastPurchaseUnitPrice", p -> assertThat(p).isEqualTo("32.505"))
        .hasPathSatisfying(
            "$[0].lastPurchasedAt", d -> assertThat(d).isEqualTo("2026-10-14T15:30:00Z"));

    // Strictly older purchase timestamp does not overwrite newer price
    TenantContext.callAs(
        asOwner,
        () -> {
          supplies.recordPurchasePrice(
              clayId, new BigDecimal("19.00"), Instant.parse("2026-10-01T00:00:00Z"));
          return null;
        });
    assertThat(mvc.get().uri(path("/supplies")).with(owner.token()))
        .hasStatusOk()
        .bodyJson()
        .hasPathSatisfying("$[0].lastPurchaseUnitPrice", p -> assertThat(p).isEqualTo("32.505"))
        .hasPathSatisfying(
            "$[0].lastPurchasedAt", d -> assertThat(d).isEqualTo("2026-10-14T15:30:00Z"));

    // Preserves up to 4 decimals (HALF_UP) and formats whole/tenths with at least 2 decimals
    TenantContext.callAs(
        asOwner,
        () -> {
          supplies.recordPurchasePrice(
              clayId, new BigDecimal("0.08756"), Instant.parse("2026-10-15T00:00:00Z"));
          return null;
        });
    assertThat(mvc.get().uri(path("/supplies")).with(owner.token()))
        .hasStatusOk()
        .bodyJson()
        .hasPathSatisfying("$[0].lastPurchaseUnitPrice", p -> assertThat(p).isEqualTo("0.0876"));

    TenantContext.callAs(
        asOwner,
        () -> {
          supplies.recordPurchasePrice(
              clayId, new BigDecimal("18.5"), Instant.parse("2026-10-16T00:00:00Z"));
          return null;
        });
    assertThat(mvc.get().uri(path("/supplies")).with(owner.token()))
        .hasStatusOk()
        .bodyJson()
        .hasPathSatisfying("$[0].lastPurchaseUnitPrice", p -> assertThat(p).isEqualTo("18.50"));

    assertThatThrownBy(() ->
            TenantContext.callAs(
                asOwner,
                () -> {
                  supplies.recordPurchasePrice(clayId, new BigDecimal("-1.00"), boughtAt);
                  return null;
                }))
        .isInstanceOf(InvalidInputException.class);
  }

  /** The maker's notices about something being out, newest first. */
  private List<String> outNotices() throws Exception {
    String body =
        mvc.get()
            .uri(path("/notifications"))
            .with(maker.token())
            .exchange()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.items[?(@.title =~ /.* is out/)].title");
  }

  private MvcTestResult move(TestUser user, String item, String kind, String quantity) {
    return post(
        user,
        path("/supplies/" + item + "/movements"),
        """
        {"kind": "%s", "quantity": "%s"}
        """
            .formatted(kind, quantity));
  }

  private MvcTestResult decide(TestUser user, String request, String to, long version) {
    return mvc.put()
        .uri(path("/purchase-requests/" + request + "/status"))
        .with(user.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"to\": \"%s\", \"expectedVersion\": %d}".formatted(to, version))
        .exchange();
  }

  private void join(TestUser user, String role) throws Exception {
    String code =
        read(post(owner, path("/invites"), "{\"role\": \"%s\"}".formatted(role)), "$.code");
    assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(user.token()))
        .hasStatusOk();
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

  private static String read(MvcTestResult result, String jsonPath) throws Exception {
    return JsonPath.read(result.getResponse().getContentAsString(), jsonPath).toString();
  }
}
