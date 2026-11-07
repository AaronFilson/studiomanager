package io.github.aaronfilson.studiomanager.invoice;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * A reader's answer: the draft, its raw output (kept for 90 days so a mistake can be traced), and
 * what the read used.
 *
 * @param raw the provider's own answer as JSON
 */
public record ReaderResult(InvoiceDraft draft, String raw, Usage usage) {

  /**
   * What one read used. Token counts are for the LLM readers; pages for the document services,
   * which bill by the page.
   *
   * @param costUsd the estimated cost, from configured prices; null when unknown
   */
  public record Usage(
      @Nullable String model,
      @Nullable Integer pages,
      @Nullable Long inputTokens,
      @Nullable Long outputTokens,
      @Nullable BigDecimal costUsd) {}
}
