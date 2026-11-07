package io.github.aaronfilson.studiomanager.supplies;

import java.math.BigDecimal;

/**
 * An item with its stock, as one query returns it ({@link SupplyItemRepository}).
 *
 * @param onHand the sum of its movements
 * @param onOrder the quantity in approved and ordered purchase requests
 */
record SupplyItemStock(SupplyItem item, BigDecimal onHand, BigDecimal onOrder) {

  SupplyModel.Item toView() {
    return item.toView(onHand, onOrder);
  }
}
