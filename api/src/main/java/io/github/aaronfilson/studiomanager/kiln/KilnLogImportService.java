package io.github.aaronfilson.studiomanager.kiln;

import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.studio.StudioService;
import io.github.aaronfilson.studiomanager.studio.StudioView;
import io.github.aaronfilson.studiomanager.studio.UnitSystem;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Preview-then-commit import of kiln controller firing logs ({@code POST
 * /api/v1/studios/{studioId}/kilns/{kilnId}/log-imports}).
 *
 * <p>A dry run parses the CSV, computes summary telemetry and a downsampled curve, and searches for
 * an existing firing on that kiln overlapping within ±2 hours without writing anything. Committing
 * either attaches telemetry to the chosen firing or records a new {@code COMPLETE} firing and
 * increments the kiln's {@code totalFirings} in one transaction.
 */
@Service
public class KilnLogImportService {

  static final Duration OVERLAP_WINDOW = Duration.ofHours(2);
  private static final int MATCH_SCAN_LIMIT = 100;

  private final KilnService kilns;
  private final FiringService reads;
  private final FiringRepository firings;
  private final StudioService studios;
  private final Clock clock;

  KilnLogImportService(
      KilnService kilns,
      FiringService reads,
      FiringRepository firings,
      StudioService studios,
      Clock clock) {
    this.kilns = kilns;
    this.reads = reads;
    this.firings = firings;
    this.studios = studios;
    this.clock = clock;
  }

  /** Request parameters for previewing or committing a kiln controller log import. */
  public record ImportRequest(
      String csvText,
      KilnLogSource source,
      @Nullable TemperatureUnit tempUnit,
      KilnLogParser.@Nullable ColumnMap columnMap,
      @Nullable UUID firingId,
      @Nullable FiringKind kind,
      @Nullable String cone,
      @Nullable String notes,
      boolean dryRun) {}

  /** Existing firing on the same kiln overlapping the parsed log within ±2 hours. */
  public record SuggestedMatch(
      UUID firingId,
      FiringKind kind,
      @Nullable String cone,
      FiringView.Status status,
      Instant createdAt,
      @Nullable Instant startedAt,
      @Nullable Instant finishedAt,
      String overlapDescription) {}

  /** Result returned for both preview ({@code committed=false}) and commit ({@code true}). */
  public record ImportResult(
      boolean committed,
      KilnLogSource source,
      boolean presetUnverified,
      TemperatureUnit tempUnit,
      boolean tempUnitInferred,
      @Nullable Instant startedAt,
      @Nullable Instant finishedAt,
      int durationMinutes,
      BigDecimal peakTemp,
      @Nullable String detectedCone,
      @Nullable String errorCode,
      int rowCount,
      int zoneCount,
      List<String> detectedColumns,
      List<String> warnings,
      List<TemperaturePoint> temperatureCurve,
      @Nullable SuggestedMatch suggestedMatch,
      @Nullable FiringView firing) {}

  /** Previews a firing-log import without mutating database state. */
  @Transactional(readOnly = true)
  public ImportResult preview(UUID kilnId, ImportRequest request) {
    Kiln kiln = kilns.find(kilnId);
    KilnLogParser.ParsedLog parsed = parseForStudio(request);
    UUID targetFiringId = request.firingId();
    if (targetFiringId != null) {
      Firing target = reads.find(targetFiringId);
      ensureSameKiln(target, kiln.id());
    }
    SuggestedMatch suggested = targetFiringId == null ? findOverlap(kiln.id(), parsed) : null;
    return toResult(false, parsed, suggested, null);
  }

  /** Commits a firing-log import: attaches to {@code firingId} or creates a COMPLETE firing. */
  @Transactional
  public ImportResult commit(UUID kilnId, ImportRequest request) {
    Kiln kiln = kilns.find(kilnId);
    KilnLogParser.ParsedLog parsed = parseForStudio(request);
    String curveJson = TemperatureCurveJson.write(parsed.temperatureCurve());
    Firing.Telemetry telemetry =
        new Firing.Telemetry(
            parsed.peakTemp(),
            parsed.tempUnit(),
            parsed.durationMinutes(),
            parsed.errorCode(),
            parsed.source(),
            curveJson);

    String rawCone = request.cone() != null ? request.cone() : parsed.detectedCone();
    String chosenCone = cleanCone(rawCone);
    String chosenNotes = cleanNotes(request.notes());

    Firing saved;
    UUID targetFiringId = request.firingId();
    if (targetFiringId != null) {
      Firing existing = reads.find(targetFiringId);
      ensureSameKiln(existing, kiln.id());
      existing.attachTelemetry(
          telemetry, parsed.startedAt(), parsed.finishedAt(), chosenCone, chosenNotes);
      firings.flush();
      saved = existing;
    } else {
      FiringKind reqKind = request.kind();
      FiringKind detectedKind = parsed.detectedKind();
      FiringKind kind =
          reqKind != null ? reqKind : (detectedKind != null ? detectedKind : FiringKind.BISQUE);
      Instant parsedFinished = parsed.finishedAt();
      Instant finishedAt = parsedFinished != null ? parsedFinished : clock.instant();
      Firing created =
          Firing.completedFromLog(
              kiln, kind, chosenCone, chosenNotes, parsed.startedAt(), finishedAt, telemetry);
      kiln.countFiring();
      firings.saveAndFlush(created);
      saved = created;
    }

    return toResult(true, parsed, null, reads.view(saved));
  }

  private KilnLogParser.ParsedLog parseForStudio(ImportRequest request) {
    StudioView studio = studios.current();
    TemperatureUnit studioDefault =
        studio.unitSystem() == UnitSystem.METRIC ? TemperatureUnit.C : TemperatureUnit.F;
    return KilnLogParser.parse(
        request.csvText(),
        request.source(),
        request.tempUnit(),
        request.columnMap(),
        studio.timeZone(),
        studioDefault);
  }

  private @Nullable SuggestedMatch findOverlap(UUID kilnId, KilnLogParser.ParsedLog parsed) {
    Instant logStart = parsed.startedAt();
    Instant logEnd = parsed.finishedAt();
    if (logStart == null && logEnd == null) {
      return null;
    }
    Instant effectiveStart = logStart != null ? logStart : Objects.requireNonNull(logEnd);
    Instant rawEnd = logEnd != null ? logEnd : effectiveStart;
    Instant effectiveEnd = rawEnd.isBefore(effectiveStart) ? effectiveStart : rawEnd;
    Instant windowStart = effectiveStart.minus(OVERLAP_WINDOW);
    Instant windowEnd = effectiveEnd.plus(OVERLAP_WINDOW);
    Instant logMid = midpoint(effectiveStart, effectiveEnd);

    List<Firing> candidates = firings.findNewestForKiln(kilnId, Limit.of(MATCH_SCAN_LIMIT));
    return candidates.stream()
        .filter(candidate -> overlaps(candidate, windowStart, windowEnd))
        .min(
            Comparator.comparing((Firing f) -> f.hasTelemetry())
                .thenComparing(f -> f.status() == FiringView.Status.CANCELLED)
                .thenComparingLong(f ->
                    Math.abs(Duration.between(logMid, midpoint(startOf(f), endOf(f))).toSeconds())))
        .map(match -> toSuggestedMatch(match, effectiveStart, effectiveEnd))
        .orElse(null);
  }

  private static boolean overlaps(Firing firing, Instant windowStart, Instant windowEnd) {
    Instant fStart = startOf(firing);
    Instant fEnd = endOf(firing);
    return !fEnd.isBefore(windowStart) && !fStart.isAfter(windowEnd);
  }

  private static Instant startOf(Firing firing) {
    Instant started = firing.startedAt();
    return started != null ? started : firing.createdAt();
  }

  private static Instant endOf(Firing firing) {
    Instant finished = firing.finishedAt();
    if (finished != null) {
      return finished;
    }
    Instant started = firing.startedAt();
    return started != null ? started : firing.createdAt();
  }

  private static Instant midpoint(Instant a, Instant b) {
    long halfMillis = Duration.between(a, b).toMillis() / 2L;
    return a.plusMillis(halfMillis);
  }

  private static SuggestedMatch toSuggestedMatch(Firing firing, Instant logStart, Instant logEnd) {
    Instant fStart = startOf(firing);
    Instant fEnd = endOf(firing);
    String description;
    if (!fEnd.isBefore(logStart) && !fStart.isAfter(logEnd)) {
      long startDiff = Math.abs(Duration.between(fStart, logStart).toMinutes());
      description =
          startDiff <= 5
              ? "Overlaps this log directly"
              : "Overlaps this log (started %d min apart)".formatted(startDiff);
    } else if (fEnd.isBefore(logStart)) {
      long gap = Duration.between(fEnd, logStart).toMinutes();
      description = "Ended %d min before this log started".formatted(gap);
    } else {
      long gap = Duration.between(logEnd, fStart).toMinutes();
      description = "Started %d min after this log ended".formatted(gap);
    }
    return new SuggestedMatch(
        firing.id(),
        firing.kind(),
        firing.cone(),
        firing.status(),
        firing.createdAt(),
        firing.startedAt(),
        firing.finishedAt(),
        description);
  }

  private static void ensureSameKiln(Firing firing, UUID kilnId) {
    if (!firing.kiln().id().equals(kilnId)) {
      throw new InvalidInputException("That firing belongs to a different kiln.");
    }
  }

  private static @Nullable String cleanCone(@Nullable String cone) {
    if (cone == null || cone.isBlank()) {
      return null;
    }
    String trimmed = cone.strip();
    if (trimmed.length() > 10) {
      throw new InvalidInputException("Keep the cone under 10 characters.");
    }
    return trimmed;
  }

  private static @Nullable String cleanNotes(@Nullable String notes) {
    if (notes == null || notes.isBlank()) {
      return null;
    }
    String trimmed = notes.strip();
    if (trimmed.length() > 1000) {
      throw new InvalidInputException("Keep notes under 1,000 characters.");
    }
    return trimmed;
  }

  private static ImportResult toResult(
      boolean committed,
      KilnLogParser.ParsedLog parsed,
      @Nullable SuggestedMatch suggested,
      @Nullable FiringView firing) {
    return new ImportResult(
        committed,
        parsed.source(),
        parsed.source().presetUnverified(),
        parsed.tempUnit(),
        parsed.tempUnitInferred(),
        parsed.startedAt(),
        parsed.finishedAt(),
        parsed.durationMinutes(),
        parsed.peakTemp(),
        parsed.detectedCone(),
        parsed.errorCode(),
        parsed.rowCount(),
        parsed.zoneCount(),
        parsed.detectedColumns(),
        parsed.warnings(),
        parsed.temperatureCurve(),
        suggested,
        firing);
  }
}
