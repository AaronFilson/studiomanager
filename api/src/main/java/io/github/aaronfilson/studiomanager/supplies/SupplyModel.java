package io.github.aaronfilson.studiomanager.supplies;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/** Supplies' types. */
public final class SupplyModel {

    private SupplyModel() {
    }

    public enum Category {
        CLAY,
        GLAZE,
        GLAZE_MATERIAL,
        TOOL,
        KILN_SUPPLY,
        CLEANING,
        OTHER
    }

    public enum MovementKind {
        RECEIVED,
        SOLD,
        USED,
        /** A stock count's correction to what the books say. */
        COUNTED
    }

    public enum RequestStatus {
        REQUESTED,
        APPROVED,
        REJECTED,
        ORDERED,
        RECEIVED
    }

    /**
     * An item and its stock. {@code low} is true at or below the par level; {@code onOrder} counts
     * approved and ordered requests, so a low item that's already on its way doesn't nag.
     */
    public record Item(UUID id, String name, Category category, String unit, @Nullable String supplier,
            @Nullable Integer leadTimeDays, @Nullable BigDecimal parLevel, @Nullable BigDecimal reorderQuantity,
            @Nullable BigDecimal salePrice, @Nullable String taxCategory, @Nullable String substitute, BigDecimal onHand,
            BigDecimal onOrder, long version) {

        public boolean low() {
            return parLevel != null && onHand.compareTo(parLevel) <= 0;
        }

        public boolean needsOrdering() {
            return low() && onOrder.signum() == 0;
        }
    }

    /** What staff can set on an item. */
    public record ItemSettings(String name, Category category, String unit, @Nullable String supplier,
            @Nullable Integer leadTimeDays, @Nullable BigDecimal parLevel, @Nullable BigDecimal reorderQuantity,
            @Nullable BigDecimal salePrice, @Nullable String taxCategory, @Nullable String substitute) {
    }

    public record Movement(UUID id, MovementKind kind, BigDecimal quantity, @Nullable UUID memberId,
            @Nullable String memberName, @Nullable String note, Instant createdAt) {
    }

    public record PurchaseRequest(UUID id, UUID itemId, String itemName, BigDecimal quantity, @Nullable String note,
            RequestStatus status, UUID requestedBy, @Nullable String requestedByName, Instant requestedAt,
            @Nullable String decidedByName, long version) {
    }

}
