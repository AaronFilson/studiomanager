package io.github.aaronfilson.studiomanager.studio;

import io.github.aaronfilson.studiomanager.platform.tenancy.MemberRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A person in the studio the request is bound to.
 *
 * @param role their role's base
 * @param roleId the role row they hold: a system role or a custom one (plan 21, lane R7)
 * @param roleName that role's name in the studio, such as {@code Staff} or {@code Kiln tech}
 */
public record MemberView(
    UUID accountId,
    @Nullable String displayName,
    StudioRole role,
    UUID roleId,
    String roleName,
    Instant joinedAt) {

  /** The role they hold, for asking what it gives them. */
  public MemberRole memberRole() {
    return new MemberRole(role, roleId);
  }
}
