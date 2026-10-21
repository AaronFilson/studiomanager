package io.github.aaronfilson.studiomanager.piece;

import java.util.EnumSet;
import java.util.Set;

/**
 * Where a piece is, from drop-off to leaving the studio. The allowed moves are an explicit
 * table, so an impossible jump (greenware straight to picked up) is a 400, not a data bug.
 *
 * <pre>
 * RECEIVED ──► BISQUE_FIRED ──► GLAZED ──► READY ──► PICKED_UP
 *    └─────────────(single fire)──┘   │       │         ▲
 *                 BISQUE_FIRED ──(bisque only)┘       UNCLAIMED ──► DISPOSED
 * </pre>
 */
public enum PieceStage {

    RECEIVED,
    BISQUE_FIRED,
    GLAZED,
    READY,
    PICKED_UP,
    UNCLAIMED,
    DISPOSED;

    public Set<PieceStage> next() {
        return switch (this) {
            case RECEIVED -> EnumSet.of(BISQUE_FIRED, GLAZED);
            case BISQUE_FIRED -> EnumSet.of(GLAZED, READY);
            case GLAZED -> EnumSet.of(READY);
            case READY -> EnumSet.of(PICKED_UP, UNCLAIMED);
            case UNCLAIMED -> EnumSet.of(PICKED_UP, DISPOSED);
            case PICKED_UP, DISPOSED -> EnumSet.noneOf(PieceStage.class);
        };
    }

    public boolean canMoveTo(PieceStage target) {
        return next().contains(target);
    }

    /** The piece has left the studio's care, one way or the other. */
    public boolean isFinal() {
        return next().isEmpty();
    }

}
