package io.github.aaronfilson.studiomanager.platform.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Stored sets, the floor, duty grants, the minute's cache and shadow mode (plan 21, lane R4). */
class RolePermissionsTests {

  private final UUID studioId = UUID.randomUUID();
  private final UUID accountId = UUID.randomUUID();
  private final Map<StudioRole, Set<Permission>> stored = new EnumMap<>(StudioRole.class);
  private final Map<UUID, Set<Permission>> customRoles = new HashMap<>();
  private final Set<Permission> dutyGrants = EnumSet.noneOf(Permission.class);
  private final AtomicInteger storeReads = new AtomicInteger();
  private final SteppingClock clock = new SteppingClock();
  private final SimpleMeterRegistry meters = new SimpleMeterRegistry();

  private RolePermissions rolePermissions(PermissionSettings.Mode mode) {
    StoredRolePermissions store =
        new StoredRolePermissions() {
          @Override
          public Optional<Set<Permission>> of(StudioRole role) {
            storeReads.incrementAndGet();
            return Optional.ofNullable(stored.get(role));
          }

          @Override
          public Optional<Set<Permission>> ofRoleId(UUID roleId) {
            storeReads.incrementAndGet();
            return Optional.ofNullable(customRoles.get(roleId));
          }
        };
    return new RolePermissions(
        store, ignoredAccount -> dutyGrants, new PermissionSettings(mode), meters, clock);
  }

  private <T> T inStudio(StudioRole role, ScopedValue.CallableOp<T, RuntimeException> operation) {
    return TenantContext.callAs(new TenantContext(accountId, studioId, role), operation);
  }

  private TenantContext resolved(RolePermissions rolePermissions, StudioRole role) {
    TenantContext person = new TenantContext(accountId, studioId, role);
    return TenantContext.callAs(person, () -> rolePermissions.resolve(person));
  }

  @Test
  void theStoredSetDecidesWithTheFloorAdded() {
    stored.put(StudioRole.STAFF, Set.of(Permission.PIECES_MOVE));
    TenantContext staff =
        resolved(rolePermissions(PermissionSettings.Mode.ENFORCING), StudioRole.STAFF);
    assertThat(staff.permissions())
        .containsExactlyInAnyOrder(Permission.PIECES_MOVE, Permission.MONEY_OWN_READ);
  }

  @Test
  void aStudioWithNoStoredRoleKeepsTheCodeDefaults() {
    TenantContext member =
        resolved(rolePermissions(PermissionSettings.Mode.ENFORCING), StudioRole.MEMBER);
    assertThat(member.permissions()).isEqualTo(Permission.defaultsFor(StudioRole.MEMBER));
  }

  @Test
  void anOwnerHoldsEverythingWhateverIsStored() {
    stored.put(StudioRole.OWNER, Set.of());
    TenantContext owner =
        resolved(rolePermissions(PermissionSettings.Mode.ENFORCING), StudioRole.OWNER);
    assertThat(owner.permissions()).containsExactlyInAnyOrder(Permission.values());
  }

  @Test
  void anOwnerOnlyPermissionInTheStoreIsNeverHeldByAnotherRole() {
    stored.put(StudioRole.STAFF, Set.of(Permission.STUDIO_EXPORT, Permission.PIECES_MOVE));
    TenantContext staff =
        resolved(rolePermissions(PermissionSettings.Mode.ENFORCING), StudioRole.STAFF);
    assertThat(staff.permissions()).doesNotContain(Permission.STUDIO_EXPORT);
  }

  @Test
  void dutiesAddWhatTheyGrant() {
    stored.put(StudioRole.MEMBER, Set.of(Permission.STUDIO_READ));
    dutyGrants.add(Permission.PIECES_INTAKE_FOR_OTHERS);
    TenantContext member =
        resolved(rolePermissions(PermissionSettings.Mode.ENFORCING), StudioRole.MEMBER);
    assertThat(member.permissions())
        .containsExactlyInAnyOrder(
            Permission.STUDIO_READ, Permission.MONEY_OWN_READ, Permission.PIECES_INTAKE_FOR_OTHERS);
  }

  @Test
  void aCustomRoleHoldsItsOwnSetWithTheFloorAndDuties() {
    UUID kilnTech = UUID.randomUUID();
    stored.put(StudioRole.MEMBER, Set.of(Permission.STUDIO_READ, Permission.PIECES_READ));
    customRoles.put(kilnTech, Set.of(Permission.KILNS_READ, Permission.FIRINGS_MANAGE));
    dutyGrants.add(Permission.SCHEDULE_SHIFTS_TAKE_DUTY);
    RolePermissions rolePermissions = rolePermissions(PermissionSettings.Mode.ENFORCING);
    TenantContext person = new TenantContext(accountId, studioId, StudioRole.MEMBER);

    TenantContext resolved =
        TenantContext.callAs(
            person,
            () -> rolePermissions.resolve(person, new MemberRole(StudioRole.MEMBER, kilnTech)));

    assertThat(resolved.role()).isEqualTo(StudioRole.MEMBER);
    assertThat(resolved.permissions())
        .containsExactlyInAnyOrder(
            Permission.KILNS_READ,
            Permission.FIRINGS_MANAGE,
            Permission.MONEY_OWN_READ,
            Permission.SCHEDULE_SHIFTS_TAKE_DUTY);
  }

  @Test
  void aCustomRoleNeverHoldsAnOwnerOnlyPermission() {
    UUID board = UUID.randomUUID();
    customRoles.put(board, Set.of(Permission.STUDIO_EXPORT, Permission.MONEY_READ));
    RolePermissions rolePermissions = rolePermissions(PermissionSettings.Mode.ENFORCING);
    Set<Permission> held =
        inStudio(
            StudioRole.STAFF,
            () -> rolePermissions.ofMemberRole(new MemberRole(StudioRole.STAFF, board)));
    assertThat(held).contains(Permission.MONEY_READ).doesNotContain(Permission.STUDIO_EXPORT);
    assertThat(RolePermissions.mayBaseACustomRole(StudioRole.OWNER)).isFalse();
    assertThat(RolePermissions.mayBaseACustomRole(StudioRole.MEMBER)).isTrue();
  }

  @Test
  void aCustomRoleIsCachedAndForgottenWithItsStudio() {
    UUID kilnTech = UUID.randomUUID();
    customRoles.put(kilnTech, Set.of(Permission.KILNS_READ));
    RolePermissions rolePermissions = rolePermissions(PermissionSettings.Mode.ENFORCING);
    MemberRole role = new MemberRole(StudioRole.MEMBER, kilnTech);
    inStudio(StudioRole.MEMBER, () -> rolePermissions.ofMemberRole(role));
    customRoles.put(kilnTech, Set.of(Permission.KILNS_MANAGE));

    assertThat(
            inStudio(
                StudioRole.MEMBER, () -> rolePermissions.memberHolds(role, Permission.KILNS_READ)))
        .isTrue();
    assertThat(storeReads).hasValue(1);

    rolePermissions.onChange(new RolePermissionsChanged(studioId));
    assertThat(
            inStudio(
                StudioRole.MEMBER,
                () -> rolePermissions.memberHolds(role, Permission.KILNS_MANAGE)))
        .isTrue();
    assertThat(storeReads).hasValue(2);
  }

  @Test
  void roleHoldsReadsTheStoredSet() {
    stored.put(StudioRole.STAFF, Set.of());
    RolePermissions rolePermissions = rolePermissions(PermissionSettings.Mode.ENFORCING);
    inStudio(
        StudioRole.MEMBER,
        () -> {
          assertThat(rolePermissions.roleHolds(StudioRole.STAFF, Permission.MAINTENANCE_MANAGE))
              .isFalse();
          assertThat(rolePermissions.roleHolds(StudioRole.OWNER, Permission.MAINTENANCE_MANAGE))
              .isTrue();
          return null;
        });
  }

  @Test
  void aSetIsCachedForAMinuteAndForgottenOnChange() {
    stored.put(StudioRole.STAFF, Set.of(Permission.PIECES_MOVE));
    RolePermissions rolePermissions = rolePermissions(PermissionSettings.Mode.ENFORCING);
    resolved(rolePermissions, StudioRole.STAFF);
    stored.put(StudioRole.STAFF, Set.of(Permission.KILNS_MANAGE));

    assertThat(resolved(rolePermissions, StudioRole.STAFF).holds(Permission.PIECES_MOVE)).isTrue();
    assertThat(storeReads).hasValue(1);

    clock.advance(RolePermissions.CACHE_FOR.plusSeconds(1));
    assertThat(resolved(rolePermissions, StudioRole.STAFF).holds(Permission.KILNS_MANAGE)).isTrue();
    assertThat(storeReads).hasValue(2);

    stored.put(StudioRole.STAFF, Set.of(Permission.FEES_EDIT));
    rolePermissions.onChange(new RolePermissionsChanged(studioId));
    assertThat(resolved(rolePermissions, StudioRole.STAFF).holds(Permission.FEES_EDIT)).isTrue();
    assertThat(storeReads).hasValue(3);
  }

  @Test
  void shadowModeKeepsTheDefaultsAndCountsTheDifference() {
    stored.put(StudioRole.STAFF, Set.of(Permission.PIECES_MOVE));
    TenantContext staff =
        resolved(rolePermissions(PermissionSettings.Mode.SHADOW), StudioRole.STAFF);
    assertThat(staff.permissions()).isEqualTo(Permission.defaultsFor(StudioRole.STAFF));
    assertThat(meters.counter("studiomanager.permissions.shadow_mismatch", "role", "STAFF").count())
        .isEqualTo(1.0);
  }

  @Test
  void shadowModeCountsNothingWhenTheStoreMatches() {
    stored.putAll(RolePermissions.storedDefaults());
    RolePermissions rolePermissions = rolePermissions(PermissionSettings.Mode.SHADOW);
    for (StudioRole role : StudioRole.values()) {
      assertThat(resolved(rolePermissions, role).permissions())
          .isEqualTo(Permission.defaultsFor(role));
    }
    assertThat(meters.find("studiomanager.permissions.shadow_mismatch").counters()).isEmpty();
  }

  @Test
  void theSeededSetsAreTheCodeDefaults() {
    stored.putAll(RolePermissions.storedDefaults());
    RolePermissions rolePermissions = rolePermissions(PermissionSettings.Mode.ENFORCING);
    for (StudioRole role : StudioRole.values()) {
      assertThat(resolved(rolePermissions, role).permissions())
          .as("%s", role)
          .isEqualTo(Permission.defaultsFor(role));
    }
    assertThat(RolePermissions.storedDefaults().get(StudioRole.OWNER)).isEmpty();
  }

  @Test
  void onlyEditableSetsCanBeStored() {
    assertThatThrownBy(() -> RolePermissions.storableFor(StudioRole.OWNER, Set.of()))
        .isInstanceOf(InvalidInputException.class);
    assertThatThrownBy(
            () -> RolePermissions.storableFor(StudioRole.STAFF, Set.of(Permission.OWNERS_MANAGE)))
        .isInstanceOf(InvalidInputException.class);
    assertThat(
            RolePermissions.storableFor(
                StudioRole.MEMBER, Set.of(Permission.MONEY_OWN_READ, Permission.FEES_EDIT)))
        .containsExactly(Permission.FEES_EDIT);
  }

  /** A clock that stands still until a test moves it. */
  private static final class SteppingClock extends Clock {

    private Instant now = Instant.parse("2026-11-07T12:00:00Z");

    void advance(Duration by) {
      now = now.plus(by);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
