package io.github.aaronfilson.studiomanager.proposal;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A proposed purchase request: more of one supply item. Approving it asks for this quantity, as
 * {@code SupplyService.request} does from the Supplies page.
 *
 * @param quantity always set: a proposal without one takes the item's usual reorder quantity
 */
@Embeddable
public record PurchaseRequestDraft(UUID itemId, BigDecimal quantity, @Nullable String note) {}
