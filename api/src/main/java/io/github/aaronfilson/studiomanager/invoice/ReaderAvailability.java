package io.github.aaronfilson.studiomanager.invoice;

/**
 * Whether a reader may read for the bound studio now and, when it may not, why, in words safe to
 * show the studio's owner. "Compare readers" shows every reader with this, so a reader that can't
 * run says so instead of disappearing (docs/planning/24, "Compare readers").
 */
public enum ReaderAvailability {
  READY(""),
  NO_KEY(
      "The studio has no Anthropic key. The owner can add one in Settings, under AI for invoices."),
  KEY_REFUSED("Anthropic refused the studio's key. The owner can replace it in Settings."),
  NOT_SET_UP("This reader isn't set up on this server."),
  NOT_ON_FOR_STUDIO("The platform admin hasn't turned this reader on for this studio."),
  NO_CONSENT(
      "The owner hasn't allowed the platform's readers. That's in Settings, under AI for invoices."),
  STUDIO_PAGES_USED_UP("This studio has used this month's pages for this reader."),
  PLATFORM_PAGES_USED_UP("This month's pages for this reader are used up across the platform.");

  private final String reason;

  ReaderAvailability(String reason) {
    this.reason = reason;
  }

  /** Why the reader can't read; empty when it can. */
  public String reason() {
    return reason;
  }

  public boolean ready() {
    return this == READY;
  }
}
