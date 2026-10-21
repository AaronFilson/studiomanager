package io.github.aaronfilson.studiomanager.kiln;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * A kiln was unloaded and its pieces moved on. Published inside the unloading transaction, so
 * anything a listener writes (such as notifications) commits or rolls back with it.
 */
public record FiringCompleted(UUID studioId, UUID firingId, FiringKind kind, String kilnName,
        List<FiredPiece> pieces) {

    public record FiredPiece(UUID pieceId, int number, UUID makerAccountId, @Nullable String description,
            @Nullable Instant holdUntil) {
    }

}
