package io.github.aaronfilson.studiomanager.access;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** One grant with its type's name, as a list shows it: a JPQL projection. */
record CertificationRow(
    UUID typeId, String name, UUID accountId, Instant grantedAt, @Nullable Instant expiresAt) {}
