package io.github.aaronfilson.studiomanager.studio;

import java.time.Instant;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;

/** A new invite. The code is shown once, at creation; only its hash is stored. */
public record InviteView(UUID id, String code, StudioRole role, Instant expiresAt) {
}
