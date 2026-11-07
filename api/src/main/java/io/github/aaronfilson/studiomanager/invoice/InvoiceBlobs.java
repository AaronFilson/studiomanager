package io.github.aaronfilson.studiomanager.invoice;

import java.time.Duration;
import java.util.UUID;

/** Where an invoice's file lives in storage, and how big it may be (ADR 12). */
final class InvoiceBlobs {

  /** 10 MB: a phone photo or a supplier's PDF, with room to spare (docs/planning/16). */
  static final int MAX_BYTES = 10 * 1024 * 1024;

  static final Duration UPLOAD_WINDOW = Duration.ofMinutes(15);

  private InvoiceBlobs() {}

  /** Names come from ids, never from a client, under a prefix of their own. */
  static String name(UUID studioId, UUID invoiceId, InvoiceFileType type) {
    return "invoices/%s/%s/invoice.%s".formatted(studioId, invoiceId, extension(type));
  }

  private static String extension(InvoiceFileType type) {
    return switch (type) {
      case PDF -> "pdf";
      case JPEG -> "jpg";
      case PNG -> "png";
    };
  }
}
