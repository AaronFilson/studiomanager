package io.github.aaronfilson.studiomanager.kiln;

import java.time.Instant;
import java.util.UUID;

/** A loaded kiln was switched on. Published inside the transaction that started it. */
public record FiringStarted(UUID studioId, UUID firingId, UUID kilnId, String kilnName, FiringKind kind,
        Instant startedAt) {
}
