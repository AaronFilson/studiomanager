package io.github.aaronfilson.studiomanager.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aaronfilson.studiomanager.TestEmailConfiguration;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.TestcontainersConfiguration;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountResolver;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.Shift;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;
import io.github.aaronfilson.studiomanager.studio.StudioService;
import io.github.aaronfilson.studiomanager.studio.UnitSystem;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * The schedule module's events, with only schedule and what it depends on running. The notify
 * module, which tells people, isn't started, so these check what schedule publishes.
 *
 * <p>Scenario runs each stimulus in a transaction of its own, and row-level security and
 * Hibernate's tenant are read when a transaction begins, so each scenario runs inside {@link #as}.
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
class ScheduleModuleTests {

  @Autowired AccountResolver accounts;
  @Autowired StudioService studios;
  @Autowired StudioMembers members;
  @Autowired ShiftChanges shiftChanges;

  private TenantContext owner;
  private TenantContext member;

  @BeforeEach
  void studioWithAMember() {
    TenantContext signedIn = TenantContext.forAccount(account(TestUser.named("Ola")));
    UUID studioId =
        TenantContext.callAs(
                signedIn,
                () ->
                    studios.create("Module Roster", UnitSystem.METRIC, "Europe/London", "GB", null))
            .id();
    owner = signedIn.inStudio(studioId, StudioRole.OWNER);
    String code = as(owner, () -> members.invite(StudioRole.MEMBER).code());
    TenantContext joining = TenantContext.forAccount(account(TestUser.named("Pim")));
    TenantContext.callAs(joining, () -> members.accept(code));
    member = joining.inStudio(studioId, StudioRole.MEMBER);
  }

  @Test
  void askingForCoverPublishesShiftOfferedToWhoeverCouldTakeIt(Scenario scenario) {
    Shift shift = heldByOwner();

    as(
        owner,
        () -> {
          scenario
              .stimulate(() -> shiftChanges.offer(shift.id(), true, 1))
              .andWaitForEventOfType(ShiftOffered.class)
              .matching(offered -> offered.shiftId().equals(shift.id()))
              .toArriveAndVerify(offered -> {
                assertThat(offered.title()).isEqualTo("Front desk");
                // Anyone could cover a shift with no duty, except whoever already has it
                assertThat(offered.recipients()).containsExactly(member.accountId());
              });
          return null;
        });
  }

  @Test
  void takingAnOfferedShiftPublishesShiftCovered(Scenario scenario) {
    Shift shift = heldByOwner();
    as(owner, () -> shiftChanges.offer(shift.id(), true, 1));

    as(
        member,
        () -> {
          scenario
              .stimulate(() -> shiftChanges.assign(shift.id(), member.accountId(), 2))
              .andWaitForEventOfType(ShiftCovered.class)
              .matching(covered -> covered.shiftId().equals(shift.id()))
              .toArriveAndVerify(
                  covered -> assertThat(covered.previousHolder()).isEqualTo(owner.accountId()));
          return null;
        });
  }

  /** A shift tomorrow, with no duty, that the owner has taken. */
  private Shift heldByOwner() {
    Instant tomorrow = Instant.now().plus(1, ChronoUnit.DAYS);
    UUID shiftId =
        as(owner, () -> shiftChanges.createWeeklyShifts("Front desk", null, tomorrow, 60, 1))
            .getFirst()
            .id();
    return as(owner, () -> shiftChanges.assign(shiftId, owner.accountId(), 0));
  }

  private UUID account(TestUser person) {
    return accounts.resolve(
        Jwt.withTokenValue("module-test")
            .header("alg", "none")
            .claim("iss", TestUser.ISSUER)
            .subject(person.subject())
            .claim("name", person.name())
            .claim("email", person.email())
            .build());
  }

  private static <T> T as(TenantContext who, ScopedValue.CallableOp<T, RuntimeException> work) {
    return TenantContext.callAs(who, work);
  }
}
