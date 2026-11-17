package io.github.aaronfilson.studiomanager.invoice;

import org.jspecify.annotations.Nullable;

/**
 * A reader couldn't read the invoice. The message is shown to staff, so it must never carry the
 * key, the provider's raw body, or anything the invoice said.
 *
 * <p>The cause and the detail are for the log only, so an admin can find out why a provider failed:
 * an HTTP status and the provider's own error code, or a sign-in error. Neither may hold the
 * provider's body, which can quote the invoice.
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
  private final @Nullable String detail;

  public InvoiceReadException(Failure failure, String message) {
    this(failure, message, null, null);
  }

  public InvoiceReadException(
      Failure failure, String message, @Nullable String detail, @Nullable Throwable cause) {
    super(message, cause);
    this.failure = failure;
    this.detail = detail;
  }

  public Failure failure() {
    return failure;
  }

  /** For the log: what the provider said went wrong, without anything from the invoice. */
  public @Nullable String detail() {
    return detail;
  }
}
