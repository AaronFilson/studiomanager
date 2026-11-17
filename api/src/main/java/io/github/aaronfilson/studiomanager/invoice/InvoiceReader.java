package io.github.aaronfilson.studiomanager.invoice;

/**
 * Turns an invoice file into a draft. One implementation per provider (Claude here; Google Document
 * AI and Azure AI Document Intelligence in lane F1g), each mapping its own output into the same
 * {@link InvoiceDraft}, so the checks, the matcher and the review are shared (ADR 18).
 *
 * <p>Implementations are Spring beans. {@link InvoiceReaders} asks each, in order, whether it can
 * read for the bound studio, and uses the first that can.
 */
public interface InvoiceReader {

  InvoiceReaderKind kind();

  /**
   * Whether this reader may read for the bound studio right now, and if not, why: for Claude, the
   * studio has a key Anthropic hasn't refused; for the platform's readers, it's set up on this
   * server, the admin has turned it on for the studio, the owner has consented, and the month's
   * pages aren't used up. Called with the studio bound and outside any transaction.
   */
  ReaderAvailability availabilityForBoundStudio();

  default boolean canReadForBoundStudio() {
    return availabilityForBoundStudio().ready();
  }

  /** Whether this server has what the reader needs, whatever any studio's rules say. */
  boolean setUp();

  /**
   * Reads one invoice. Called with the studio bound and outside any transaction, so no database
   * connection is held while the provider works.
   *
   * @throws InvoiceReadException when it can't, with a message safe to show staff
   */
  ReaderResult read(InvoiceFile file);
}
