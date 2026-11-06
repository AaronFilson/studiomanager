package io.github.aaronfilson.studiomanager.studio;

import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import java.time.Instant;
import java.util.UUID;

/** A new invite. The code is shown once, at creation; only its hash is stored. */
public record InviteView(UUID id, String code, StudioRole role, Instant expiresAt) {}
