package io.github.aaronfilson.studiomanager.kiln;

import io.github.aaronfilson.studiomanager.piece.PieceView;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TenantId;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * One load of a kiln, from loading to unloading or cancelling. Its status changes are methods here,
 * so the rules for which change is allowed when live with the data.
 */
// Hibernate reads and writes the mapped fields by reflection; some have no Java reader
@SuppressWarnings("UnusedVariable")
@Entity
@Table(name = "firing")
@EntityListeners(AuditingEntityListener.class)
class Firing {

  private static final int MAX_NOTES = 1000;

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "kiln_id")
  private Kiln kiln;

  @Enumerated(EnumType.STRING)
  private FiringKind kind;

  private @Nullable String cone;

  @Enumerated(EnumType.STRING)
  private FiringView.Status status;

  private @Nullable String notes;

  @CreatedBy private @Nullable UUID createdBy;

  @CreatedDate private @Nullable Instant createdAt;

  private @Nullable Instant startedAt;

  private @Nullable Instant finishedAt;

  private @Nullable BigDecimal peakTemp;

  @Enumerated(EnumType.STRING)
  private @Nullable TemperatureUnit tempUnit;

  private @Nullable Integer durationMinutes;

  private @Nullable String errorCode;

  @Enumerated(EnumType.STRING)
  private @Nullable KilnLogSource logSource;

  @JdbcTypeCode(SqlTypes.JSON)
  private @Nullable String temperatureCurve;

  @Version private @Nullable Long version;

  @OneToMany(mappedBy = "firing", cascade = CascadeType.PERSIST)
  @OrderBy("key.pieceId")
  private List<FiringPiece> pieces = new ArrayList<>();

  @SuppressWarnings("NullAway.Init")
  protected Firing() {}

  /** A new load of {@code pieceIds}, still loading. */
  Firing(Kiln kiln, FiringKind kind, @Nullable String cone, Collection<UUID> pieceIds) {
    this.id = Uuids.v7();
    this.kiln = kiln;
    this.kind = kind;
    this.cone = cone == null || cone.isBlank() ? null : cone.strip();
    this.status = FiringView.Status.LOADING;
    for (UUID pieceId : pieceIds) {
      pieces.add(new FiringPiece(this, pieceId));
    }
  }

  /** Telemetry fields persisted from a controller CSV log. */
  record Telemetry(
      BigDecimal peakTemp,
      TemperatureUnit tempUnit,
      int durationMinutes,
      @Nullable String errorCode,
      KilnLogSource logSource,
      @Nullable String temperatureCurveJson) {}

  /** A historical COMPLETE firing created directly from an imported controller log. */
  static Firing completedFromLog(
      Kiln kiln,
      FiringKind kind,
      @Nullable String cone,
      @Nullable String notes,
      @Nullable Instant startedAt,
      Instant finishedAt,
      Telemetry telemetry) {
    Firing firing = new Firing(kiln, kind, cone, List.of());
    firing.status = FiringView.Status.COMPLETE;
    firing.notes = notes;
    firing.startedAt = startedAt;
    firing.finishedAt = finishedAt;
    firing.applyTelemetry(telemetry);
    return firing;
  }

  UUID id() {
    return id;
  }

  Kiln kiln() {
    return kiln;
  }

  FiringKind kind() {
    return kind;
  }

  @Nullable String cone() {
    return cone;
  }

  FiringView.Status status() {
    return status;
  }

  long version() {
    return Objects.requireNonNull(version, "A firing has a version once it's saved");
  }

  Instant createdAt() {
    return Objects.requireNonNull(createdAt, "A firing has a creation time once it's saved");
  }

  @Nullable Instant startedAt() {
    return startedAt;
  }

  @Nullable Instant finishedAt() {
    return finishedAt;
  }

  boolean hasTelemetry() {
    return logSource != null || peakTemp != null;
  }

  /**
   * Attaches controller telemetry to this firing, filling blank timestamps or cone from the log.
   * Only firings in progress or already complete may receive telemetry.
   */
  void attachTelemetry(
      Telemetry telemetry,
      @Nullable Instant logStartedAt,
      @Nullable Instant logFinishedAt,
      @Nullable String logCone,
      @Nullable String logNotes) {
    if (this.status != FiringView.Status.FIRING && this.status != FiringView.Status.COMPLETE) {
      throw new InvalidInputException(
          "Telemetry can only be attached to a firing in progress or already complete.");
    }
    applyTelemetry(telemetry);
    if (this.startedAt == null && logStartedAt != null) {
      this.startedAt = logStartedAt;
    }
    if (this.finishedAt == null
        && logFinishedAt != null
        && this.status == FiringView.Status.COMPLETE) {
      this.finishedAt = logFinishedAt;
    }
    if (this.cone == null && logCone != null) {
      this.cone = logCone;
    }
    if (this.notes == null && logNotes != null) {
      this.notes = logNotes;
    }
  }

  private void applyTelemetry(Telemetry telemetry) {
    this.peakTemp = telemetry.peakTemp();
    this.tempUnit = telemetry.tempUnit();
    this.durationMinutes = telemetry.durationMinutes();
    this.errorCode = telemetry.errorCode();
    this.logSource = telemetry.logSource();
    this.temperatureCurve = telemetry.temperatureCurveJson();
  }

  /** The pieces in the load, ordered by id. Loads its piece rows if they aren't loaded yet. */
  List<UUID> pieceIds() {
    return pieces.stream().map(FiringPiece::pieceId).toList();
  }

  /** Switches the kiln on. Only a load that's still loading can start. */
  void start(Instant now) {
    if (status != FiringView.Status.LOADING) {
      throw new InvalidInputException("Only a load that's still loading can be started.");
    }
    status = FiringView.Status.FIRING;
    startedAt = now;
  }

  /** Unloads a firing in progress. Its pieces leave the load, free to be loaded again later. */
  void complete(@Nullable String unloadNotes, Instant now) {
    if (status != FiringView.Status.FIRING) {
      throw new InvalidInputException("Only a firing in progress can be unloaded.");
    }
    finish(FiringView.Status.COMPLETE, unloadNotes, now);
  }

  /** Abandons a load before or during firing; its pieces go back to the queue unchanged. */
  void cancel(@Nullable String cancelNotes, Instant now) {
    if (!status.open()) {
      throw new InvalidInputException("This firing is already finished.");
    }
    finish(FiringView.Status.CANCELLED, cancelNotes, now);
  }

  private void finish(FiringView.Status finalStatus, @Nullable String finishNotes, Instant now) {
    String cleanedNotes = finishNotes == null || finishNotes.isBlank() ? null : finishNotes.strip();
    if (cleanedNotes != null && cleanedNotes.length() > MAX_NOTES) {
      throw new InvalidInputException("Keep notes under 1,000 characters.");
    }
    status = finalStatus;
    finishedAt = now;
    if (cleanedNotes != null) {
      notes = cleanedNotes;
    }
    pieces.forEach(FiringPiece::close);
  }

  /**
   * The view the API returns, with the full stored temperature curve (at most 250 points).
   *
   * @param pieceCount how many pieces are in the load, counted by the caller so lists stay at a
   *     fixed number of queries
   * @param loaded the pieces themselves, or none for a list
   */
  FiringView toView(int pieceCount, List<PieceView> loaded) {
    return toView(pieceCount, loaded, TemperatureCurveJson.read(temperatureCurve));
  }

  /**
   * List view with the temperature curve downsampled to at most {@code maxCurvePoints} points
   * (preserving start, peak, and end) so inline sparklines stay lightweight.
   */
  FiringView toListView(int pieceCount, int maxCurvePoints) {
    List<TemperaturePoint> full = TemperatureCurveJson.read(temperatureCurve);
    List<TemperaturePoint> curve =
        full == null ? null : KilnLogParser.downsample(full, maxCurvePoints);
    return toView(pieceCount, List.of(), curve);
  }

  private FiringView toView(
      int pieceCount, List<PieceView> loaded, @Nullable List<TemperaturePoint> curve) {
    return new FiringView(
        id,
        kiln.id(),
        kiln.name(),
        kind,
        cone,
        status,
        notes,
        Objects.requireNonNull(createdAt, "A firing has a creation time once it's saved"),
        startedAt,
        finishedAt,
        pieceCount,
        version(),
        loaded,
        peakTemp,
        tempUnit,
        durationMinutes,
        errorCode,
        logSource,
        curve);
  }
}
