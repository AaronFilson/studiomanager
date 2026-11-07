package io.github.aaronfilson.studiomanager.supplies;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.StatementCounts;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Supplies on Hibernate (ADR 21): lists at a fixed number of statements, stock summed in the same
 * query as the items, the client's version checked, and the tenant applied by Hibernate.
 */
@IntegrationTest
class SupplyPersistenceTests {

  @Autowired MockMvcTester mvc;
  @Autowired StatementCounts statements;
  @Autowired SupplyService supplies;

  private final TestUser owner = TestUser.named("Pip");
  private UUID studioId;
  private UUID ownerAccountId;

  @BeforeEach
  void studio() throws Exception {
    studioId = UUID.fromString(read(createStudio(owner, "Stock Room"), "$.id"));
    ownerAccountId =
        UUID.fromString(
            read(mvc.get().uri("/api/v1/me").with(owner.token()).exchange(), "$.account.id"));
  }

  @Test
  void theItemListIsTheSameNumberOfQueriesForOneItemOrMany() throws Exception {
    stockedAndRequested("B-Mix 5");
    long forOne = count(supplies::items);

    stockedAndRequested("Speckled buff");
    stockedAndRequested("Porcelain");
    long forThree = count(supplies::items);

    assertThat(TenantContext.callAs(inStudio(), supplies::items)).hasSize(3);
    assertThat(forThree).isEqualTo(forOne);
  }

  @Test
  void openRequestsAreTheSameNumberOfQueriesForOneOrMany() throws Exception {
    stockedAndRequested("B-Mix 5");
    long forOne = count(supplies::openRequests);

    stockedAndRequested("Speckled buff");
    stockedAndRequested("Porcelain");
    long forThree = count(supplies::openRequests);

    assertThat(TenantContext.callAs(inStudio(), supplies::openRequests)).hasSize(3);
    assertThat(forThree).isEqualTo(forOne);
  }

  @Test
  void anItemsHistoryIsTheSameNumberOfQueriesForOneMovementOrMany() throws Exception {
    UUID item = UUID.fromString(stockedAndRequested("B-Mix 5"));
    long forOne = count(() -> supplies.movements(item));

    move(item.toString(), "USED", "1");
    move(item.toString(), "USED", "1");
    long forThree = count(() -> supplies.movements(item));

    assertThat(TenantContext.callAs(inStudio(), () -> supplies.movements(item)).items())
        .hasSize(3);
    assertThat(forThree).isEqualTo(forOne);
  }

  @Test
  void aStaleVersionIsAConflictAndSavingNoChangeKeepsTheVersion() throws Exception {
    String item = create("Versioned");
    String rename =
        """
        {"item": {"name": "%s", "category": "CLAY", "unit": "bag"}, "expectedVersion": %d}
        """;
    assertThat(put("/supplies/" + item, rename.formatted("Renamed", 0)))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.version")
        .isEqualTo(1);
    assertThat(put("/supplies/" + item, rename.formatted("Renamed", 1)))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.version")
        .isEqualTo(1);
    assertThat(put("/supplies/" + item, rename.formatted("Again", 0)))
        .hasStatus(HttpStatus.CONFLICT);
  }

  @Test
  void aTakenNameIsAnInputErrorOnCreateAndOnRename() throws Exception {
    create("Twin");
    String other = create("Other");
    assertThat(post(path("/supplies"), "{\"name\": \"Twin\", \"category\": \"CLAY\", \"unit\": \"bag\"}"))
        .hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(
            put(
                "/supplies/" + other,
                """
                {"item": {"name": "Twin", "category": "CLAY", "unit": "bag"}, "expectedVersion": 0}
                """))
        .hasStatus(HttpStatus.BAD_REQUEST);
  }

  @Test
  void hibernateSeesOnlyTheBoundStudiosItems() throws Exception {
    String ours = create("Ours");
    TestUser stranger = TestUser.named("Sol");
    UUID theirStudio = UUID.fromString(read(createStudio(stranger, "Elsewhere"), "$.id"));
    UUID theirAccount =
        UUID.fromString(
            read(mvc.get().uri("/api/v1/me").with(stranger.token()).exchange(), "$.account.id"));
    TenantContext theirs = new TenantContext(theirAccount, theirStudio, StudioRole.OWNER);

    assertThat(TenantContext.callAs(theirs, supplies::items)).isEmpty();
    assertThat(
            mvc.get()
                .uri("/api/v1/studios/" + theirStudio + "/supplies/" + ours + "/movements")
                .with(stranger.token()))
        .hasStatus(HttpStatus.NOT_FOUND);
  }

  /** An item with stock, a par level, and an open purchase request. Returns its id. */
  private String stockedAndRequested(String name) throws Exception {
    String item =
        read(
            post(
                path("/supplies"),
                """
                {"name": "%s", "category": "CLAY", "unit": "bag", "parLevel": "5", "reorderQuantity": "10"}
                """
                    .formatted(name)),
            "$.id");
    move(item, "RECEIVED", "6");
    assertThat(post(path("/purchase-requests"), "{\"itemId\": \"%s\"}".formatted(item)))
        .hasStatus(HttpStatus.CREATED);
    return item;
  }

  private String create(String name) throws Exception {
    MvcTestResult created =
        post(
            path("/supplies"),
            "{\"name\": \"%s\", \"category\": \"CLAY\", \"unit\": \"bag\"}".formatted(name));
    assertThat(created).hasStatus(HttpStatus.CREATED);
    return read(created, "$.id");
  }

  private void move(String item, String kind, String quantity) {
    assertThat(
            post(
                path("/supplies/" + item + "/movements"),
                "{\"kind\": \"%s\", \"quantity\": \"%s\"}".formatted(kind, quantity)))
        .hasStatusOk();
  }

  private long count(Callable<?> listing) throws Exception {
    return statements.of(() -> TenantContext.callAs(inStudio(), listing::call)).statements();
  }

  private TenantContext inStudio() {
    return new TenantContext(ownerAccountId, studioId, StudioRole.OWNER);
  }

  private MvcTestResult createStudio(TestUser user, String name) {
    return mvc.post()
        .uri("/api/v1/studios")
        .with(user.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(
            """
            {"name": "%s", "unitSystem": "METRIC", "timeZone": "Europe/London", "country": "GB"}
            """
                .formatted(name))
        .exchange();
  }

  private MvcTestResult post(String uri, String json) {
    return mvc.post()
        .uri(uri)
        .with(owner.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .exchange();
  }

  private MvcTestResult put(String suffix, String json) {
    return mvc.put()
        .uri(path(suffix))
        .with(owner.token())
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
