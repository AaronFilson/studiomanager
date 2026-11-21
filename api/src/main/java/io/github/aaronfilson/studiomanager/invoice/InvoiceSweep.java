package io.github.aaronfilson.studiomanager.invoice;

import io.github.aaronfilson.studiomanager.platform.storage.BlobStore;
import io.github.aaronfilson.studiomanager.platform.sweep.Sweep;
import io.github.aaronfilson.studiomanager.platform.sweep.SweepRunner;
import io.github.aaronfilson.studiomanager.platform.tenancy.SystemActor;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Operational cleanup for the supplies inbox (docs/planning/16, "Safety"):
 *
 * <ul>
 *   <li>Purges raw reader JSON ({@code invoice.raw}) older than 90 days while keeping the confirmed
 *       draft and stock history intact.
 *   <li>Removes abandoned {@code UPLOADING} invoice rows older than 24 hours along with any blob
 *       bytes that were uploaded without completing.
 * </ul>
 *
 * <p>Runs across studios via {@code SECURITY DEFINER} functions under {@link
 * SystemActor#INVOICE_CLEANUP} (ADR 10, ADR 16, ADR 23).
 */
@Component
@ConditionalOnBooleanProperty(name = "studiomanager.invoices.sweep", matchIfMissing = true)
class InvoiceSweep implements Sweep {

  private static final Logger log = LoggerFactory.getLogger(InvoiceSweep.class);

  static final Duration KEEP_RAW = Duration.ofDays(90);
  static final Duration ABANDONED_AFTER = Duration.ofDays(1);

  /** Result of one cleanup pass. */
  record Result(int rawAnswersPurged, int abandonedUploadsRemoved) {}

  private record AbandonedInvoice(UUID studioId, UUID invoiceId, InvoiceFileType fileType) {}

  private final JdbcClient jdbc;
  private final TransactionOperations transactions;
  private final BlobStore blobs;
  private final SweepRunner sweeps;
  private final Clock clock;

  InvoiceSweep(
      JdbcClient jdbc,
      TransactionOperations transactions,
      BlobStore blobs,
      SweepRunner sweeps,
      Clock clock) {
    this.jdbc = jdbc;
    this.transactions = transactions;
    this.blobs = blobs;
    this.sweeps = sweeps;
    this.clock = clock;
  }

  @Scheduled(initialDelayString = "PT8M", fixedDelayString = "PT1H")
  public void run() {
    sweeps.run(this);
  }

  @Override
  public String name() {
    return "invoice-cleanup";
  }

  @Override
  public void sweep() {
    runAt(clock.instant());
  }

  /** Purges raw reader output older than 90 days and removes abandoned uploads older than 1 day. */
  Result runAt(Instant now) {
    int purged = purgeRawBefore(now.minus(KEEP_RAW));
    int removed = removeAbandonedBefore(now.minus(ABANDONED_AFTER));
    if (purged > 0 || removed > 0) {
      log.info(
          "Invoice cleanup: purged {} raw reader answers and removed {} abandoned uploads",
          purged,
          removed);
    }
    return new Result(purged, removed);
  }

  private int purgeRawBefore(Instant before) {
    Integer purged =
        TenantContext.callAs(
            TenantContext.system(SystemActor.INVOICE_CLEANUP),
            () ->
                transactions.execute(tx ->
                    jdbc.sql("select app.purge_invoice_raw_answers(:before)")
                        .param("before", before.atOffset(ZoneOffset.UTC))
                        .query(Integer.class)
                        .single()));
    return purged == null ? 0 : purged;
  }

  private int removeAbandonedBefore(Instant before) {
    List<AbandonedInvoice> taken =
        TenantContext.callAs(
            TenantContext.system(SystemActor.INVOICE_CLEANUP),
            () ->
                transactions.execute(tx ->
                    jdbc.sql(
                            "select studio_id, invoice_id, file_type"
                                + " from app.take_abandoned_invoice_uploads(:before)")
                        .param("before", before.atOffset(ZoneOffset.UTC))
                        .query((rs, row) ->
                            new AbandonedInvoice(
                                rs.getObject("studio_id", UUID.class),
                                rs.getObject("invoice_id", UUID.class),
                                InvoiceFileType.valueOf(rs.getString("file_type"))))
                        .list()));
    if (taken == null || taken.isEmpty()) {
      return 0;
    }
    for (AbandonedInvoice abandoned : taken) {
      try {
        blobs.delete(
            InvoiceBlobs.name(abandoned.studioId(), abandoned.invoiceId(), abandoned.fileType()));
      } catch (RuntimeException e) {
        log.atWarn()
            .setCause(e)
            .addKeyValue("studioId", abandoned.studioId())
            .addKeyValue("invoiceId", abandoned.invoiceId())
            .log("Couldn't delete blob for abandoned invoice {}", abandoned.invoiceId());
      }
    }
    return taken.size();
  }
}
