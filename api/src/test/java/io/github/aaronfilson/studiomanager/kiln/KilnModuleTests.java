package io.github.aaronfilson.studiomanager.kiln;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aaronfilson.studiomanager.TestEmailConfiguration;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.TestcontainersConfiguration;
import io.github.aaronfilson.studiomanager.firing.PieceSize;
import io.github.aaronfilson.studiomanager.piece.PieceIntake;
import io.github.aaronfilson.studiomanager.piece.PieceService;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountResolver;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.StudioService;
import io.github.aaronfilson.studiomanager.studio.UnitSystem;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;
import org.springframework.modulith.test.Scenario;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;

/**
 * The kiln module's events, with only kiln and what it depends on running. The modules that listen
 * (notify, schedule, maintenance, webhooks, the stream) aren't started, so these check what kiln
 * publishes, not what others do with it.
 *
 * <p>Scenario runs each stimulus in a transaction of its own, and row-level security and
 * Hibernate's tenant are read when a transaction begins, so each scenario runs inside {@link
 * #inStudio}.
 */
// account and apitoken implement platform interfaces that the security filters need
@ApplicationModuleTest(
    mode = BootstrapMode.ALL_DEPENDENCIES,
    extraIncludes = {"account", "apitoken"})
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestEmailConfiguration.class})
@EnabledIf(
    value = "io.github.aaronfilson.studiomanager.DockerAvailability#dockerAvailableOrCi",
    disabledReason = "Integration tests need Docker")
class KilnModuleTests {

  @Autowired AccountResolver accounts;
  @Autowired StudioService studios;
  @Autowired PieceService pieces;
  @Autowired KilnService kilns;
  @Autowired FiringLoads loads;
  @Autowired FiringService firings;

  @Autowired FiringTransitions transitions;

  private TenantContext owner;
  private UUID kilnId;

  @BeforeEach
  void studioWithAKiln() {
    TestUser person = TestUser.named("Kim");
    UUID accountId =
        accounts.resolve(
            Jwt.withTokenValue("module-test")
                .header("alg", "none")
                .claim("iss", TestUser.ISSUER)
                .subject(person.subject())
                .claim("name", person.name())
                .claim("email", person.email())
                .build());
    TenantContext signedIn = TenantContext.forAccount(accountId);
    UUID studioId =
        TenantContext.callAs(
                signedIn,
                () ->
                    studios.create("Module Kilns", UnitSystem.METRIC, "Europe/London", "GB", null))
            .id();
    owner = signedIn.inStudio(studioId, StudioRole.OWNER);
    kilnId =
        inStudio(() ->
            kilns
                .create(
                    new KilnService.KilnSettings("Skutt", null, KilnView.KilnType.ELECTRIC, null))
                .id());
  }

  @Test
  void startingALoadPublishesFiringStarted(Scenario scenario) {
    UUID firingId = load(intake("Vase")).id();

    inStudio(() -> {
      scenario
          .stimulate(() -> transitions.start(firingId, 0))
          .andWaitForEventOfType(FiringStarted.class)
          .matching(started -> started.firingId().equals(firingId))
          .toArriveAndVerify(started -> {
            assertThat(started.kilnId()).isEqualTo(kilnId);
            assertThat(started.kilnName()).isEqualTo("Skutt");
            assertThat(started.kind()).isEqualTo(FiringKind.BISQUE);
          });
      return null;
    });
  }

  @Test
  void unloadingPublishesOneFiringCompletedWithEveryPieceAndCountsTheFiring(Scenario scenario) {
    UUID jug = intake("Jug");
    UUID cup = intake("Cup");
    UUID firingId = load(jug, cup).id();
    inStudio(() -> transitions.start(firingId, 0));

    inStudio(() -> {
      scenario
          .stimulate(() -> transitions.complete(firingId, "Even", 1))
          .andWaitForEventOfType(FiringCompleted.class)
          .matching(completed -> completed.firingId().equals(firingId))
          .toArriveAndVerify(completed -> {
            assertThat(completed.kind()).isEqualTo(FiringKind.BISQUE);
            assertThat(completed.pieces())
                .extracting(FiringCompleted.FiredPiece::pieceId)
                .containsExactlyInAnyOrder(jug, cup);
          });
      return null;
    });
    assertThat(inStudio(() -> kilns.get(kilnId)).totalFirings()).isEqualTo(1);
    assertThat(inStudio(() -> firings.get(firingId)).pieceCount()).isEqualTo(2);
  }

  @Test
  void cancellingPublishesFiringEndedNotCompleted(Scenario scenario) {
    UUID firingId = load(intake("Bowl")).id();

    inStudio(() -> {
      scenario
          .stimulate(() -> transitions.cancel(firingId, "Element failed", 0))
          .andWaitForEventOfType(FiringEnded.class)
          .matching(ended -> ended.firingId().equals(firingId))
          .toArriveAndVerify(ended -> assertThat(ended.completed()).isFalse());
      return null;
    });
    assertThat(inStudio(() -> loads.queue(kilnId, FiringKind.BISQUE)).items())
        .extracting(entry -> entry.piece().piece().description())
        .containsExactly("Bowl");
  }

  private FiringView load(UUID... pieceIds) {
    return inStudio(() -> loads.load(kilnId, FiringKind.BISQUE, "04", List.of(pieceIds)));
  }

  private UUID intake(String description) {
    return inStudio(() ->
        pieces.intake(new PieceIntake(null, description, null, null, PieceSize.unmeasured())).id());
  }

  private <T> T inStudio(ScopedValue.CallableOp<T, RuntimeException> work) {
    return TenantContext.callAs(owner, work);
  }
}
