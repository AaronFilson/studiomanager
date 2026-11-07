package io.github.aaronfilson.studiomanager.invoice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** The bound studio's invoices; Hibernate adds the studio to every query ({@code @TenantId}). */
interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

  /** The inbox, newest first. The lines come in one batch per page (batch fetch size, ADR 21). */
  @Query("select i from Invoice i order by i.createdAt desc, i.id desc")
  List<Invoice> findNewestFirst(Limit limit);

  /** One invoice with its lines, in one statement. */
  @Query("select i from Invoice i left join fetch i.lines where i.id = :id")
  Optional<Invoice> findWithLines(UUID id);
}
