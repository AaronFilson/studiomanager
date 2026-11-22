package io.github.aaronfilson.studiomanager.invoice;

import java.nio.charset.StandardCharsets;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight pure-Java PDF page counter to enforce the 10-page invoice cap before sending a
 * document to Claude, Azure Document Intelligence, or Google Document AI (docs/planning/16).
 *
 * <p>Checks explicit {@code /Type /Page} leaf objects (never matching {@code /Type /Pages}) and the
 * maximum {@code /Count N} on {@code /Type /Pages} tree nodes. Returns empty for compressed object
 * streams where neither marker appears in plain bytes, so unusual valid PDFs aren't falsely blocked
 * while standard multi-page PDFs above the cap are stopped immediately.
 */
final class PdfPageCounter {

  /** Maximum pages accepted for an invoice PDF (docs/planning/16, "Shape"). */
  static final int MAX_PAGES = 10;

  private static final Pattern PAGE_LEAF = Pattern.compile("/Type\\s*/Page(?![A-Za-z0-9_])");
  private static final Pattern PAGES_NODE =
      Pattern.compile("/Type\\s*/Pages(?![A-Za-z0-9_])[^<>]{0,400}/Count\\s+(\\d{1,6})");
  private static final Pattern COUNT_BEFORE_PAGES =
      Pattern.compile("/Count\\s+(\\d{1,6})[^<>]{0,400}/Type\\s*/Pages(?![A-Za-z0-9_])");

  private PdfPageCounter() {}

  /** Estimates the page count of a PDF from its raw bytes, or empty if undetermined. */
  static OptionalInt countPages(byte[] pdfBytes) {
    if (pdfBytes.length < 5) {
      return OptionalInt.empty();
    }
    String ascii = new String(pdfBytes, StandardCharsets.ISO_8859_1);
    int maxCount = 0;
    maxCount = Math.max(maxCount, maxCapturedInt(PAGES_NODE, ascii));
    maxCount = Math.max(maxCount, maxCapturedInt(COUNT_BEFORE_PAGES, ascii));

    int leafCount = 0;
    Matcher leafMatcher = PAGE_LEAF.matcher(ascii);
    while (leafMatcher.find()) {
      leafCount++;
    }

    int pages = Math.max(maxCount, leafCount);
    return pages > 0 ? OptionalInt.of(pages) : OptionalInt.empty();
  }

  private static int maxCapturedInt(Pattern pattern, String input) {
    int max = 0;
    Matcher matcher = pattern.matcher(input);
    while (matcher.find()) {
      try {
        int value = Integer.parseInt(matcher.group(1));
        if (value > max) {
          max = value;
        }
      } catch (NumberFormatException ignored) {
        // Ignore implausible counts
      }
    }
    return max;
  }
}
