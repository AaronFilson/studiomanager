package io.github.aaronfilson.studiomanager.invoice;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** The bound studio's reader runs. */
interface InvoiceReadRepository extends JpaRepository<InvoiceRead, UUID> {

  /** The pages one reader has read for the bound studio since a moment. */
  @Query(
      "select coalesce(sum(r.pages), 0) from InvoiceRead r"
          + " where r.reader = :reader and r.createdAt >= :since")
  long pagesSince(@Param("reader") InvoiceReaderKind reader, @Param("since") Instant since);
}
