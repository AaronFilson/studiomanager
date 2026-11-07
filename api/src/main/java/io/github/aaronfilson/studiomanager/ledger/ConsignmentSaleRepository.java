package io.github.aaronfilson.studiomanager.ledger;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/**
 * The bound studio's consignment sales, fetched with their item, whose description every view
 * shows. Hibernate adds the studio to every query ({@code @TenantId}).
 */
interface ConsignmentSaleRepository extends JpaRepository<ConsignmentSaleEntity, UUID> {

  /** Locks the sale, so it can't be refunded twice at once. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from ConsignmentSaleEntity s where s.id = :saleId")
  Optional<ConsignmentSaleEntity> findForUpdate(UUID saleId);

  /** Newest first; everyone's when {@code payeeId} is null. */
  @Query(
      """
      select s from ConsignmentSaleEntity s join fetch s.item i
      where (:payeeId is null or i.payee.id = :payeeId)
      order by s.soldOn desc, s.createdAt desc
      """)
  List<ConsignmentSaleEntity> findNewestFirst(@Nullable UUID payeeId, Limit limit);
}
