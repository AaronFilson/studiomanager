package io.github.aaronfilson.studiomanager.invoice;

import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.storage.BlobStore;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

/**
 * "Compare readers": one invoice through every reader at the same time, with what each read, how
 * long it took and what it cost (docs/planning/20, "Invoice readers"). A reader that may not read
 * for the studio is listed with the reason instead of being left out (docs/planning/24). Each
 * successful compared draft is stored in {@code invoice_comparison} and scored when staff confirm
 * the invoice (or immediately if the invoice is already confirmed).
 *
 * <p>Like the reading job, no transaction is open while a provider works.
 */
@Service
class ReaderComparison {

  private static final Logger log = LoggerFactory.getLogger(ReaderComparison.class);

  /**
   * One reader's go.
   *
   * @param availability whether it could read; when it couldn't, nothing ran
   * @param outcome how the read went; null when it didn't run
   * @param draft what it read, through the same gate as a real read; null when it failed
   * @param note why it failed or didn't run, safe to show
   */
  record Compared(
      InvoiceReaderKind reader,
      ReaderAvailability availability,
      InvoiceRead.@Nullable Outcome outcome,
      @Nullable InvoiceDraft draft,
      List<String> problems,
      List<String> explanations,
      @Nullable String note,
      long elapsedMs,
      ReaderResult.@Nullable Usage usage) {}

  private final InvoiceRepository invoices;
  private final InvoiceReadRepository reads;
  private final InvoiceQueries queries;
  private final InvoiceReaders readers;
  private final BlobStore blobs;
  private final TransactionOperations transactions;
  private final Clock clock;

  ReaderComparison(
      InvoiceRepository invoices,
      InvoiceReadRepository reads,
      InvoiceQueries queries,
      InvoiceReaders readers,
      BlobStore blobs,
      TransactionOperations transactions,
      Clock clock) {
    this.invoices = invoices;
    this.reads = reads;
    this.queries = queries;
    this.readers = readers;
    this.blobs = blobs;
    this.transactions = transactions;
    this.clock = clock;
  }

  List<Compared> compare(UUID studioId, UUID invoiceId) {
    InvoiceInbox.requireBound(studioId);
    InvoiceFileType type =
        transactions.execute(tx -> {
          Invoice invoice =
              invoices
                  .findById(invoiceId)
                  .orElseThrow(() -> new NotFoundException("No such invoice."));
          if (invoice.status() == InvoiceStatus.UPLOADING) {
            throw new ConflictException("The file hasn't been uploaded yet.");
          }
          return invoice.fileType();
        });
    if (type == null) {
      throw new NotFoundException("No such invoice.");
    }
    // Every reader gets the same upright, size-limited copy (docs/planning/24, "Images")
    InvoiceFile file =
        ReadingCopy.of(
            new InvoiceFile(
                blobs.head(InvoiceBlobs.name(studioId, invoiceId, type), InvoiceBlobs.MAX_BYTES),
                type));
    // The readers run at the same time, so Compare takes as long as the slowest one, not all of
    // them added up. Each task is bound to the caller's studio: row-level security reads it.
    TenantContext caller = TenantContext.require();
    List<Future<Compared>> goes = new ArrayList<>();
    try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
      for (InvoiceReader reader : readers.everyForBoundStudio()) {
        goes.add(
            pool.submit(() ->
                TenantContext.callAs(caller, () -> go(reader, studioId, invoiceId, file))));
      }
    }
    List<Compared> compared = new ArrayList<>();
    for (Future<Compared> go : goes) {
      compared.add(result(go));
    }
    return compared;
  }

  private Compared go(InvoiceReader reader, UUID studioId, UUID invoiceId, InvoiceFile file) {
    ReaderAvailability availability = reader.availabilityForBoundStudio();
    return availability.ready()
        ? run(reader, studioId, invoiceId, file)
        : new Compared(
            reader.kind(),
            availability,
            null,
            null,
            List.of(),
            List.of(),
            availability.reason(),
            0,
            null);
  }

  /** A finished go; the pool has closed, so every one is done. */
  private static Compared result(Future<Compared> go) {
    try {
      return go.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Compare readers was interrupted", e);
    } catch (ExecutionException e) {
      if (e.getCause() instanceof RuntimeException cause) {
        throw cause;
      }
      throw new IllegalStateException("A reader failed in Compare readers", e.getCause());
    }
  }

  private Compared run(InvoiceReader reader, UUID studioId, UUID invoiceId, InvoiceFile file) {
    long startedNanos = System.nanoTime();
    ReaderResult result = null;
    InvoiceRead.Outcome outcome;
    String note = null;
    try {
      result = reader.read(file);
      outcome = InvoiceRead.Outcome.READ;
    } catch (InvoiceReadException e) {
      InvoiceFailures.log(log, reader.kind(), invoiceId, e);
      outcome = InvoiceRead.Outcome.of(e.failure());
      note = e.getMessage();
    } catch (RuntimeException e) {
      InvoiceFailures.logUnexpected(log, reader.kind(), invoiceId, e);
      outcome = InvoiceRead.Outcome.UNAVAILABLE;
      note = InvoiceFailures.UNEXPECTED;
    }
    long elapsed = Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
    ReaderResult.Usage usage = result == null ? null : result.usage();
    InvoiceRead.Outcome ended = outcome;
    Instant now = clock.instant();
    if (result == null) {
      transactions.executeWithoutResult(tx -> {
        if (invoices.findForShare(invoiceId).isPresent()) {
          reads.save(new InvoiceRead(invoiceId, reader.kind(), ended, usage, elapsed));
        }
      });
      return new Compared(
          reader.kind(),
          ReaderAvailability.READY,
          outcome,
          null,
          List.of(),
          List.of(),
          note,
          elapsed,
          null);
    }
    InvoiceDrafts.Sanitized sanitized =
        InvoiceDrafts.sanitize(result.draft(), LocalDate.now(clock.withZone(ZoneOffset.UTC)));
    InvoiceDraft draft = sanitized.draft();
    transactions.executeWithoutResult(tx -> {
      // Reload under a shared row lock: if the invoice was discarded while reading, skip saving
      // instead of failing an FK check; if it was confirmed in the meantime, score immediately so
      // scored_at is never cleared on a confirmed invoice.
      Optional<Invoice> current = invoices.findForShare(invoiceId);
      if (current.isEmpty()) {
        return;
      }
      Invoice invoice = current.get();
      reads.save(new InvoiceRead(invoiceId, reader.kind(), ended, usage, elapsed));
      UUID comparisonId =
          queries.saveComparison(
              studioId,
              invoiceId,
              reader.kind(),
              usage == null ? null : usage.model(),
              draft,
              elapsed,
              usage == null ? null : usage.costUsd(),
              now);
      if (invoice.status() == InvoiceStatus.CONFIRMED) {
        InvoiceDraft confirmedDraft = invoice.draft();
        InvoiceScorer.Score score = InvoiceScorer.score(confirmedDraft, draft);
        InvoiceScorer.Checked checked = InvoiceScorer.checks(confirmedDraft, draft, score);
        queries.saveComparisonScore(comparisonId, score, checked, now);
      }
    });
    List<String> problems = new ArrayList<>(sanitized.notes());
    problems.addAll(InvoiceChecks.problems(draft));
    return new Compared(
        reader.kind(),
        ReaderAvailability.READY,
        outcome,
        draft,
        problems,
        InvoiceChecks.explanations(draft),
        null,
        elapsed,
        result.usage());
  }
}
