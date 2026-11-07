package io.github.aaronfilson.studiomanager.supplies;

import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioPermissions;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;
import io.github.aaronfilson.studiomanager.studio.StudioService;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.Item;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Selling an item to a member: stock goes down, and {@link SupplySold} puts the price (plus tax) on
 * their balance in the same transaction.
 */
@Service
public class SupplySales {

  private final StockMovements stock;
  private final SupplyService supplies;
  private final StudioMembers members;
  private final StudioService studios;
  private final ApplicationEventPublisher events;
  private final Clock clock;
  private final StudioPermissions permissions;

  SupplySales(
      StockMovements stock,
      SupplyService supplies,
      StudioMembers members,
      StudioService studios,
      ApplicationEventPublisher events,
      Clock clock,
      StudioPermissions permissions) {
    this.permissions = permissions;
    this.stock = stock;
    this.supplies = supplies;
    this.members = members;
    this.studios = studios;
    this.events = events;
    this.clock = clock;
  }

  @Transactional
  public Item sell(UUID itemId, UUID memberId, BigDecimal quantity) {
    permissions.require(Permission.SUPPLIES_SELL, SupplyService.ONLY_STAFF);
    SupplyItem item = stock.lock(itemId);
    BigDecimal price = item.salePrice();
    if (price == null) {
      throw new InvalidInputException(
          "%s isn't for sale; give it a price first.".formatted(item.name()));
    }
    if (members.roleOf(memberId).isEmpty()) {
      throw new InvalidInputException("That person isn't a member of this studio.");
    }
    BigDecimal amount =
        price
            .multiply(SupplyService.positive(quantity))
            .setScale(
                studios.current().currency().getDefaultFractionDigits(), RoundingMode.HALF_UP);
    // The movement is flushed before the ledger's listener writes with SQL
    UUID movementId = stock.sell(item, memberId, quantity);
    events.publishEvent(
        new SupplySold(
            movementId,
            memberId,
            item.name(),
            quantity,
            amount,
            item.taxCategory(),
            clock.instant()));
    return supplies.item(itemId);
  }
}
