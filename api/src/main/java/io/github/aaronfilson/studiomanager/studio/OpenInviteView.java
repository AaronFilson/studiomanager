package io.github.aaronfilson.studiomanager.studio;

import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * An invite nobody has accepted yet.
 *
 * @param email who it was emailed to; {@code null} for a link invite
 * @param role the invitee's role's base
 * @param roleId the role row they join in; {@code null} on invites made before custom roles
 * @param lastSentAt when the email last went out; {@code null} until it has
 */
public record OpenInviteView(
    UUID id,
    @Nullable String email,
    StudioRole role,
    @Nullable UUID roleId,
    Instant createdAt,
    Instant expiresAt,
    @Nullable Instant lastSentAt) {

  public boolean expiredAt(Instant now) {
    return !expiresAt.isAfter(now);
  }
}
