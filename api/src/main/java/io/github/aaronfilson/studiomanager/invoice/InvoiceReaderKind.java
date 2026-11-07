package io.github.aaronfilson.studiomanager.invoice;

/** The readers that can turn an invoice into a draft (docs/planning/20, "Invoice readers"). */
public enum InvoiceReaderKind {
  /** Claude, with structured output, on the studio's own Anthropic key. */
  CLAUDE,
  /** Google Document AI's Invoice Parser, paid by the platform (lane F1g). */
  GOOGLE_DOCUMENT_AI,
  /** Azure AI Document Intelligence's prebuilt invoice model, paid by the platform (lane F1g). */
  AZURE_DOCUMENT_INTELLIGENCE;

  /**
   * Whether the platform pays for this reader. Those need the studio owner's consent ({@code
   * allow_platform_readers}) and the admin's say-so, under a monthly page cap.
   */
  public boolean paidByPlatform() {
    return switch (this) {
      case CLAUDE -> false;
      case GOOGLE_DOCUMENT_AI -> true;
      case AZURE_DOCUMENT_INTELLIGENCE -> true;
    };
  }
}
