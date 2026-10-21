package io.github.aaronfilson.studiomanager.kiln;

import java.util.UUID;

/** A kiln was added to the bound studio. Maintenance gives it an asset, in the same transaction. */
public record KilnCreated(UUID kilnId, UUID studioId) {
}
