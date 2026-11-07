package io.github.aaronfilson.studiomanager.notify;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aaronfilson.studiomanager.TestEmailConfiguration;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.TestcontainersConfiguration;
import io.github.aaronfilson.studiomanager.kiln.FiringCompleted;
import io.github.aaronfilson.studiomanager.kiln.FiringKind;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountResolver;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.StudioService;
import io.github.aaronfilson.studiomanager.studio.UnitSystem;
import java.time.Instant;
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
 * The notify module's event flow: a finished firing becomes one notice per maker, listing every
 * piece of theirs, and each notice is put in the outbox as {@link NotificationCreated}.
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
class NotifyModuleTests {

  @Autowired AccountResolver accounts;
  @Autowired StudioService studios;
  @Autowired NotificationService notifications;

  private TenantContext owner;

  @BeforeEach
  void studio() {
    TestUser person = TestUser.named("Nell");
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
                    studios.create(
                        "Module Notices", UnitSystem.METRIC, "Europe/London", "GB", null))
            .id();
    owner = signedIn.inStudio(studioId, StudioRole.OWNER);
  }

  @Test
  void aFinishedFiringIsOneNoticePerMakerListingEachOfTheirPieces(Scenario scenario) {
    UUID maker = owner.accountId();
    FiringCompleted completed =
        new FiringCompleted(
            owner.requireStudioId(),
            Uuids.v7(),
            FiringKind.GLAZE,
            Uuids.v7(),
            "Skutt",
            List.of(
                new FiringCompleted.FiredPiece(Uuids.v7(), 7, maker, "Jug", Instant.now()),
                new FiringCompleted.FiredPiece(Uuids.v7(), 3, maker, null, Instant.now())));

    inStudio(() -> {
      scenario
          .publish(completed)
          .andWaitForEventOfType(NotificationCreated.class)
          .matching(created -> created.accountId().equals(maker))
          .toArriveAndVerify(created -> {
            assertThat(created.kind()).isEqualTo(NotificationView.Kind.PIECES_READY);
            assertThat(created.title()).isEqualTo("Your 2 pieces are ready to collect");
            assertThat(created.body()).startsWith("Piece 3\nPiece 7: Jug");
          });
      return null;
    });
    assertThat(inStudio(() -> notifications.mine()).items())
        .extracting(NotificationView::title)
        .containsExactly("Your 2 pieces are ready to collect");
  }

  private <T> T inStudio(ScopedValue.CallableOp<T, RuntimeException> work) {
    return TenantContext.callAs(owner, work);
  }
}
