package io.github.aaronfilson.studiomanager.invoice;

/**
 * A reader couldn't read the invoice. The message is shown to staff, so it must never carry the
 * key, the provider's raw body, or anything the invoice said.
 */
public class InvoiceReadException extends RuntimeException {

  /** Why the read failed, which decides what happens next. */
  public enum Failure {
    /** The provider refused the credentials (401 or 403): the key is marked refused. */
    REFUSED_CREDENTIALS,
    /** The provider was down, slow or rate-limited, after the client's own retries. */
    UNAVAILABLE,
    /** The provider answered, but not with a usable invoice. */
    UNREADABLE
  }

  private final Failure failure;

  public InvoiceReadException(Failure failure, String message) {
    super(message);
    this.failure = failure;
  }

  public Failure failure() {
    return failure;
  }
}
