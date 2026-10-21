package io.github.aaronfilson.studiomanager.piece;

import java.time.Instant;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.platform.Money;

/**
 * A piece was taken in and priced. Published inside the intake transaction, so the ledger posts
 * the fee to the maker's balance atomically with the piece itself.
 */
public record PieceReceived(UUID pieceId, int number, UUID makerAccountId, Money fee, String taxCategory,
        Instant receivedAt) {
}
