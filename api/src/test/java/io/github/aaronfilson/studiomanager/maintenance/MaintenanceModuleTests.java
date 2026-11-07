package io.github.aaronfilson.studiomanager.maintenance;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aaronfilson.studiomanager.TestEmailConfiguration;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.TestcontainersConfiguration;
import io.github.aaronfilson.studiomanager.kiln.KilnCreated;
import io.github.aaronfilson.studiomanager.kiln.KilnService;
import io.github.aaronfilson.studiomanager.kiln.KilnView;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.AssetKind;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.TaskSettings;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountResolver;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.StudioService;
import io.github.aaronfilson.studiomanager.studio.UnitSystem;
import java.time.Duration;
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
 * The maintenance module's events, with only maintenance and what it depends on running. The
 * modules that listen (notify, the stream, webhooks) aren't started, so these check what
 * maintenance publishes, and what it does with the kiln module's {@link KilnCreated}.
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
class MaintenanceModuleTests {

  @Autowired AccountResolver accounts;
  @Autowired StudioService studios;
  @Autowired KilnService kilns;
  @Autowired MaintenanceService maintenance;
  @Autowired AssetChanges assetChanges;
  @Autowired TaskChanges taskChanges;
  @Autowired MaintenanceReminders reminders;
  @Autowired ProblemReportService problems;

  private TenantContext owner;

  @BeforeEach
  void studio() {
    TestUser person = TestUser.named("Rae");
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
                    studios.create("Module Upkeep", UnitSystem.METRIC, "Europe/London", "GB", null))
            .id();
    owner = signedIn.inStudio(studioId, StudioRole.OWNER);
  }

  @Test
  void aNewKilnGetsItsAssetInTheSameTransaction(Scenario scenario) {
    inStudio(() -> {
      scenario
          .stimulate(() ->
              kilns.create(
                  new KilnService.KilnSettings("Bailey", null, KilnView.KilnType.GAS, null)))
          .andWaitForEventOfType(KilnCreated.class)
          .toArriveAndVerify(created ->
              assertThat(inStudio(() -> maintenance.assetOfKiln(created.kilnId()))).isPresent());
      return null;
    });
    assertThat(inStudio(() -> maintenance.assets(Instant.now())))
        .extracting(MaintenanceModel.Asset::name)
        .containsExactly("Bailey");
  }

  @Test
  void reportingAProblemPublishesProblemReported(Scenario scenario) {
    UUID wheel = asset("Wheel 4");

    inStudio(() -> {
      scenario
          .stimulate(() -> problems.report(wheel, "  Grinds when it slows  "))
          .andWaitForEventOfType(ProblemReported.class)
          .toArriveAndVerify(reported -> {
            assertThat(reported.assetName()).isEqualTo("Wheel 4");
            assertThat(reported.description()).isEqualTo("Grinds when it slows");
            // The owner reported it, and already knows
            assertThat(reported.recipients()).doesNotContain(owner.accountId());
          });
      return null;
    });
  }

  @Test
  void aTaskComingDuePublishesMaintenanceDueOncePerCycle(Scenario scenario) {
    UUID trap = asset("Sink trap");
    inStudio(() ->
        taskChanges.createTask(
            trap, new TaskSettings("Empty the trap", List.of(), 7, null, null, null)));
    Instant nearlyDue = Instant.now().plus(Duration.ofDays(6).plusHours(12));

    inStudio(() -> {
      scenario
          .stimulate(() -> reminders.remindDue(nearlyDue))
          .andWaitForEventOfType(MaintenanceDue.class)
          .toArriveAndVerify(due -> {
            assertThat(due.assetName()).isEqualTo("Sink trap");
            assertThat(due.taskTitle()).isEqualTo("Empty the trap");
            // Nobody is assigned, so the staff hear
            assertThat(due.recipients()).containsExactly(owner.accountId());
          });
      return null;
    });
    assertThat(inStudio(() -> reminders.remindDue(nearlyDue))).isZero();
  }

  private UUID asset(String name) {
    return inStudio(() -> assetChanges.createAsset(AssetKind.WHEEL, name, null, null).id());
  }

  private <T> T inStudio(ScopedValue.CallableOp<T, RuntimeException> work) {
    return TenantContext.callAs(owner, work);
  }
}
