package io.github.aaronfilson.studiomanager.studio;

import java.time.Instant;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;

import org.jspecify.annotations.Nullable;

/** A person in the studio the request is bound to. */
public record MemberView(UUID accountId, @Nullable String displayName, StudioRole role, Instant joinedAt) {
}
