package io.github.aaronfilson.studiomanager.ledger;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** The bound studio's journal; Hibernate adds the studio to every query ({@code @TenantId}). */
interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

  @Query("select e.id from JournalEntry e where e.idempotencyKey = :idempotencyKey")
  Optional<UUID> findIdByKey(String idempotencyKey);

  boolean existsByIdempotencyKey(String idempotencyKey);
}
