package io.github.aaronfilson.studiomanager.piece;

/** How many pieces are in one stage: a row of {@link PieceRepository#countByStage}. */
record PieceStageCount(PieceStage stage, long pieces) {}
