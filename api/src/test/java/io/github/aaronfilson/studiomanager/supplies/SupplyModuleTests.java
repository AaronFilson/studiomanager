package io.github.aaronfilson.studiomanager.supplies;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aaronfilson.studiomanager.TestEmailConfiguration;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.TestcontainersConfiguration;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountResolver;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.StudioService;
import io.github.aaronfilson.studiomanager.studio.UnitSystem;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.Category;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.ItemSettings;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.MovementKind;
import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
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
 * The supplies module's events, with only supplies and what it depends on running. The ledger,
 * notify, webhooks and the stream, which listen, aren't started, so these check what supplies
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
class SupplyModuleTests {

  @Autowired AccountResolver accounts;
  @Autowired StudioService studios;
  @Autowired SupplyService supplies;
  @Autowired SupplyItems items;
  @Autowired SupplySales sales;

  private TenantContext owner;

  @BeforeEach
  void studio() {
    TestUser person = TestUser.named("Sam");
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
                        "Module Supplies", UnitSystem.METRIC, "Europe/London", "GB", null))
            .id();
    owner = signedIn.inStudio(studioId, StudioRole.OWNER);
  }

  @Test
  void sellingToAMemberPublishesTheSaleWithItsPrice(Scenario scenario) {
    UUID clay = item("Porcelain", new BigDecimal("12.50"), null);
    inStudio(() -> supplies.recordMovement(clay, MovementKind.RECEIVED, BigDecimal.TEN, null));

    inStudio(() -> {
      scenario
          .stimulate(() -> sales.sell(clay, owner.accountId(), new BigDecimal("2")))
          .andWaitForEventOfType(SupplySold.class)
          .matching(sold -> sold.memberId().equals(owner.accountId()))
          .toArriveAndVerify(sold -> {
            assertThat(sold.itemName()).isEqualTo("Porcelain");
            assertThat(sold.amount()).isEqualByComparingTo("25.00");
          });
      return null;
    });
    assertThat(inStudio(() -> supplies.item(clay)).onHand()).isEqualByComparingTo("8");
  }

  @Test
  void takingTheLastOfAnItemWithASubstitutePublishesThatItRanOut(Scenario scenario) {
    UUID clay = item("Speckled buff", null, "Use B-Mix 5 while we wait");
    inStudio(() -> supplies.recordMovement(clay, MovementKind.RECEIVED, BigDecimal.ONE, null));

    inStudio(() -> {
      scenario
          .stimulate(() -> supplies.recordMovement(clay, MovementKind.USED, BigDecimal.ONE, null))
          .andWaitForEventOfType(SupplyRanOut.class)
          .matching(ranOut -> ranOut.itemId().equals(clay))
          .toArriveAndVerify(ranOut -> {
            assertThat(ranOut.itemName()).isEqualTo("Speckled buff");
            assertThat(ranOut.substitute()).isEqualTo("Use B-Mix 5 while we wait");
          });
      return null;
    });
  }

  private UUID item(String name, @Nullable BigDecimal salePrice, @Nullable String substitute) {
    return inStudio(() ->
        items
            .create(
                new ItemSettings(
                    name,
                    Category.CLAY,
                    "25 lb bag",
                    null,
                    null,
                    null,
                    null,
                    salePrice,
                    null,
                    substitute))
            .id());
  }

  private <T> T inStudio(ScopedValue.CallableOp<T, RuntimeException> work) {
    return TenantContext.callAs(owner, work);
  }
}
