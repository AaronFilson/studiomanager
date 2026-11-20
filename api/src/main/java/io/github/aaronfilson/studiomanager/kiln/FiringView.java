package io.github.aaronfilson.studiomanager.kiln;

import io.github.aaronfilson.studiomanager.piece.PieceView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** One load of a kiln, and the pieces in it. Lists leave {@code pieces} empty. */
public record FiringView(
    UUID id,
    UUID kilnId,
    String kilnName,
    FiringKind kind,
    @Nullable String cone,
    Status status,
    @Nullable String notes,
    Instant createdAt,
    @Nullable Instant startedAt,
    @Nullable Instant finishedAt,
    int pieceCount,
    long version,
    List<PieceView> pieces,
    @Nullable BigDecimal peakTemp,
    @Nullable TemperatureUnit tempUnit,
    @Nullable Integer durationMinutes,
    @Nullable String errorCode,
    @Nullable KilnLogSource logSource,
    @Nullable List<TemperaturePoint> temperatureCurve) {

  public FiringView(
      UUID id,
      UUID kilnId,
      String kilnName,
      FiringKind kind,
      @Nullable String cone,
      Status status,
      @Nullable String notes,
      Instant createdAt,
      @Nullable Instant startedAt,
      @Nullable Instant finishedAt,
      int pieceCount,
      long version,
      List<PieceView> pieces) {
    this(
        id,
        kilnId,
        kilnName,
        kind,
        cone,
        status,
        notes,
        createdAt,
        startedAt,
        finishedAt,
        pieceCount,
        version,
        pieces,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  public enum Status {
    LOADING,
    FIRING,
    COMPLETE,
    CANCELLED;

    boolean open() {
      return this == LOADING || this == FIRING;
    }
  }
}
