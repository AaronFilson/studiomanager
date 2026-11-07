package io.github.aaronfilson.studiomanager.platform.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.AccessDeniedException;

class StudioPermissionsTests {

  private final StudioPermissions permissions = new StudioPermissions();
  private final StudioAuthorization studioAuthz = new StudioAuthorization(permissions);
  private final UUID studioId = UUID.randomUUID();

  private TenantContext as(StudioRole role) {
    return TenantContext.forAccount(UUID.randomUUID()).inStudio(studioId, role);
  }

  @Test
  void nothingIsHeldOutsideACall() {
    assertThat(permissions.has(Permission.STUDIO_READ)).isFalse();
    assertThat(studioAuthz.can(studioId, Permission.STUDIO_READ)).isFalse();
    assertThatThrownBy(() -> permissions.require(Permission.STUDIO_READ))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void aMemberCanReadButNotMovePieces() {
    TenantContext.callAs(
        as(StudioRole.MEMBER),
        () -> {
          assertThat(studioAuthz.can(studioId, Permission.PIECES_READ)).isTrue();
          assertThat(studioAuthz.can(studioId, Permission.PIECES_MOVE)).isFalse();
          permissions.require(Permission.PIECES_INTAKE_OWN);
          assertThatThrownBy(() -> permissions.require(Permission.PIECES_MOVE))
              .isInstanceOf(AccessDeniedException.class);
          return null;
        });
  }

  @Test
  void staffCantDoWhatOnlyOwnersCan() {
    TenantContext.callAs(
        as(StudioRole.STAFF),
        () -> {
          assertThat(studioAuthz.can(studioId, Permission.PIECES_MOVE)).isTrue();
          assertThat(studioAuthz.can(studioId, Permission.STUDIO_EXPORT)).isFalse();
          return null;
        });
    TenantContext.callAs(
        as(StudioRole.OWNER),
        () -> {
          assertThat(studioAuthz.can(studioId, Permission.STUDIO_EXPORT)).isTrue();
          return null;
        });
  }

  @Test
  void aPermissionHoldsOnlyInTheBoundStudio() {
    TenantContext.callAs(
        as(StudioRole.OWNER),
        () -> {
          assertThat(studioAuthz.can(UUID.randomUUID(), Permission.STUDIO_READ)).isFalse();
          return null;
        });
  }

  @Test
  void anAnnotationNamesThePermissionAsText() {
    // As in @PreAuthorize("@studioAuthz.can(#studioId, 'PIECES_MOVE')")
    StandardEvaluationContext evaluation = new StandardEvaluationContext(studioAuthz);
    evaluation.setVariable("studioId", studioId);
    Expression check = new SpelExpressionParser().parseExpression("can(#studioId, 'PIECES_MOVE')");
    TenantContext.callAs(
        as(StudioRole.STAFF),
        () -> {
          assertThat(check.getValue(evaluation, Boolean.class)).isTrue();
          return null;
        });
  }

  @Test
  void canAgreesWithHasRoleForEveryRoleOnDayOne() {
    // R1 changes no behaviour: a STAFF-default permission is held exactly where hasRole(STAFF) is
    for (StudioRole role : StudioRole.values()) {
      TenantContext.callAs(
          as(role),
          () -> {
            for (Permission permission : Permission.values()) {
              StudioRole minimum = minimumRoleFor(permission.defaultGrant());
              assertThat(studioAuthz.can(studioId, permission))
                  .as("%s %s", role, permission)
                  .isEqualTo(studioAuthz.hasRole(studioId, minimum));
            }
            return null;
          });
    }
  }

  private static StudioRole minimumRoleFor(Permission.Grant grant) {
    return switch (grant) {
      case FLOOR -> StudioRole.MEMBER;
      case MEMBER -> StudioRole.MEMBER;
      case STAFF -> StudioRole.STAFF;
      case DUTY_ONLY -> StudioRole.OWNER;
      case OWNER_ONLY -> StudioRole.OWNER;
    };
  }
}
