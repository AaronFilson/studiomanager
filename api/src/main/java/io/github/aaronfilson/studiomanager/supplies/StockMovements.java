package io.github.aaronfilson.studiomanager.supplies;

import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.MovementKind;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stock going on and off the shelf. Every movement is added with its item's row locked, so two at
 * once can't both see the last bag.
 *
 * <p>Each movement is flushed as it's added: {@link SupplyRanOut} and {@link SupplySold} have
 * synchronous listeners that write with SQL in the same transaction (ADR 21).
 */
@Service
class StockMovements {

  private final SupplyItemRepository itemRepository;
  private final StockMovementRepository movementRepository;
  private final EntityManager entityManager;
  private final ApplicationEventPublisher events;

  StockMovements(
      SupplyItemRepository itemRepository,
      StockMovementRepository movementRepository,
      EntityManager entityManager,
      ApplicationEventPublisher events) {
    this.itemRepository = itemRepository;
    this.movementRepository = movementRepository;
    this.entityManager = entityManager;
    this.events = events;
  }

  /**
   * Stock received, used, or counted by hand. For a count, {@code quantity} is what is on the
   * shelf, and the movement records the difference from the books; a count that matches them adds
   * nothing.
   */
  @Transactional
  public void adjust(UUID itemId, MovementKind kind, BigDecimal quantity, @Nullable String note) {
    SupplyItem item = lock(itemId);
    BigDecimal onHand = movementRepository.onHand(itemId);
    BigDecimal change =
        switch (kind) {
          case RECEIVED -> SupplyService.positive(quantity);
          case USED -> SupplyService.positive(quantity).negate();
          case COUNTED -> {
            if (quantity.signum() < 0) {
              throw new InvalidInputException("A count can't be negative.");
            }
            yield quantity.subtract(onHand);
          }
          case SOLD ->
              throw new InvalidInputException(
                  "Record sales with the sale endpoint, so the member is charged.");
        };
    if (change.signum() != 0) {
      add(item, onHand, new StockMovement(itemId, kind, change, null, SupplyService.clean(note)));
    }
  }

  /**
   * Loads an item and holds its row until the transaction ends. Every movement starts here.
   *
   * @throws NotFoundException if the studio has no such item
   */
  @Transactional
  public SupplyItem lock(UUID itemId) {
    return itemRepository
        .findAndLock(itemId)
        .orElseThrow(() -> new NotFoundException("No such item."));
  }

  /**
   * Takes a sale off the shelf of an item already {@linkplain #lock locked}. Returns the movement's
   * id, which the sale's ledger entry points back to.
   */
  @Transactional
  public UUID sell(SupplyItem lockedItem, UUID memberId, BigDecimal quantity) {
    StockMovement movement =
        new StockMovement(lockedItem.id(), MovementKind.SOLD, quantity.negate(), memberId, null);
    add(lockedItem, movementRepository.onHand(lockedItem.id()), movement);
    return movement.id();
  }

  /**
   * Adds a movement. If it takes an item with a substitute from in stock to out, members are told
   * what to use instead ({@link SupplyRanOut}); once per stock-out, because it fires on the change,
   * not on every movement while the shelf is empty.
   */
  private void add(SupplyItem item, BigDecimal onHandBefore, StockMovement movement) {
    // Persisted, not saved: the id is set, so Spring Data's save would look for the row first
    entityManager.persist(movement);
    entityManager.flush();
    BigDecimal onHandAfter = onHandBefore.add(movement.quantity());
    String substitute = item.substitute();
    if (onHandBefore.signum() > 0 && onHandAfter.signum() <= 0 && substitute != null) {
      events.publishEvent(
          new SupplyRanOut(
              TenantContext.require().requireStudioId(), item.id(), item.name(), substitute));
    }
  }
}
