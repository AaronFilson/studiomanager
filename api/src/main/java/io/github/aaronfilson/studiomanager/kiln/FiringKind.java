package io.github.aaronfilson.studiomanager.kiln;

import io.github.aaronfilson.studiomanager.piece.PieceStage;

/** What a firing does to the pieces in it: which stage it takes them from, and to. */
public enum FiringKind {

    /** Greenware to bisqueware, ready for members to glaze. */
    BISQUE(PieceStage.RECEIVED, PieceStage.BISQUE_FIRED),

    /** Glazed work to finished pieces, ready for pickup. */
    GLAZE(PieceStage.GLAZED, PieceStage.READY);

    private final PieceStage takes;
    private final PieceStage leaves;

    FiringKind(PieceStage takes, PieceStage leaves) {
        this.takes = takes;
        this.leaves = leaves;
    }

    public PieceStage takes() {
        return takes;
    }

    public PieceStage leaves() {
        return leaves;
    }

}
