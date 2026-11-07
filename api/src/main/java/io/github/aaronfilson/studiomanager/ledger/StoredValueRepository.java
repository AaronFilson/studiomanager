package io.github.aaronfilson.studiomanager.ledger;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/** The bound studio's stored value ({@code @TenantId}). Balances are read in SQL. */
interface StoredValueRepository extends JpaRepository<StoredValue, UUID> {

  /** Locks the balance, so two uses at once can't both spend it. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from StoredValue s where s.id = :storedValueId")
  Optional<StoredValue> findForUpdate(UUID storedValueId);
}
