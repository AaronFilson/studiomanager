package io.github.aaronfilson.studiomanager.ledger;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/** Opening balances held against invites in the bound studio ({@code @TenantId}). */
interface PendingOpeningBalanceRepository extends JpaRepository<PendingOpeningBalance, UUID> {

  /** The ones not yet posted, oldest first, locked so a retried event can't post one twice. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select b from PendingOpeningBalance b
      where b.inviteId in :inviteIds and b.entryId is null
      order by b.createdAt
      """)
  List<PendingOpeningBalance> findUnpostedForUpdate(Collection<UUID> inviteIds);
}
