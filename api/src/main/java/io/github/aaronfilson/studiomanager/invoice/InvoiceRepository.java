package io.github.aaronfilson.studiomanager.invoice;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/** The bound studio's invoices; Hibernate adds the studio to every query ({@code @TenantId}). */
interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

  /** The inbox, newest first. The lines come in one batch per page (batch fetch size, ADR 21). */
  @Query("select i from Invoice i order by i.createdAt desc, i.id desc")
  List<Invoice> findNewestFirst(Limit limit);

  /** One invoice with its lines, in one statement. */
  @Query("select i from Invoice i left join fetch i.lines where i.id = :id")
  Optional<Invoice> findWithLines(UUID id);

  /**
   * Loads one invoice under a shared row lock so concurrent discard or confirm transactions
   * serialize cleanly against saving a comparison or reader run.
   */
  @Lock(LockModeType.PESSIMISTIC_READ)
  @Query("select i from Invoice i where i.id = :id")
  Optional<Invoice> findForShare(UUID id);

  /**
   * Loads one invoice under an exclusive row lock so background reading cannot race a concurrent
   * discard when persisting its run and draft.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select i from Invoice i where i.id = :id")
  Optional<Invoice> findAndLock(UUID id);

  /** Other invoices in a status with this invoice number, any case, oldest first. */
  @Query(
      """
      select i from Invoice i
      where i.id <> :id and i.status = :status and lower(i.invoiceNumber) = lower(:number)
      order by i.confirmedAt, i.id
      """)
  List<Invoice> findOthersNumbered(UUID id, String number, InvoiceStatus status);
}
