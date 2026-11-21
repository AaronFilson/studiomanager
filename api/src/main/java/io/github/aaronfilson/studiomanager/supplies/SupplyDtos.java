package io.github.aaronfilson.studiomanager.supplies;

import io.github.aaronfilson.studiomanager.contract.SupplyCategoryDto;
import io.github.aaronfilson.studiomanager.contract.SupplyItemDto;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.Item;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneOffset;
import org.jspecify.annotations.Nullable;

/** Converts supply items to the HTTP contract, for this module and the "today" view. */
public final class SupplyDtos {

  private SupplyDtos() {}

  public static SupplyItemDto toDto(Item item) {
    return new SupplyItemDto()
        .id(item.id())
        .name(item.name())
        .category(SupplyCategoryDto.fromValue(item.category().name()))
        .unit(item.unit())
        .supplier(item.supplier())
        .leadTimeDays(item.leadTimeDays())
        .parLevel(plain(item.parLevel()))
        .reorderQuantity(plain(item.reorderQuantity()))
        .salePrice(item.salePrice() == null ? null : item.salePrice().toPlainString())
        .lastPurchaseUnitPrice(formatPrice(item.lastPurchaseUnitPrice()))
        .lastPurchasedAt(
            item.lastPurchasedAt() == null ? null : item.lastPurchasedAt().atOffset(ZoneOffset.UTC))
        .taxCategory(item.taxCategory())
        .substitute(item.substitute())
        .onHand(item.onHand().stripTrailingZeros().toPlainString())
        .onOrder(item.onOrder().stripTrailingZeros().toPlainString())
        .low(item.low())
        .needsOrdering(item.needsOrdering())
        .version(item.version());
  }

  private static @Nullable String plain(@Nullable BigDecimal value) {
    return value == null ? null : value.stripTrailingZeros().toPlainString();
  }

  /** Strips trailing zeros beyond cents while preserving at least 2 decimal places. */
  private static @Nullable String formatPrice(@Nullable BigDecimal value) {
    if (value == null) {
      return null;
    }
    BigDecimal normalized = value.stripTrailingZeros();
    if (normalized.scale() < 2) {
      normalized = normalized.setScale(2, RoundingMode.UNNECESSARY);
    }
    return normalized.toPlainString();
  }
}
