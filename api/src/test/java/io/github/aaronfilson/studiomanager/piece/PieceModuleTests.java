package io.github.aaronfilson.studiomanager.piece;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aaronfilson.studiomanager.TestEmailConfiguration;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.TestcontainersConfiguration;
import io.github.aaronfilson.studiomanager.firing.PieceSize;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountResolver;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.StudioService;
import io.github.aaronfilson.studiomanager.studio.UnitSystem;
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
 * The piece module's event, with only piece and what it depends on running: taking in a piece
 * publishes {@link PieceReceived}, which the ledger charges and the stream shows. Those listeners
 * aren't started here.
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
class PieceModuleTests {

  @Autowired AccountResolver accounts;
  @Autowired StudioService studios;
  @Autowired PieceService pieces;

  private TenantContext owner;

  @BeforeEach
  void studio() {
    TestUser person = TestUser.named("Pip");
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
                    studios.create("Module Pieces", UnitSystem.METRIC, "Europe/London", "GB", null))
            .id();
    owner = signedIn.inStudio(studioId, StudioRole.OWNER);
  }

  @Test
  void takingInAPiecePublishesPieceReceivedWithItsNumberAndFee(Scenario scenario) {
    inStudio(() -> {
      scenario
          .stimulate(() ->
              pieces.intake(new PieceIntake(null, "Teapot", null, null, PieceSize.unmeasured())))
          .andWaitForEventOfType(PieceReceived.class)
          .matching(received -> received.makerAccountId().equals(owner.accountId()))
          .toArriveAndVerify((received, piece) -> {
            assertThat(received.pieceId()).isEqualTo(piece.id());
            assertThat(received.number()).isEqualTo(piece.number());
            assertThat(received.fee()).isEqualTo(piece.fee());
          });
      return null;
    });
  }

  private <T> T inStudio(ScopedValue.CallableOp<T, RuntimeException> work) {
    return TenantContext.callAs(owner, work);
  }
}
