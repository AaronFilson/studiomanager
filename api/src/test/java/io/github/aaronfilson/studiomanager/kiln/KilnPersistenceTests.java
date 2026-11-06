package io.github.aaronfilson.studiomanager.kiln;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.StatementCounts;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Kiln on Hibernate (ADR 21): lists at a fixed number of statements, the client's version checked,
 * and the tenant applied by Hibernate as well as by row-level security.
 */
@IntegrationTest
class KilnPersistenceTests {

  @Autowired MockMvcTester mvc;
  @Autowired StatementCounts statements;
  @Autowired FiringService firings;
  @Autowired KilnRepository kilnRepository;
  @Autowired TransactionOperations transactions;

  private final TestUser owner = TestUser.named("Pat");
  private UUID studioId;
  private UUID ownerAccountId;

  @BeforeEach
  void studio() throws Exception {
    studioId = UUID.fromString(read(createStudio(owner, "Fixed Queries"), "$.id"));
    ownerAccountId =
        UUID.fromString(
            read(mvc.get().uri("/api/v1/me").with(owner.token()).exchange(), "$.account.id"));
  }

  @Test
  void aKilnsFiringLogIsTheSameNumberOfQueriesForOneFiringOrMany() throws Exception {
    UUID kiln = kiln("Log kiln");
    fireOnce(kiln);
    long forOne = countListing(kiln);

    fireOnce(kiln);
    fireOnce(kiln);
    long forThree = countListing(kiln);

    assertThat(TenantContext.callAs(inStudio(), () -> firings.listForKiln(kiln)).items())
        .hasSize(3);
    assertThat(forThree).isEqualTo(forOne);
  }

  @Test
  void loadsInProgressAreTheSameNumberOfQueriesForOneKilnOrMany() throws Exception {
    load(kiln("Open one"), piece());
    long forOne = statements.of(() -> TenantContext.callAs(inStudio(), firings::open)).statements();

    load(kiln("Open two"), piece());
    load(kiln("Open three"), piece());
    StatementCounts.Counted<?> forThree =
        statements.of(() -> TenantContext.callAs(inStudio(), firings::open));

    assertThat(forThree.statements()).isEqualTo(forOne);
  }

  @Test
  void aStaleVersionIsAConflictAndTheCurrentOneSaves() throws Exception {
    UUID kiln = kiln("Versioned");
    String rename =
        """
        {"kiln": {"name": "Renamed", "kind": "GAS"}, "expectedVersion": %d}
        """;
    assertThat(put("/kilns/" + kiln, rename.formatted(0)))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.version")
        .isEqualTo(1);
    assertThat(put("/kilns/" + kiln, rename.formatted(0))).hasStatus(HttpStatus.CONFLICT);
  }

  @Test
  void aTakenNameIsAnInputErrorNotAServerError() throws Exception {
    kiln("Twin");
    assertThat(post(path("/kilns"), "{\"name\": \"Twin\", \"kind\": \"GAS\"}"))
        .hasStatus(HttpStatus.BAD_REQUEST);
  }

  @Test
  void hibernateSeesOnlyTheBoundStudiosKilnsAndNoneWithNoStudioBound() throws Exception {
    UUID ours = kiln("Ours");
    TestUser stranger = TestUser.named("Sol");
    UUID theirStudio = UUID.fromString(read(createStudio(stranger, "Elsewhere"), "$.id"));
    UUID theirAccount =
        UUID.fromString(
            read(mvc.get().uri("/api/v1/me").with(stranger.token()).exchange(), "$.account.id"));
    TenantContext theirs = new TenantContext(theirAccount, theirStudio, StudioRole.OWNER);

    assertThat(TenantContext.callAs(theirs, this::readKilnIds)).doesNotContain(ours);
    assertThat(TenantContext.callAs(theirs, () -> findKiln(ours))).isFalse();
    // "My studios" and the sweeps run with no studio bound: entities with @TenantId match nothing
    assertThat(TenantContext.callAs(TenantContext.forAccount(ownerAccountId), this::readKilnIds))
        .isEmpty();
    // ...while the account and studio entities, which have no @TenantId, still work there
    assertThat(mvc.get().uri("/api/v1/me").with(owner.token()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.memberships[*].studio.id")
        .asArray()
        .contains(studioId.toString());
  }

  private List<UUID> readKilnIds() {
    return transactions.execute(
        status -> kilnRepository.findAllInOrder().stream().map(Kiln::id).toList());
  }

  private boolean findKiln(UUID kilnId) {
    return Boolean.TRUE.equals(
        transactions.execute(status -> kilnRepository.findById(kilnId).isPresent()));
  }

  private long countListing(UUID kiln) throws Exception {
    return statements
        .of(() -> TenantContext.callAs(inStudio(), () -> firings.listForKiln(kiln)))
        .statements();
  }

  private TenantContext inStudio() {
    return new TenantContext(ownerAccountId, studioId, StudioRole.OWNER);
  }

  private void fireOnce(UUID kiln) throws Exception {
    String firing = read(load(kiln, piece()), "$.id");
    transition(firing, "{\"to\": \"FIRING\", \"expectedVersion\": 0}");
    transition(firing, "{\"to\": \"COMPLETE\", \"expectedVersion\": 1}");
  }

  private void transition(String firing, String json) {
    assertThat(post(path("/firings/" + firing + "/transitions"), json)).hasStatusOk();
  }

  private MvcTestResult load(UUID kiln, String piece) {
    MvcTestResult result =
        post(
            path("/kilns/" + kiln + "/firings"),
            "{\"kind\": \"BISQUE\", \"pieceIds\": [\"%s\"]}".formatted(piece));
    assertThat(result).hasStatus(HttpStatus.CREATED);
    return result;
  }

  private UUID kiln(String name) throws Exception {
    return UUID.fromString(
        read(
            post(path("/kilns"), "{\"name\": \"%s\", \"kind\": \"ELECTRIC\"}".formatted(name)),
            "$.id"));
  }

  private String piece() throws Exception {
    return read(post(path("/pieces"), "{\"size\": {}}"), "$.id");
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
