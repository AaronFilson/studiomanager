package io.github.aaronfilson.studiomanager.invoice;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Pages read this calendar month (UTC) by the platform-paid readers, counted from {@code
 * invoice_read}: for the caps before each read, and for the admin page.
 */
@Component
public class PlatformReaderUsage {

  /** One studio's use of one reader this month. */
  public record StudioUse(
      UUID studioId, InvoiceReaderKind reader, long reads, long pages, BigDecimal costUsd) {}

  private final InvoiceReadRepository reads;
  private final InvoiceQueries queries;
  private final TransactionOperations transactions;
  private final Clock clock;

  PlatformReaderUsage(
      InvoiceReadRepository reads,
      InvoiceQueries queries,
      TransactionOperations transactions,
      Clock clock) {
    this.reads = reads;
    this.queries = queries;
    this.transactions = transactions;
    this.clock = clock;
  }

  /** The start of this calendar month, in UTC. */
  public Instant monthStart() {
    return LocalDate.now(clock.withZone(ZoneOffset.UTC))
        .withDayOfMonth(1)
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant();
  }

  /** Pages the reader has read for the bound studio this month. */
  long studioPagesThisMonth(InvoiceReaderKind reader) {
    Instant since = monthStart();
    Long pages = transactions.execute(tx -> reads.pagesSince(reader, since));
    return pages == null ? 0 : pages;
  }

  /** Pages the reader has read for every studio together this month. */
  long platformPagesThisMonth(InvoiceReaderKind reader) {
    Instant since = monthStart();
    Map<InvoiceReaderKind, Long> pages =
        Objects.requireNonNull(transactions.execute(tx -> queries.platformPagesSince(since)));
    return pages.getOrDefault(reader, 0L);
  }

  /** Each studio's use this month. Only a platform admin may ask; the database checks it. */
  public List<StudioUse> byStudioThisMonth() {
    Instant since = monthStart();
    List<StudioUse> used = transactions.execute(tx -> queries.usageByStudioSince(since));
    return used == null ? List.of() : used;
  }
}
