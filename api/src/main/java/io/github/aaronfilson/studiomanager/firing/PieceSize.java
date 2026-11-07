package io.github.aaronfilson.studiomanager.firing;

import io.github.aaronfilson.studiomanager.platform.measure.Length;
import io.github.aaronfilson.studiomanager.platform.measure.Weight;
import org.jspecify.annotations.Nullable;

/** What was measured of a piece: its bounding box, its weight, both, or neither. */
public record PieceSize(
    @Nullable Length length,
    @Nullable Length width,
    @Nullable Length height,
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
    return box() != null;
  }

  /** The three dimensions together, or null when the piece wasn't measured. */
  public @Nullable Box box() {
    if (length == null || width == null || height == null) {
      return null;
    }
    return new Box(length, width, height);
  }

  /** A piece's bounding box: all three dimensions, none of them missing. */
  public record Box(Length length, Length width, Length height) {}
}
