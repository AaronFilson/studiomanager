package io.github.aaronfilson.studiomanager.supplies;

import io.github.aaronfilson.studiomanager.supplies.SupplyModel.RequestStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/**
 * The bound studio's supply items; Hibernate adds the studio to every query ({@code @TenantId}).
 *
 * <p>Stock on hand and on order are sums, worked out with the items in one query, so a list of any
 * length is one statement.
 */
interface SupplyItemRepository extends JpaRepository<SupplyItem, UUID> {

  /** The item with its stock; {@code :onOrder} names the request statuses that count as on order. */
  String WITH_STOCK =
      """
      select new io.github.aaronfilson.studiomanager.supplies.SupplyItemStock(
          i,
          coalesce((select sum(m.quantity) from StockMovement m where m.itemId = i.id), 0.0BD),
          coalesce((select sum(r.quantity) from PurchaseRequestEntity r
                    where r.item = i and r.status in :onOrder), 0.0BD))
      from SupplyItem i
      """;

  @Query(WITH_STOCK + " where i.archived = false order by i.category, i.name")
  List<SupplyItemStock> findActiveWithStock(Collection<RequestStatus> onOrder);

  @Query(WITH_STOCK + " where i.id = :itemId")
  Optional<SupplyItemStock> findWithStock(UUID itemId, Collection<RequestStatus> onOrder);

  /**
   * Loads an item and locks its row until the transaction ends, so two movements at once can't both
   * see the last bag on the shelf.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select i from SupplyItem i where i.id = :itemId")
  Optional<SupplyItem> findAndLock(UUID itemId);
}
