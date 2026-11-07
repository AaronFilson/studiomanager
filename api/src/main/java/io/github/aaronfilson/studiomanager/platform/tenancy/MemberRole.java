package io.github.aaronfilson.studiomanager.platform.tenancy;

import java.util.Objects;
import java.util.UUID;

/**
 * The role a member holds in a studio (plan 21, lane R7): the role row they point at, which may be
 * a system role or one of the studio's custom roles, and its base, which is what the rest of the
 * system sees as their {@link StudioRole}.
 *
 * @param base the role's base: the system role's own rank, or the rank a custom role sits at
 * @param roleId the {@code studio_role} row
 */
public record MemberRole(StudioRole base, UUID roleId) {

  public MemberRole {
    Objects.requireNonNull(base, "base");
    Objects.requireNonNull(roleId, "roleId");
  }
}
