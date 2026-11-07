package io.github.aaronfilson.studiomanager.invoice;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** The bound studio's reader runs. */
interface InvoiceReadRepository extends JpaRepository<InvoiceRead, UUID> {}
