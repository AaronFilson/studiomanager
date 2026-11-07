package io.github.aaronfilson.studiomanager.invoice;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Amounts as an invoice prints them ("$1,234.50"), turned into plain decimals for the document
 * services' answers when they give only the printed text. Anything else becomes null: it's never
 * guessed at, and {@link InvoiceDrafts#sanitize} checks what's left.
 */
final class PrintedValues {

  /** Currency signs, spaces and thousands separators, dropped before the decimal is read. */
  private static final Pattern DECORATION = Pattern.compile("[\\s$€£,]");

  private PrintedValues() {}

  static @Nullable BigDecimal amount(@Nullable String printed) {
    if (printed == null || printed.length() > 40) {
      return null;
    }
    return ClaudeInvoiceAnswer.decimal(DECORATION.matcher(printed).replaceAll(""));
  }

  /** Text as read, or null when it's blank. */
  static @Nullable String text(@Nullable String read) {
    return read == null || read.isBlank() ? null : read.strip();
  }
}
