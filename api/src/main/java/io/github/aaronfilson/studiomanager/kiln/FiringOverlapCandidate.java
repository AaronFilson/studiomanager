package io.github.aaronfilson.studiomanager.kiln;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Lightweight firing metadata for log-import overlap matching without hydrating JSONB curves. */
record FiringOverlapCandidate(
    UUID id,
    FiringKind kind,
    @Nullable String cone,
    FiringView.Status status,
    Instant createdAt,
    @Nullable Instant startedAt,
    @Nullable Instant finishedAt,
    @Nullable BigDecimal peakTemp) {

  boolean hasTelemetry() {
    return peakTemp != null;
  }

  Instant effectiveStart() {
    return startedAt != null ? startedAt : createdAt;
  }

  Instant effectiveEnd() {
    if (finishedAt != null) {
      return finishedAt;
    }
    return startedAt != null ? startedAt : createdAt;
  }
}
