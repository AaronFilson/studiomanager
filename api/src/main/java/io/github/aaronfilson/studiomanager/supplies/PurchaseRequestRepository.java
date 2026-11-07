package io.github.aaronfilson.studiomanager.supplies;

import io.github.aaronfilson.studiomanager.supplies.SupplyModel.RequestStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Purchase requests, always fetched with their item, since every view shows its name. Hibernate
 * adds the studio to every query ({@code @TenantId}).
 */
interface PurchaseRequestRepository extends JpaRepository<PurchaseRequestEntity, UUID> {

  @Query("select r from PurchaseRequestEntity r join fetch r.item where r.id = :requestId")
  Optional<PurchaseRequestEntity> findWithItem(UUID requestId);

  @Query(
      """
      select r from PurchaseRequestEntity r join fetch r.item
      where r.status in :statuses
      order by r.requestedAt, r.id
      """)
  List<PurchaseRequestEntity> findWithItemByStatusIn(Collection<RequestStatus> statuses);
}
