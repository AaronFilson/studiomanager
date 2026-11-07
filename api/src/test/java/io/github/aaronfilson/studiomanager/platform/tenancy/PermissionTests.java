package io.github.aaronfilson.studiomanager.platform.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PermissionTests {

  @Test
  void keysAreUniqueAndShapedAreaDotAction() {
    for (Permission permission : Permission.values()) {
      assertThat(permission.key()).as(permission.name()).matches("[a-z_]+(\\.[a-z_]+)+");
      assertThat(Permission.byKey(permission.key())).hasValue(permission);
    }
  }

  @Test
  void anUnknownKeyIsNotAPermission() {
    assertThat(Permission.byKey("pieces.everything")).isEmpty();
  }

  @Test
  void theOwnerOnlyPermissionsAreExactlyThePlansSix() {
    assertThat(Permission.ownerOnly())
        .containsExactlyInAnyOrder(
            Permission.STUDIO_EXPORT,
            Permission.STUDIO_AI_KEY_MANAGE,
            Permission.STUDIO_ROLES_MANAGE,
            Permission.API_TOKENS_MANAGE_ALL,
            Permission.SUPPLIES_PURCHASE_APPROVE,
            Permission.OWNERS_MANAGE);
  }

  @Test
  void anOwnerHoldsEverythingAndEachRoleHoldsWhatTheOneBelowDoes() {
    Set<Permission> owner = Permission.defaultsFor(StudioRole.OWNER);
    Set<Permission> staff = Permission.defaultsFor(StudioRole.STAFF);
    Set<Permission> member = Permission.defaultsFor(StudioRole.MEMBER);
    assertThat(owner).isEqualTo(EnumSet.allOf(Permission.class));
    assertThat(staff).containsAll(member);
    assertThat(staff).doesNotContainAnyElementsOf(Permission.ownerOnly());
    assertThat(member).doesNotContainAnyElementsOf(Permission.ownerOnly());
  }

  @Test
  void theDefaultsMatchTheRoleChecksTheyReplace() {
    Set<Permission> staff = Permission.defaultsFor(StudioRole.STAFF);
    Set<Permission> member = Permission.defaultsFor(StudioRole.MEMBER);
    // Money is split (decision 2), and staff hold both halves on day one
    assertThat(staff)
        .contains(
            Permission.MONEY_TAKE_PAYMENT,
            Permission.MONEY_REFUND_AND_ADJUST,
            Permission.MONEY_PAYOUTS,
            Permission.API_TOKENS_OWN,
            Permission.MCP_USE);
    assertThat(member)
        .contains(Permission.STUDIO_READ, Permission.PIECES_INTAKE_OWN, Permission.MONEY_OWN_READ)
        .doesNotContain(Permission.PIECES_MOVE, Permission.MONEY_READ, Permission.MCP_USE);
    // A duty, not a role, grants these
    assertThat(staff)
        .doesNotContain(
            Permission.SCHEDULE_SHIFTS_TAKE_DUTY, Permission.MAINTENANCE_COMPLETE_ASSIGNED);
  }

  @Test
  void theFloorIsHeldByEveryRole() {
    for (Permission permission : Permission.values()) {
      if (permission.isFloor()) {
        for (StudioRole role : StudioRole.values()) {
          assertThat(Permission.defaultsFor(role)).as(role.name()).contains(permission);
        }
      }
    }
  }

  @Test
  void aContextHoldsItsRolesDefaults() {
    UUID studioId = UUID.randomUUID();
    TenantContext staff =
        TenantContext.forAccount(UUID.randomUUID()).inStudio(studioId, StudioRole.STAFF);
    assertThat(staff.permissions()).isEqualTo(Permission.defaultsFor(StudioRole.STAFF));
    assertThat(staff.viaToken(UUID.randomUUID()).permissions()).isEqualTo(staff.permissions());
    assertThat(TenantContext.forAccount(UUID.randomUUID()).permissions()).isEmpty();
    assertThat(TenantContext.system(SystemActor.WEBHOOK_SWEEP, studioId).permissions())
        .isEqualTo(Permission.defaultsFor(StudioRole.STAFF));
  }

  @Test
  void onlyAnOwnerHoldsAnOwnerOnlyPermission() {
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new TenantContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                StudioRole.STAFF,
                TenantContext.PrincipalKind.PERSON,
                null,
                Set.of(Permission.STUDIO_EXPORT)));
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new TenantContext(
                UUID.randomUUID(),
                null,
                null,
                TenantContext.PrincipalKind.PERSON,
                null,
                Set.of(Permission.STUDIO_READ)));
  }

  @Test
  void aJobNeverHoldsMoreThanItsCeilingsDefaults() {
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new TenantContext(
                SystemActor.WEBHOOK_SWEEP.accountId(),
                UUID.randomUUID(),
                StudioRole.STAFF,
                TenantContext.PrincipalKind.SYSTEM,
                null,
                Set.of(Permission.SCHEDULE_SHIFTS_TAKE_DUTY)));
  }
}
