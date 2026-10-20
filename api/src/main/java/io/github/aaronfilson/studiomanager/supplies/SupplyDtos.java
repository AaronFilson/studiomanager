package io.github.aaronfilson.studiomanager.supplies;

import java.math.BigDecimal;

import io.github.aaronfilson.studiomanager.contract.SupplyCategoryDto;
import io.github.aaronfilson.studiomanager.contract.SupplyItemDto;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.Item;

import org.jspecify.annotations.Nullable;

/** Converts supply items to the HTTP contract, for this module and the "today" view. */
public final class SupplyDtos {

    private SupplyDtos() {
    }

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

}
