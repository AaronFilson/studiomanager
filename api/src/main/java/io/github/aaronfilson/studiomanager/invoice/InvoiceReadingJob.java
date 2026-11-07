package io.github.aaronfilson.studiomanager.invoice;

import io.github.aaronfilson.studiomanager.platform.storage.BlobStore;
import io.github.aaronfilson.studiomanager.platform.tenancy.SystemActor;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Reads an invoice once its file is in, off the request thread, as the invoice reader's own system
 * account (ADR 16) bound to the invoice's studio.
 *
 * <p>Three steps, so no database connection is held while a provider works: load what to read, read
 * it with no transaction open, then save the draft and the run. Whatever happens, the invoice ends
 * up as a draft: read, or empty with the reason, for staff to fill in next to the image.
 *
 * <p>Not an {@code @ApplicationModuleListener}: that would open a transaction before the studio is
 * bound, and row-level security and Hibernate's tenant are read when a transaction begins. The
 * outbox still records it, as it does any {@code @TransactionalEventListener}.
 */
@Component
class InvoiceReadingJob {

  static final int NOTE_LENGTH = 300;

  private final InvoiceRepository invoices;
  private final InvoiceReadRepository reads;
  private final InvoiceReaders readers;
  private final InvoiceMatching matching;
  private final BlobStore blobs;
  private final TransactionOperations transactions;
  private final Clock clock;

  InvoiceReadingJob(
      InvoiceRepository invoices,
      InvoiceReadRepository reads,
      InvoiceReaders readers,
      InvoiceMatching matching,
      BlobStore blobs,
      TransactionOperations transactions,
      Clock clock) {
    this.invoices = invoices;
    this.reads = reads;
    this.readers = readers;
    this.matching = matching;
    this.blobs = blobs;
    this.transactions = transactions;
    this.clock = clock;
  }

  @Async
  @TransactionalEventListener
  void on(InvoiceReceived received) {
    TenantContext.callAs(
        TenantContext.system(SystemActor.INVOICE_READER, received.studioId()),
        () -> {
          read(received.studioId(), received.invoiceId());
          return null;
        });
  }

  /** Reads one invoice of the bound studio, if it's still waiting to be read. */
  void read(UUID studioId, UUID invoiceId) {
    InvoiceFileType type =
        transactions.execute(tx ->
            invoices
                .findById(invoiceId)
                .filter(invoice -> invoice.status() == InvoiceStatus.READING)
                .map(Invoice::fileType)
                .orElse(null));
    if (type == null) {
      // Discarded, or read already
      return;
    }
    Optional<InvoiceReader> chosen = readers.forBoundStudio();
    if (chosen.isEmpty()) {
      save(
          invoiceId,
          null,
          null,
          "No reader is set up for this studio, so fill this one in. The owner can add an Anthropic key in Settings.");
      return;
    }
    InvoiceReader reader = chosen.get();
    byte[] content =
        blobs.head(InvoiceBlobs.name(studioId, invoiceId, type), InvoiceBlobs.MAX_BYTES);
    long started = clock.millis();
    ReaderResult result = null;
    InvoiceRead.Outcome outcome;
    String note = null;
    try {
      result = reader.read(new InvoiceFile(content, type));
      outcome = InvoiceRead.Outcome.READ;
    } catch (InvoiceReadException e) {
      outcome = outcomeOf(e.failure());
      note = e.getMessage();
    }
    long elapsed = clock.millis() - started;
    ReaderResult answered = result;
    String reason = note;
    InvoiceRead.Outcome ended = outcome;
    transactions.executeWithoutResult(tx -> {
      reads.save(
          new InvoiceRead(
              invoiceId,
              reader.kind(),
              ended,
              answered == null ? null : answered.usage(),
              elapsed));
      save(invoiceId, reader.kind(), answered, reason);
    });
  }

  /** Saves the draft (read, or empty with the reason), if the invoice is still waiting for it. */
  private void save(
      UUID invoiceId,
      @Nullable InvoiceReaderKind reader,
      @Nullable ReaderResult result,
      @Nullable String note) {
    transactions.executeWithoutResult(tx -> {
      InvoiceDraft draft;
      List<String> notes = new ArrayList<>();
      if (note != null) {
        notes.add(note);
      }
      if (result == null) {
        draft = InvoiceDraft.empty();
      } else {
        InvoiceDrafts.Sanitized sanitized =
            InvoiceDrafts.sanitize(result.draft(), LocalDate.now(clock.withZone(ZoneOffset.UTC)));
        draft = sanitized.draft();
        notes.addAll(sanitized.notes());
      }
      // SQL first: matching reads supplies and aliases before any entity changes (ADR 21, rule 5)
      List<Invoice.LineMatch> matches = matching.matchAll(draft);
      invoices
          .findWithLines(invoiceId)
          .filter(invoice -> invoice.status() == InvoiceStatus.READING)
          .ifPresent(invoice -> {
            invoice.filled(
                draft,
                matches,
                result == null ? null : reader,
                result == null ? null : result.raw(),
                notes.isEmpty() ? null : shortened(String.join(" ", notes)));
            invoices.flush();
          });
    });
  }

  private static InvoiceRead.Outcome outcomeOf(InvoiceReadException.Failure failure) {
    return switch (failure) {
      case REFUSED_CREDENTIALS -> InvoiceRead.Outcome.REFUSED_CREDENTIALS;
      case UNAVAILABLE -> InvoiceRead.Outcome.UNAVAILABLE;
      case UNREADABLE -> InvoiceRead.Outcome.UNREADABLE;
    };
  }

  private static String shortened(String text) {
    return text.length() <= NOTE_LENGTH ? text : text.substring(0, NOTE_LENGTH - 1) + "…";
  }
}
