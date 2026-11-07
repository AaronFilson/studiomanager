package io.github.aaronfilson.studiomanager.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aaronfilson.studiomanager.TestEmailConfiguration;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.TestcontainersConfiguration;
import io.github.aaronfilson.studiomanager.piece.PieceReceived;
import io.github.aaronfilson.studiomanager.platform.Money;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountResolver;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.StudioService;
import io.github.aaronfilson.studiomanager.studio.UnitSystem;
import io.github.aaronfilson.studiomanager.supplies.SupplySold;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
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
 * The ledger's event flows, with only the ledger and what it depends on running: a piece taken in
 * and something sold at the desk each land on the member's balance, with tax.
 *
 * <p>Scenario runs each stimulus in a transaction of its own, and row-level security and
 * Hibernate's tenant are read when a transaction begins, so each scenario runs inside {@link
 * #inStudio}, and so does each check of the balance.
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
class LedgerModuleTests {

  private static final Currency USD = Currency.getInstance("USD");

  @Autowired AccountResolver accounts;
  @Autowired StudioService studios;
  @Autowired LedgerService ledger;
  @Autowired TaxRates taxRates;

  private TenantContext owner;

  @BeforeEach
  void studioWithRetailTax() {
    TestUser person = TestUser.named("Lee");
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
                        "Module Books", UnitSystem.US_CUSTOMARY, "America/Chicago", "US", null))
            .id();
    owner = signedIn.inStudio(studioId, StudioRole.OWNER);
    inStudio(() -> taxRates.setTaxRate("RETAIL", new BigDecimal("0.08"), LocalDate.of(2020, 1, 1)));
  }

  @Test
  void aPieceTakenInChargesItsMakerTheFeeWithTax(Scenario scenario) {
    PieceReceived received =
        new PieceReceived(
            Uuids.v7(),
            7,
            owner.accountId(),
            new Money(new BigDecimal("10.00"), USD),
            "RETAIL",
            Instant.now());

    inStudio(() -> {
      scenario
          .publish(received)
          .andWaitForStateChange(this::owedByOwner, owed -> owed.signum() != 0)
          .andVerify(owed -> assertThat(owed).isEqualByComparingTo("10.80"));
      return null;
    });
  }

  @Test
  void somethingSoldAtTheDeskGoesOnTheMembersBalanceOnce(Scenario scenario) {
    SupplySold sold =
        new SupplySold(
            Uuids.v7(),
            owner.accountId(),
            "Clay, 25 lb",
            BigDecimal.ONE,
            new BigDecimal("30.00"),
            null,
            Instant.now());

    inStudio(() -> {
      scenario
          .publish(sold)
          .andWaitForStateChange(this::owedByOwner, owed -> owed.signum() != 0)
          .andVerify(owed -> assertThat(owed).isEqualByComparingTo("32.40"));
      return null;
    });
    // Delivered twice, it posts once: the idempotency key is the stock movement
    inStudio(() -> {
      scenario
          .publish(sold)
          .andWaitForStateChange(this::owedByOwner)
          .andVerify(owed -> assertThat(owed).isEqualByComparingTo("32.40"));
      return null;
    });
  }

  private BigDecimal owedByOwner() {
    return inStudio(() -> ledger.balanceOf(owner.accountId()).owed());
  }

  private <T> T inStudio(ScopedValue.CallableOp<T, RuntimeException> work) {
    return TenantContext.callAs(owner, work);
  }
}
