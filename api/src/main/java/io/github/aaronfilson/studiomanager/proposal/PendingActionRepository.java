package io.github.aaronfilson.studiomanager.proposal;

import io.github.aaronfilson.studiomanager.proposal.PendingActionView.Status;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * The bound studio's pending actions; Hibernate adds the studio to every query ({@code @TenantId}).
 */
interface PendingActionRepository extends JpaRepository<PendingAction, UUID> {

  /** Those with this status, oldest first, up to {@code limit}. */
  List<PendingAction> findByStatusOrderByCreatedAtAscIdAsc(Status status, Limit limit);
}
