package io.github.aaronfilson.studiomanager.studio;

import java.time.Instant;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;

import org.jspecify.annotations.Nullable;

/**
 * An invite nobody has accepted yet.
 *
 * @param email who it was emailed to; {@code null} for a link invite
 * @param lastSentAt when the email last went out; {@code null} until it has
 */
public record OpenInviteView(UUID id, @Nullable String email, StudioRole role, Instant createdAt,
        Instant expiresAt, @Nullable Instant lastSentAt) {

    public boolean expiredAt(Instant now) {
        return !expiresAt.isAfter(now);
    }

}
