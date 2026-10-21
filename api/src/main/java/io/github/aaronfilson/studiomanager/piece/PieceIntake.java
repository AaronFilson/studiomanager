package io.github.aaronfilson.studiomanager.piece;

import java.util.UUID;

import io.github.aaronfilson.studiomanager.firing.PieceSize;

import org.jspecify.annotations.Nullable;

/**
 * A piece being dropped off.
 *
 * @param makerAccountId whose piece it is; null means the caller's own. Only staff can take in
 *     pieces for someone else.
 */
public record PieceIntake(@Nullable UUID makerAccountId, @Nullable String description, @Nullable String clayBody,
        @Nullable String makerMark, PieceSize size) {
}
