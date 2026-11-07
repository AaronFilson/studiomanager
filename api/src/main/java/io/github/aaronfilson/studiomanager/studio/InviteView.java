package io.github.aaronfilson.studiomanager.studio;

import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import java.time.Instant;
import java.util.UUID;

/**
 * A new invite. The code is shown once, at creation; only its hash is stored.
 *
 * @param role the invitee's role's base
 * @param roleId the role row they join in, system or custom
 * @param roleName that role's name, such as {@code Kiln tech}
 */
public record InviteView(
    UUID id, String code, StudioRole role, UUID roleId, String roleName, Instant expiresAt) {}
