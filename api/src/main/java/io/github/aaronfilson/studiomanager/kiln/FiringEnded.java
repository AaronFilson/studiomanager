package io.github.aaronfilson.studiomanager.kiln;

import java.time.Instant;
import java.util.UUID;

/**
 * A firing is over: unloaded ({@code completed}) or cancelled, whether or not it had started.
 * Published inside the transaction that ended it, alongside {@link FiringCompleted} for an unload.
 */
public record FiringEnded(UUID studioId, UUID firingId, UUID kilnId, Instant endedAt, boolean completed) {
}
