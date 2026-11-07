package io.github.aaronfilson.studiomanager.ledger;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Journal lines in the bound studio ({@code @TenantId}); written once, summed in SQL. */
interface JournalLineRepository extends JpaRepository<JournalLine, UUID> {}
