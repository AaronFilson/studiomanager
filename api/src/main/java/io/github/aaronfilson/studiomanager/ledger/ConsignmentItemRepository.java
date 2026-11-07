package io.github.aaronfilson.studiomanager.ledger;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/**
 * The bound studio's consigned work, always fetched with its payee, whose name every view shows.
 * Hibernate adds the studio to every query ({@code @TenantId}).
 */
interface ConsignmentItemRepository extends JpaRepository<ConsignmentItemEntity, UUID> {

  @Query("select i from ConsignmentItemEntity i join fetch i.payee where i.id = :itemId")
  Optional<ConsignmentItemEntity> findWithPayee(UUID itemId);

  /** Locks the item, so two sales at once can't both take the last one. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select i from ConsignmentItemEntity i where i.id = :itemId")
  Optional<ConsignmentItemEntity> findForUpdate(UUID itemId);

  /** Newest first; everyone's work when {@code payeeId} is null. */
  @Query(
      """
      select i from ConsignmentItemEntity i join fetch i.payee p
      where (:payeeId is null or p.id = :payeeId)
        and (:inStockOnly = false or i.sold + i.returned < i.quantity)
      order by i.receivedOn desc, i.createdAt desc
      """)
  List<ConsignmentItemEntity> findNewestFirst(
      @Nullable UUID payeeId, boolean inStockOnly, Limit limit);

  /** Work kept past its return date with some still on the shelf, oldest first. */
  @Query(
      """
      select i from ConsignmentItemEntity i join fetch i.payee p
      where (:payeeId is null or p.id = :payeeId)
        and i.sold + i.returned < i.quantity and i.returnBy < :today
      order by i.returnBy, i.createdAt
      """)
  List<ConsignmentItemEntity> findDueBack(@Nullable UUID payeeId, LocalDate today, Limit limit);
}
