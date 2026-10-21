package io.github.aaronfilson.studiomanager.supplies;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Something was sold to a member, such as a bag of clay. Published inside the sale's transaction
 * so the ledger puts it on the member's balance atomically with the stock movement.
 */
public record SupplySold(UUID movementId, UUID memberId, String itemName, BigDecimal quantity, BigDecimal amount,
        @Nullable String taxCategory, Instant soldAt) {
}
