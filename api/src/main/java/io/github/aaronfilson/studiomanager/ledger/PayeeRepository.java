package io.github.aaronfilson.studiomanager.ledger;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/** The bound studio's payees; Hibernate adds the studio to every query ({@code @TenantId}). */
interface PayeeRepository extends JpaRepository<PayeeEntity, UUID> {

  /** Active payees first, then by name. */
  @Query("select p from PayeeEntity p order by p.archivedAt nulls first, p.name")
  List<PayeeEntity> findInOrder(Limit limit);

  @Query("select p from PayeeEntity p where p.accountId = :accountId")
  Optional<PayeeEntity> findByAccount(UUID accountId);

  /** Locks the payee, so two payments at once can't both pay the same balance. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from PayeeEntity p where p.id = :payeeId")
  Optional<PayeeEntity> findForUpdate(UUID payeeId);
}
