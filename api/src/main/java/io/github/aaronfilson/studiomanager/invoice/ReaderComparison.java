package io.github.aaronfilson.studiomanager.invoice;

import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.storage.BlobStore;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

/**
 * "Compare readers": one invoice through every reader allowed for the studio, one after another,
 * with what each read, how long it took and what it cost (docs/planning/20, "Invoice readers"). The
 * invoice's own draft is left alone. Each run is recorded in {@code invoice_read} like any other,
 * so it counts toward the platform's page caps.
 *
 * <p>Like the reading job, no transaction is open while a provider works.
 */
@Service
class ReaderComparison {

  /**
   * One reader's go.
   *
   * @param draft what it read, through the same gate as a real read; null when it failed
   * @param note why it failed, safe to show
   */
  record Compared(
      InvoiceReaderKind reader,
      InvoiceRead.Outcome outcome,
      @Nullable InvoiceDraft draft,
      List<String> problems,
      @Nullable String note,
      long elapsedMs,
      ReaderResult.@Nullable Usage usage) {}

  private final InvoiceRepository invoices;
  private final InvoiceReadRepository reads;
  private final InvoiceReaders readers;
  private final BlobStore blobs;
  private final TransactionOperations transactions;
  private final Clock clock;

  ReaderComparison(
      InvoiceRepository invoices,
      InvoiceReadRepository reads,
      InvoiceReaders readers,
      BlobStore blobs,
      TransactionOperations transactions,
      Clock clock) {
    this.invoices = invoices;
    this.reads = reads;
    this.readers = readers;
    this.blobs = blobs;
    this.transactions = transactions;
    this.clock = clock;
  }

  List<Compared> compare(UUID studioId, UUID invoiceId) {
    InvoiceInbox.requireBound(studioId);
    Invoice invoice =
        transactions.execute(tx ->
            invoices
                .findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("No such invoice.")));
    if (invoice == null) {
      throw new NotFoundException("No such invoice.");
    }
    if (invoice.status() == InvoiceStatus.UPLOADING) {
      throw new ConflictException("The file hasn't been uploaded yet.");
    }
    InvoiceFileType type = invoice.fileType();
    InvoiceFile file =
        new InvoiceFile(
            blobs.head(InvoiceBlobs.name(studioId, invoiceId, type), InvoiceBlobs.MAX_BYTES), type);
    List<Compared> compared = new ArrayList<>();
    for (InvoiceReader reader : readers.allForBoundStudio()) {
      compared.add(run(reader, invoiceId, file));
    }
    return compared;
  }

  private Compared run(InvoiceReader reader, UUID invoiceId, InvoiceFile file) {
    long started = clock.millis();
    ReaderResult result = null;
    InvoiceRead.Outcome outcome;
    String note = null;
    try {
      result = reader.read(file);
      outcome = InvoiceRead.Outcome.READ;
    } catch (InvoiceReadException e) {
      outcome = InvoiceRead.Outcome.of(e.failure());
      note = e.getMessage();
    }
    long elapsed = clock.millis() - started;
    ReaderResult.Usage usage = result == null ? null : result.usage();
    InvoiceRead.Outcome ended = outcome;
    transactions.executeWithoutResult(
        tx -> reads.save(new InvoiceRead(invoiceId, reader.kind(), ended, usage, elapsed)));
    if (result == null) {
      return new Compared(reader.kind(), outcome, null, List.of(), note, elapsed, null);
    }
    InvoiceDrafts.Sanitized sanitized =
        InvoiceDrafts.sanitize(result.draft(), LocalDate.now(clock.withZone(ZoneOffset.UTC)));
    List<String> problems = new ArrayList<>(sanitized.notes());
    problems.addAll(InvoiceChecks.problems(sanitized.draft()));
    return new Compared(
        reader.kind(), outcome, sanitized.draft(), problems, null, elapsed, result.usage());
  }
}
