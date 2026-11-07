package io.github.aaronfilson.studiomanager.maintenance;

import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import org.jspecify.annotations.Nullable;

/** Checks on text people type, and the module's conflict message, shared by its services. */
final class MaintenanceText {

  private MaintenanceText() {}

  /** Blank becomes null; anything else is stripped and must fit in {@code max} characters. */
  static @Nullable String optional(@Nullable String value, int max, String what) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String cleaned = value.strip();
    if (cleaned.length() > max) {
      throw new InvalidInputException("Keep the %s to %d characters.".formatted(what, max));
    }
    return cleaned;
  }

  static ConflictException staleVersion() {
    return ConflictException.staleVersion(
        "Someone else changed this at the same time. Reload and try again.");
  }

  static ConflictException staleVersionOfReport() {
    return ConflictException.staleVersion(
        "Someone else changed or resolved this report. Reload and try again.");
  }
}
