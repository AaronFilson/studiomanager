package io.github.aaronfilson.studiomanager.kiln;

import java.util.UUID;

/** How many pieces one firing holds, from {@link FiringRepository#countPieces}. */
record FiringPieceCount(UUID firingId, long pieces) {}
