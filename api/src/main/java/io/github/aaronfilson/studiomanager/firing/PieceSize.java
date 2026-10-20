package io.github.aaronfilson.studiomanager.firing;

import io.github.aaronfilson.studiomanager.platform.measure.Length;
import io.github.aaronfilson.studiomanager.platform.measure.Weight;

import org.jspecify.annotations.Nullable;

/** What was measured of a piece: its bounding box, its weight, both, or neither. */
public record PieceSize(@Nullable Length length, @Nullable Length width, @Nullable Length height,
        @Nullable Weight weight) {

    public PieceSize {
        boolean any = length != null || width != null || height != null;
        boolean all = length != null && width != null && height != null;
        if (any && !all) {
            throw new IllegalArgumentException("Give all three dimensions or none");
        }
    }

    public static PieceSize unmeasured() {
        return new PieceSize(null, null, null, null);
    }

    public boolean hasDimensions() {
        return length != null;
    }

}
