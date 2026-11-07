package io.github.aaronfilson.studiomanager.piece;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.StatementCounts;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.util.ArrayList;
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
 * Pieces on Hibernate (ADR 21): lists at a fixed number of statements, the client's version
 * checked, a whole load moved at once, and the tenant applied by Hibernate as well as by row-level
 * security.
 */
@IntegrationTest
class PiecePersistenceTests {

  @Autowired MockMvcTester mvc;
  @Autowired StatementCounts statements;
  @Autowired PieceService pieces;
  @Autowired PieceRepository pieceRepository;
  @Autowired TransactionOperations transactions;

  private final TestUser owner = TestUser.named("Pia");
  private UUID studioId;
  private UUID ownerAccountId;

  @BeforeEach
  void studio() throws Exception {
    studioId = UUID.fromString(read(createStudio(owner, "Fixed Pieces"), "$.id"));
    ownerAccountId =
        UUID.fromString(
            read(mvc.get().uri("/api/v1/me").with(owner.token()).exchange(), "$.account.id"));
  }

  @Test
  void theListIsTheSameNumberOfQueriesForOnePieceOrMany() throws Exception {
    piece();
    long forOne = count(() -> pieces.list(null, null));

    piece();
    piece();
    StatementCounts.Counted<?> forThree =
        statements.of(() -> TenantContext.callAs(inStudio(), () -> pieces.list(null, null)));

    assertThat(forThree.statements()).isEqualTo(forOne);
  }

  @Test
  void aLoadAndAQueueAreTheSameNumberOfQueriesForOnePieceOrMany() throws Exception {
    List<UUID> one = List.of(piece());
    long loadOfOne = count(() -> pieces.listByIds(one));
    long queueOfOne = count(() -> pieces.waitingIn(PieceStage.RECEIVED));

    List<UUID> three = new ArrayList<>(one);
    three.add(piece());
    three.add(piece());

    assertThat(count(() -> pieces.listByIds(three))).isEqualTo(loadOfOne);
    assertThat(count(() -> pieces.waitingIn(PieceStage.RECEIVED))).isEqualTo(queueOfOne);
  }

  @Test
  void advancingALoadMovesEveryPieceOnceAndBumpsEachVersion() throws Exception {
    List<UUID> load = List.of(piece(), piece());

    List<PieceView> moved =
        TenantContext.callAs(
            inStudio(),
            () -> pieces.advanceAll(load, PieceStage.RECEIVED, PieceStage.BISQUE_FIRED, "Fired"));

    assertThat(moved).extracting(PieceView::stage).containsOnly(PieceStage.BISQUE_FIRED);
    assertThat(moved).extracting(PieceView::version).containsOnly(1L);
    PieceView first = TenantContext.callAs(inStudio(), () -> pieces.get(load.getFirst()));
    assertThat(first.timeline())
        .extracting(PieceView.Event::to)
        .containsExactly(PieceStage.RECEIVED, PieceStage.BISQUE_FIRED);
    // A piece no longer where the load expects it stops the whole move
    assertThat(
            post(
                path("/pieces/" + load.getFirst() + "/moves"),
                "{\"to\": \"GLAZED\", \"expectedVersion\": 1}"))
        .hasStatusOk();
    assertThatThrownBy(() ->
            TenantContext.callAs(
                inStudio(),
                () -> pieces.advanceAll(load, PieceStage.BISQUE_FIRED, PieceStage.GLAZED, "Again")))
        .isInstanceOf(ConflictException.class);
    assertThat(TenantContext.callAs(inStudio(), () -> pieces.get(load.getLast())).stage())
        .isEqualTo(PieceStage.BISQUE_FIRED);
  }

  @Test
  void aStaleVersionIsAConflictAndTheCurrentOneSaves() throws Exception {
    UUID piece = piece();
    String move =
        """
        {"to": "BISQUE_FIRED", "expectedVersion": %d}
        """;
    assertThat(post(path("/pieces/" + piece + "/moves"), move.formatted(0)))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.version")
        .isEqualTo(1);
    String again =
        """
        {"to": "GLAZED", "expectedVersion": 0}
        """;
    assertThat(post(path("/pieces/" + piece + "/moves"), again)).hasStatus(HttpStatus.CONFLICT);
  }

  @Test
  void hibernateSeesOnlyTheBoundStudiosPiecesAndNoneWithNoStudioBound() throws Exception {
    UUID ours = piece();
    TestUser stranger = TestUser.named("Sid");
    UUID theirStudio = UUID.fromString(read(createStudio(stranger, "Other Pieces"), "$.id"));
    UUID theirAccount =
        UUID.fromString(
            read(mvc.get().uri("/api/v1/me").with(stranger.token()).exchange(), "$.account.id"));
    TenantContext theirs = new TenantContext(theirAccount, theirStudio, StudioRole.OWNER);

    assertThat(TenantContext.callAs(theirs, () -> findPiece(ours))).isFalse();
    assertThat(
            TenantContext.callAs(TenantContext.forAccount(ownerAccountId), () -> findPiece(ours)))
        .isFalse();
    assertThat(TenantContext.callAs(inStudio(), () -> findPiece(ours))).isTrue();
  }

  private boolean findPiece(UUID pieceId) {
    return Boolean.TRUE.equals(
        transactions.execute(status -> pieceRepository.findById(pieceId).isPresent()));
  }

  private long count(ScopedValue.CallableOp<?, RuntimeException> work) throws Exception {
    return statements.of(() -> TenantContext.callAs(inStudio(), work)).statements();
  }

  private TenantContext inStudio() {
    return new TenantContext(ownerAccountId, studioId, StudioRole.OWNER);
  }

  private UUID piece() throws Exception {
    MvcTestResult created = post(path("/pieces"), "{\"description\": \"Bowl\", \"size\": {}}");
    assertThat(created).hasStatus(HttpStatus.CREATED);
    return UUID.fromString(read(created, "$.id"));
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

  private String path(String suffix) {
    return "/api/v1/studios/" + studioId + suffix;
  }

  private static String read(MvcTestResult result, String jsonPath) throws Exception {
    return JsonPath.read(result.getResponse().getContentAsString(), jsonPath).toString();
  }
}
