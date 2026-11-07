package io.github.aaronfilson.studiomanager.invoice;

import io.github.aaronfilson.studiomanager.platform.Capped;
import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.storage.BlobStore;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The supplies inbox: staff start an upload, the browser puts the file straight into storage (ADR
 * 12), and completing the upload checks the file and hands it to a reader.
 *
 * <p>Each call takes the studio from the path and checks it's the bound one, so the blob names it
 * builds can only ever be the bound studio's.
 */
@Service
class InvoiceInbox {

  static final int LIST_LIMIT = 100;

  private final InvoiceRepository invoices;
  private final BlobStore blobs;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  InvoiceInbox(
      InvoiceRepository invoices, BlobStore blobs, ApplicationEventPublisher events, Clock clock) {
    this.invoices = invoices;
    this.blobs = blobs;
    this.events = events;
    this.clock = clock;
  }

  /** Where to put the file, until when, and how big it may be. */
  record Upload(UUID invoiceId, URI uploadUrl, Instant expiresAt, int maxBytes) {}

  /** An invoice with a short-lived link to its file, for the review screen. */
  record Opened(InvoiceView invoice, URI fileUrl) {}

  @Transactional
  Upload startUpload(UUID studioId, InvoiceFileType type) {
    requireBound(studioId);
    Invoice invoice = invoices.saveAndFlush(new Invoice(type));
    Instant expiresAt = clock.instant().plus(InvoiceBlobs.UPLOAD_WINDOW);
    return new Upload(
        invoice.id(),
        blobs.uploadUrl(InvoiceBlobs.name(studioId, invoice.id(), type), expiresAt),
        expiresAt,
        InvoiceBlobs.MAX_BYTES);
  }

  /**
   * Checks the file arrived, is within the size limit, and is the kind it said it was; then hands
   * it to a reader. Completing it twice is harmless.
   */
  @Transactional
  InvoiceView completeUpload(UUID studioId, UUID invoiceId) {
    requireBound(studioId);
    Invoice invoice = find(invoiceId);
    switch (invoice.status()) {
      case UPLOADING -> {
        String blob = InvoiceBlobs.name(studioId, invoiceId, invoice.fileType());
        BlobStore.BlobInfo info =
            blobs
                .info(blob)
                .orElseThrow(() -> new InvalidInputException("The file hasn't been uploaded yet."));
        if (info.size() > InvoiceBlobs.MAX_BYTES) {
          throw new InvalidInputException(
              "The file is larger than %d MB.".formatted(InvoiceBlobs.MAX_BYTES / 1024 / 1024));
        }
        if (!invoice.fileType().startsLike(blobs.head(blob, InvoiceFileType.magicLength()))) {
          throw new InvalidInputException(
              "The file isn't the %s it was started as.".formatted(invoice.fileType().name()));
        }
        invoice.uploaded(info.size());
        // Written before the event: the reader loads it in a transaction of its own
        invoices.flush();
        events.publishEvent(new InvoiceReceived(invoiceId, studioId));
      }
      case READING, READY, NEEDS_LOOK, CONFIRMED -> {
        // Already in: nothing to do
      }
    }
    return invoice.toView(InvoiceChecks.problems(invoice.draft()));
  }

  /** The inbox, newest first. */
  @Transactional(readOnly = true)
  Capped<InvoiceView.Summary> list(UUID studioId) {
    requireBound(studioId);
    return Capped.fromFetched(
            invoices.findNewestFirst(Limit.of(Capped.fetchSize(LIST_LIMIT))), LIST_LIMIT)
        .map(Invoice::toSummary);
  }

  @Transactional(readOnly = true)
  Opened open(UUID studioId, UUID invoiceId) {
    requireBound(studioId);
    Invoice invoice = find(invoiceId);
    return new Opened(
        invoice.toView(InvoiceChecks.problems(invoice.draft())),
        blobs.readUrl(
            InvoiceBlobs.name(studioId, invoiceId, invoice.fileType()),
            invoice.fileType().mediaType()));
  }

  /** Throws away an invoice that hasn't been confirmed, and its file. */
  @Transactional
  void discard(UUID studioId, UUID invoiceId) {
    requireBound(studioId);
    Invoice invoice = find(invoiceId);
    switch (invoice.status()) {
      case UPLOADING, READING, READY, NEEDS_LOOK -> {
        invoices.delete(invoice);
        invoices.flush();
        blobs.delete(InvoiceBlobs.name(studioId, invoiceId, invoice.fileType()));
      }
      case CONFIRMED ->
          throw new ConflictException(
              "A confirmed invoice is kept: its stock has already been received.");
    }
  }

  private Invoice find(UUID invoiceId) {
    return invoices
        .findWithLines(invoiceId)
        .orElseThrow(() -> new NotFoundException("No such invoice."));
  }

  /** The path's studio is the bound one; anything else is a bug or a probe, and finds nothing. */
  static void requireBound(UUID studioId) {
    if (!studioId.equals(TenantContext.require().requireStudioId())) {
      throw new NotFoundException("No such invoice.");
    }
  }
}
