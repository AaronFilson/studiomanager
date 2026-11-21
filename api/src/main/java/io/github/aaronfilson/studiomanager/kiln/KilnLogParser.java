package io.github.aaronfilson.studiomanager.kiln;

import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Pure Java tolerant parser for kiln controller firing logs (Bartlett Genesis, Orton/TAP, and
 * generic CSV/TSV/semicolon exports).
 *
 * <p>Scans up to the first 20 non-blank lines to locate the header row (extracting preamble
 * metadata such as firing date, cone, or error code), maps columns case-insensitively with
 * controller presets and optional user overrides, infers °F vs °C when omitted, downsamples the
 * temperature curve to at most 250 points while preserving the start, peak, and end, and enforces
 * strict untrusted-input limits (2 MB text, 50,000 rows).
 */
public final class KilnLogParser {

  /** Maximum accepted raw CSV size in UTF-8 bytes (2 MB). */
  public static final int MAX_CSV_BYTES = 2 * 1024 * 1024;

  /** Maximum accepted data rows after the header. */
  public static final int MAX_DATA_ROWS = 50_000;

  /** Maximum points retained in the stored downsampled temperature curve. */
  public static final int MAX_CURVE_POINTS = 250;

  /** Maximum non-blank leading lines scanned to locate the header row. */
  public static final int MAX_HEADER_SCAN_LINES = 20;

  /** Maximum stored length for a controller error code (matches DB check constraint). */
  public static final int MAX_ERROR_CODE_LENGTH = 40;

  /** Minimum accepted temperature reading in either °F or °C. */
  public static final double MIN_TEMP = 0.0;

  /** Maximum accepted temperature reading in either °F or °C (fits numeric(7,2) safely). */
  public static final double MAX_TEMP = 9999.9;

  /** Maximum accepted firing duration in minutes (1 year). */
  public static final int MAX_DURATION_MINUTES = 525_600;

  /** Maximum number of thermocouple zone columns accepted per log. */
  public static final int MAX_ZONES = 16;

  private static final int MAX_NON_BLANK_LINES = MAX_DATA_ROWS + MAX_HEADER_SCAN_LINES + 100;
  private static final double MIDNIGHT_ROLLOVER_THRESHOLD_MINUTES = 360.0;
  private static final int MAX_CONE_LENGTH = 10;
  private static final int MAX_HEADER_LENGTH = 80;
  private static final int MAX_SKIP_WARNINGS = 3;
  private static final double PEAK_FAHRENHEIT_THRESHOLD = 1400.0;
  private static final double AMBIENT_CELSIUS_MAX = 42.0;
  private static final double CELSIUS_PEAK_MIN = 500.0;

  private static final char[] CANDIDATE_DELIMITERS = {',', ';', '\t'};

  private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}]+");
  private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
  private static final Pattern NON_CHAMBER_TEMP =
      Pattern.compile(
          "(?:^|[^a-z])(?:board|ambient|case|cj|cold|room|offset|setpoint|set\\s+point|target|"
              + "status|alarm|error|ramp|hold)(?:$|[^a-z])");
  private static final Pattern CONE_IN_TEXT = Pattern.compile("(?i)\\bcone\\s*(0?\\d{1,2})\\b");
  private static final Pattern CONE_VALUE = Pattern.compile("^(0?\\d{1,2})$");
  private static final Pattern HEADER_FAHRENHEIT =
      Pattern.compile("(?i)(°\\s*f\\b|\\(f\\)|\\[f\\]|deg\\s*f\\b|fahrenheit|_f$|\\bf$)");
  private static final Pattern HEADER_CELSIUS =
      Pattern.compile("(?i)(°\\s*c\\b|\\(c\\)|\\[c\\]|deg\\s*c\\b|celsius|_c$|\\bc$)");
  private static final Pattern TRAILING_UNIT =
      Pattern.compile("\\s*(?:\\([^)]*\\)|\\[[^\\]]*\\]|°\\s*[fcFC]|%)\\s*$");
  private static final Pattern NUMBER_WITH_UNIT =
      Pattern.compile("^([+-]?\\d+(?:\\.\\d+)?)\\s*(?:°\\s*)?([fcFC])?$");
  private static final Pattern CLOCK_OR_ELAPSED =
      Pattern.compile("^(\\d{1,4}):(\\d{1,2})(?::(\\d{1,2})(?:\\.\\d+)?)?$");
  private static final Pattern ERROR_EVENT_HINT =
      Pattern.compile(
          "(?i)(\\bE-?\\d+[A-Z]?\\b|\\bERR(?:OR)?[-_\\s]?\\w*\\b|\\bFTH\\b|\\bFTL\\b|\\bTCO\\b|"
              + "\\bFAIL(?:ED|URE)?\\b|\\bFAULT\\b|\\bALARM\\b|\\bABORT(?:ED)?\\b|"
              + "\\bOVERHEAT\\b|\\bTHERMOCOUPLE\\b|\\bOPEN\\s*TC\\b|\\bPOWER\\s*(?:FAIL|OUT)\\b|"
              + "\\bPF\\b)");

  private static final Set<String> BENIGN_STATUS_VALUES =
      Set.of(
          "",
          "-",
          "--",
          "none",
          "ok",
          "normal",
          "0",
          "complete",
          "completed",
          "cpl",
          "done",
          "firing",
          "ramp",
          "hold",
          "soak",
          "cool",
          "cooling",
          "idle",
          "standby",
          "start",
          "started",
          "end",
          "ended",
          "firing start",
          "firing complete",
          "firing ended",
          "segment complete",
          "program complete");

  private static final Set<String> GENESIS_TIME_ALIASES =
      Set.of(
          "elapsed",
          "elapsed time",
          "elapsed_time",
          "time",
          "timestamp",
          "firing time",
          "run time",
          "runtime",
          "minutes",
          "min",
          "seconds",
          "sec",
          "sample time",
          "date/time",
          "datetime");

  private static final Set<String> TAP_TIME_ALIASES =
      Set.of(
          "timestamp",
          "date/time",
          "datetime",
          "time",
          "minute",
          "minutes",
          "elapsed",
          "elapsed min",
          "elapsed minutes",
          "elapsed time",
          "min",
          "seconds",
          "sec");

  private static final Set<String> ZONE_TEMP_ALIASES =
      Set.of(
          "tc1",
          "tc2",
          "tc3",
          "tc4",
          "tc 1",
          "tc 2",
          "tc 3",
          "tc 4",
          "tc1 temp",
          "tc2 temp",
          "tc3 temp",
          "tc4 temp",
          "zone 1",
          "zone 2",
          "zone 3",
          "zone 4",
          "zone1",
          "zone2",
          "zone3",
          "zone4",
          "z1 temp",
          "z2 temp",
          "z3 temp",
          "z4 temp",
          "z1",
          "z2",
          "z3",
          "z4",
          "top",
          "middle",
          "mid",
          "bottom",
          "bot",
          "top temp",
          "middle temp",
          "mid temp",
          "bottom temp",
          "temp 1",
          "temp 2",
          "temp 3",
          "temp1",
          "temp2",
          "temp3");

  private static final Set<String> SINGLE_TEMP_ALIASES =
      Set.of(
          "temp",
          "temperature",
          "kiln temp",
          "kiln temperature",
          "actual",
          "actual temp",
          "actual temperature",
          "chamber temp",
          "chamber temperature",
          "current temp",
          "current temperature",
          "pv",
          "reading");

  private static final Set<String> SETPOINT_ALIASES =
      Set.of(
          "setpoint",
          "set point",
          "set_point",
          "sp",
          "target",
          "target temp",
          "target temperature",
          "set temp",
          "set temperature",
          "program temp",
          "sv");

  private static final Set<String> SEGMENT_ALIASES =
      Set.of("segment", "seg", "step", "ramp", "stage", "phase");

  private static final Set<String> OUTPUT_ALIASES =
      Set.of(
          "out1",
          "out2",
          "out3",
          "out4",
          "out 1",
          "out 2",
          "out 3",
          "out 4",
          "out",
          "output",
          "output %",
          "% output",
          "power",
          "power %",
          "duty",
          "duty %",
          "duty cycle",
          "on-time",
          "on time",
          "z1 out",
          "z2 out",
          "z3 out",
          "zone 1 out",
          "zone 2 out",
          "zone 3 out",
          "relay",
          "relay %");

  private static final Set<String> DEDICATED_ERROR_ALIASES =
      Set.of("error", "error code", "error_code", "err", "fault", "alarm", "alert");

  private static final Set<String> EVENT_STATUS_ALIASES =
      Set.of("event", "events", "status", "message", "log", "note", "notes");

  private static final List<DateTimeFormatter> LOCAL_DATE_TIME_FORMATS =
      List.of(
          DateTimeFormatter.ISO_LOCAL_DATE_TIME,
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT),
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT),
          DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss", Locale.ROOT),
          DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale.ROOT),
          DateTimeFormatter.ofPattern("M/d/yyyy H:mm:ss", Locale.ROOT),
          DateTimeFormatter.ofPattern("M/d/yyyy H:mm", Locale.ROOT),
          DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a", Locale.US),
          DateTimeFormatter.ofPattern("M/d/yyyy h:mm a", Locale.US),
          DateTimeFormatter.ofPattern("M-d-yyyy H:mm:ss", Locale.ROOT),
          DateTimeFormatter.ofPattern("M-d-yyyy H:mm", Locale.ROOT));

  private static final List<DateTimeFormatter> LOCAL_DATE_FORMATS =
      List.of(
          DateTimeFormatter.ISO_LOCAL_DATE,
          DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ROOT),
          DateTimeFormatter.ofPattern("M/d/yyyy", Locale.ROOT),
          DateTimeFormatter.ofPattern("M-d-yyyy", Locale.ROOT));

  private KilnLogParser() {}

  /** Optional user-supplied header names for overriding preset column detection. */
  public record ColumnMap(
      @Nullable String time,
      @Nullable String temperature,
      @Nullable String setpoint,
      @Nullable String segment,
      @Nullable String output,
      @Nullable String error) {

    public static ColumnMap empty() {
      return new ColumnMap(null, null, null, null, null, null);
    }

    boolean hasAny() {
      return notBlank(time)
          || notBlank(temperature)
          || notBlank(setpoint)
          || notBlank(segment)
          || notBlank(output)
          || notBlank(error);
    }
  }

  /** Summary and downsampled curve extracted from one controller firing log. */
  public record ParsedLog(
      KilnLogSource source,
      TemperatureUnit tempUnit,
      boolean tempUnitInferred,
      @Nullable Instant startedAt,
      @Nullable Instant finishedAt,
      int durationMinutes,
      BigDecimal peakTemp,
      @Nullable String detectedCone,
      @Nullable FiringKind detectedKind,
      @Nullable String errorCode,
      int rowCount,
      int zoneCount,
      List<String> detectedColumns,
      List<String> warnings,
      List<TemperaturePoint> temperatureCurve) {}

  /**
   * Parses {@code csvText} into firing telemetry and a downsampled curve.
   *
   * @param csvText raw CSV/semicolon/TSV controller log text (at most {@value #MAX_CSV_BYTES}
   *     bytes)
   * @param source controller family preset
   * @param unitOverride explicit °F/°C selection, or null to infer
   * @param columnMap optional column-header overrides
   * @param studioZone studio's time zone for timestamps without an offset
   * @param studioDefaultUnit fallback temperature unit from the studio's unit system
   */
  public static ParsedLog parse(
      String csvText,
      KilnLogSource source,
      @Nullable TemperatureUnit unitOverride,
      @Nullable ColumnMap columnMap,
      ZoneId studioZone,
      @Nullable TemperatureUnit studioDefaultUnit) {
    if (csvText.isBlank()) {
      throw new InvalidInputException("Kiln log file is empty.");
    }
    if (csvText.indexOf('\0') >= 0) {
      throw new InvalidInputException("Kiln log must be a plain-text CSV, not a binary file.");
    }
    if (csvText.getBytes(StandardCharsets.UTF_8).length > MAX_CSV_BYTES) {
      throw new InvalidInputException("Kiln log exceeds the 2 MB size limit.");
    }

    String cleanText = csvText.startsWith("\uFEFF") ? csvText.substring(1) : csvText;
    List<String> nonBlankLines = splitNonBlankLines(cleanText);
    if (nonBlankLines.isEmpty()) {
      throw new InvalidInputException("Kiln log file is empty.");
    }

    ColumnMap overrides = columnMap == null ? ColumnMap.empty() : columnMap;
    HeaderMatch header = locateHeader(nonBlankLines, source, overrides);

    List<String> preambleLines = nonBlankLines.subList(0, header.lineIndex());
    Preamble preamble = parsePreamble(preambleLines, header.delimiter(), studioZone);

    int dataStart = header.lineIndex() + 1;
    int rawDataCount = nonBlankLines.size() - dataStart;
    if (rawDataCount > MAX_DATA_ROWS) {
      throw new InvalidInputException(
          "Kiln log exceeds the %,d-row limit.".formatted(MAX_DATA_ROWS));
    }
    if (rawDataCount <= 0) {
      throw new InvalidInputException("Kiln log has a header row but no temperature data rows.");
    }

    ColumnBinding binding = header.binding();
    if (binding.tempIndices().size() > MAX_ZONES) {
      throw new InvalidInputException(
          "Kiln log exceeds the maximum of %d temperature zone columns.".formatted(MAX_ZONES));
    }
    List<String> headers = header.headers();
    List<String> warnings = new ArrayList<>();

    TimeUnitKind headerTimeUnit =
        binding.timeIndex() >= 0
            ? detectTimeUnit(headers.get(binding.timeIndex()))
            : TimeUnitKind.MINUTES;
    boolean headerImpliesClock =
        binding.timeIndex() >= 0 && looksLikeClockHeader(headers.get(binding.timeIndex()));
    boolean headerImpliesElapsed =
        binding.timeIndex() >= 0 && looksLikeElapsedHeader(headers.get(binding.timeIndex()));
    TemperatureUnit headerUnitHint = detectHeaderUnit(headers, binding);
    double defaultStepMinutes = source == KilnLogSource.GENESIS ? 0.5 : 1.0;

    if (binding.timeIndex() < 0) {
      warnings.add(
          "No time column was found; assuming %.1f-minute steps between rows."
              .formatted(defaultStepMinutes));
    }

    List<RawSample> rawSamples = new ArrayList<>(Math.min(rawDataCount, 2048));
    @Nullable TemperatureUnit cellUnitHint = null;
    @Nullable String rowErrorCode = null;
    int skippedRows = 0;

    for (int i = dataStart; i < nonBlankLines.size(); i++) {
      List<String> cells = parseCsvRow(nonBlankLines.get(i), header.delimiter());
      List<Double> zoneTemps = new ArrayList<>(binding.tempIndices().size());
      for (int idx : binding.tempIndices()) {
        if (idx < cells.size()) {
          ParsedNumber parsed = parseNumberWithUnit(cells.get(idx));
          if (parsed != null) {
            if (parsed.value() < MIN_TEMP || parsed.value() > MAX_TEMP) {
              throw new InvalidInputException(
                  "Temperature reading %.1f on row %d is out of range (must be between 0 and"
                      + " 9,999.9)."
                          .formatted(parsed.value(), i + 1));
            }
            double temp = round1(parsed.value());
            zoneTemps.add(temp);
            if (cellUnitHint == null && parsed.unit() != null) {
              cellUnitHint = parsed.unit();
            }
          }
        }
      }
      if (zoneTemps.isEmpty()) {
        skippedRows++;
        if (skippedRows <= MAX_SKIP_WARNINGS) {
          warnings.add("Skipped row %d: missing temperature reading.".formatted(i + 1));
        }
        continue;
      }

      double avgTemp = round1(average(zoneTemps));

      @Nullable Double setpoint = null;
      if (binding.setpointIndex() >= 0 && binding.setpointIndex() < cells.size()) {
        ParsedNumber parsedSp = parseNumberWithUnit(cells.get(binding.setpointIndex()));
        if (parsedSp != null) {
          if (parsedSp.value() < MIN_TEMP || parsedSp.value() > MAX_TEMP) {
            throw new InvalidInputException(
                "Setpoint reading %.1f on row %d is out of range (must be between 0 and 9,999.9)."
                    .formatted(parsedSp.value(), i + 1));
          }
          setpoint = round1(parsedSp.value());
          if (cellUnitHint == null && parsedSp.unit() != null) {
            cellUnitHint = parsedSp.unit();
          }
        }
      }

      if (rowErrorCode == null
          && binding.errorIndex() >= 0
          && binding.errorIndex() < cells.size()) {
        String extracted = extractError(cells.get(binding.errorIndex()), binding.errorDedicated());
        if (extracted != null) {
          rowErrorCode = extracted;
        }
      }

      @Nullable Instant instant = null;
      @Nullable ParsedClock clockVal = null;
      boolean unparseableTime = false;
      if (binding.timeIndex() >= 0 && binding.timeIndex() < cells.size()) {
        String rawTime = sanitize(cells.get(binding.timeIndex()), 80);
        instant = parseInstant(rawTime, studioZone);
        if (instant == null) {
          clockVal = parseClockOrElapsed(rawTime, headerTimeUnit, headerImpliesClock);
          if (clockVal == null) {
            unparseableTime = true;
          }
        }
      }

      // Preserve zone alignment: if a multi-zone row has any missing zone cell, omit zoneTemps
      // rather than shifting a later zone into an earlier slot.
      @Nullable List<Double> zones =
          binding.tempIndices().size() > 1 && zoneTemps.size() == binding.tempIndices().size()
              ? List.copyOf(zoneTemps)
              : null;
      rawSamples.add(new RawSample(avgTemp, setpoint, zones, instant, clockVal, unparseableTime));
    }

    if (skippedRows > MAX_SKIP_WARNINGS) {
      warnings.add(
          "Skipped %d additional rows without valid temperature readings."
              .formatted(skippedRows - MAX_SKIP_WARNINGS));
    }
    if (rawSamples.isEmpty()) {
      throw new InvalidInputException(
          "No valid temperature readings were found after the header row.");
    }

    normalizeSampleOrder(rawSamples, headerImpliesElapsed);

    List<TemperaturePoint> rawPoints = new ArrayList<>(rawSamples.size());
    @Nullable Instant firstInstant = null;
    @Nullable Instant lastInstant = null;
    @Nullable Double firstRawMinutes = null;
    double lastElapsedMinutes = 0.0;
    @Nullable Double previousClockMinutes = null;
    long clockDayOffsetMinutes = 0L;
    boolean treatTwoOrThreePartAsClock = false;
    boolean clockModeDecided = false;
    @Nullable LocalDate preambleDate = preamble.dateOnly();

    double peakTemp = Double.NEGATIVE_INFINITY;
    int peakIndex = -1;
    @Nullable Double firstTemp = null;
    boolean warnedUnparseableTime = false;

    for (RawSample sample : rawSamples) {
      if (firstTemp == null) {
        firstTemp = sample.avgTemp();
      }

      double elapsedMinutes;
      Instant sampleInstant = sample.instant();
      ParsedClock sampleClock = sample.clockVal();
      if (sampleInstant != null) {
        if (firstInstant == null) {
          firstInstant = sampleInstant;
        }
        lastInstant = sampleInstant;
        elapsedMinutes =
            Math.max(0.0, Duration.between(firstInstant, sampleInstant).toMillis() / 60_000.0);
      } else if (sampleClock != null) {
        if (!clockModeDecided) {
          clockModeDecided = true;
          treatTwoOrThreePartAsClock =
              sampleClock.hasColons()
                  && !sampleClock.over24Hours()
                  && !headerImpliesElapsed
                  && sampleClock.totalMinutes() > 0.0
                  && (headerImpliesClock || preambleDate != null);
        }
        if (treatTwoOrThreePartAsClock) {
          double clockMinutes = sampleClock.totalMinutes();
          if (previousClockMinutes != null
              && previousClockMinutes - clockMinutes > MIDNIGHT_ROLLOVER_THRESHOLD_MINUTES) {
            clockDayOffsetMinutes += 24L * 60L;
          }
          previousClockMinutes = clockMinutes;
          double absoluteMinutes = clockMinutes + clockDayOffsetMinutes;
          if (firstRawMinutes == null) {
            firstRawMinutes = absoluteMinutes;
            if (preambleDate != null && firstInstant == null) {
              LocalTime startClock =
                  LocalTime.ofSecondOfDay(Math.round(clockMinutes * 60.0) % 86_400L);
              firstInstant = preambleDate.atTime(startClock).atZone(studioZone).toInstant();
            }
          }
          elapsedMinutes = Math.max(0.0, absoluteMinutes - firstRawMinutes);
        } else {
          double valueMinutes = sampleClock.totalMinutes();
          if (firstRawMinutes == null) {
            firstRawMinutes = valueMinutes;
          }
          elapsedMinutes = Math.max(0.0, valueMinutes - firstRawMinutes);
        }
      } else {
        if (sample.unparseableTime() && !warnedUnparseableTime) {
          warnedUnparseableTime = true;
          warnings.add("Some rows had unreadable timestamps and used estimated intervals.");
        }
        elapsedMinutes = rawPoints.isEmpty() ? 0.0 : lastElapsedMinutes + defaultStepMinutes;
      }

      elapsedMinutes = round1(elapsedMinutes);
      lastElapsedMinutes = Math.max(lastElapsedMinutes, elapsedMinutes);

      int idx = rawPoints.size();
      rawPoints.add(
          new TemperaturePoint(elapsedMinutes, sample.avgTemp(), sample.setpoint(), sample.zones()));
      if (sample.avgTemp() > peakTemp) {
        peakTemp = sample.avgTemp();
        peakIndex = idx;
      }
    }

    if (peakTemp < MIN_TEMP || peakTemp > MAX_TEMP) {
      throw new InvalidInputException(
          "Peak temperature %.1f is out of range (must be between 0 and 9,999.9)."
              .formatted(peakTemp));
    }

    @Nullable Instant startedAt = firstInstant != null ? firstInstant : preamble.startedAt();
    @Nullable Instant finishedAt = lastInstant;
    long rawDurationMinutes;
    if (firstInstant != null && lastInstant != null) {
      if (lastInstant.isBefore(firstInstant)) {
        throw new InvalidInputException(
            "Kiln log timestamps are out of order (end time is before start time).");
      }
      rawDurationMinutes =
          Math.round(Duration.between(firstInstant, lastInstant).toSeconds() / 60.0);
    } else {
      if (!Double.isFinite(lastElapsedMinutes)
          || lastElapsedMinutes < 0.0
          || lastElapsedMinutes > MAX_DURATION_MINUTES) {
        throw new InvalidInputException(
            "Firing duration must be between 0 and %,d minutes.".formatted(MAX_DURATION_MINUTES));
      }
      rawDurationMinutes = Math.round(lastElapsedMinutes);
    }
    if (rawDurationMinutes < 0L || rawDurationMinutes > MAX_DURATION_MINUTES) {
      throw new InvalidInputException(
          "Firing duration must be between 0 and %,d minutes.".formatted(MAX_DURATION_MINUTES));
    }
    int durationMinutes = (int) rawDurationMinutes;
    if (startedAt != null && finishedAt == null) {
      finishedAt = startedAt.plusMillis(Math.round(lastElapsedMinutes * 60_000.0));
    }
    if (startedAt != null && finishedAt != null && finishedAt.isBefore(startedAt)) {
      throw new InvalidInputException(
          "Kiln log timestamps are out of order (end time is before start time).");
    }

    UnitDecision unit =
        decideUnit(
            unitOverride,
            headerUnitHint,
            preamble.unit(),
            cellUnitHint,
            peakTemp,
            firstTemp == null ? 0.0 : firstTemp,
            studioDefaultUnit);

    String errorCode = rowErrorCode != null ? rowErrorCode : preamble.errorCode();
    List<TemperaturePoint> curve = downsample(rawPoints, peakIndex, MAX_CURVE_POINTS);
    BigDecimal peakDecimal = BigDecimal.valueOf(peakTemp).setScale(2, RoundingMode.HALF_UP);

    List<String> cleanHeaders =
        headers.stream()
            .map(h -> sanitize(h, MAX_HEADER_LENGTH))
            .filter(s -> !s.isEmpty())
            .toList();

    return new ParsedLog(
        source,
        unit.unit(),
        unit.inferred(),
        startedAt,
        finishedAt,
        durationMinutes,
        peakDecimal,
        preamble.cone(),
        preamble.kind(),
        errorCode,
        rawPoints.size(),
        binding.tempIndices().size(),
        cleanHeaders,
        List.copyOf(warnings),
        curve);
  }

  /**
   * Reverses rows when a controller exported newest-first (monotonically non-increasing timestamps
   * or elapsed values), and rejects logs whose start/end timestamps move backwards without being
   * cleanly reversed.
   */
  private static void normalizeSampleOrder(List<RawSample> samples, boolean headerImpliesElapsed) {
    @Nullable Instant firstInst = null;
    @Nullable Instant prevInst = null;
    @Nullable Instant lastInst = null;
    int instantCount = 0;
    boolean instantNonIncreasing = true;

    @Nullable Double firstClock = null;
    @Nullable Double prevClock = null;
    @Nullable Double lastClock = null;
    int clockCount = 0;
    boolean clockNonIncreasing = true;
    boolean anyHasColons = false;

    for (RawSample sample : samples) {
      Instant inst = sample.instant();
      if (inst != null) {
        instantCount++;
        if (firstInst == null) {
          firstInst = inst;
        }
        if (prevInst != null && inst.isAfter(prevInst)) {
          instantNonIncreasing = false;
        }
        prevInst = inst;
        lastInst = inst;
      }
      ParsedClock cv = sample.clockVal();
      if (cv != null) {
        clockCount++;
        if (cv.hasColons()) {
          anyHasColons = true;
        }
        double mins = cv.totalMinutes();
        if (firstClock == null) {
          firstClock = mins;
        }
        if (prevClock != null && mins > prevClock) {
          clockNonIncreasing = false;
        }
        prevClock = mins;
        lastClock = mins;
      }
    }

    if (instantCount >= 2
        && firstInst != null
        && lastInst != null
        && lastInst.isBefore(firstInst)) {
      if (instantNonIncreasing) {
        Collections.reverse(samples);
      } else {
        throw new InvalidInputException(
            "Kiln log timestamps are out of order (end time is before start time).");
      }
      return;
    }

    if (instantCount == 0
        && clockCount >= 2
        && (!anyHasColons || headerImpliesElapsed)
        && firstClock != null
        && lastClock != null
        && lastClock < firstClock) {
      if (clockNonIncreasing) {
        Collections.reverse(samples);
      } else {
        throw new InvalidInputException(
            "Kiln log elapsed times are out of order (end time is before start time).");
      }
    }
  }

  private static UnitDecision decideUnit(
      @Nullable TemperatureUnit override,
      @Nullable TemperatureUnit headerHint,
      @Nullable TemperatureUnit preambleHint,
      @Nullable TemperatureUnit cellHint,
      double peakTemp,
      double firstTemp,
      @Nullable TemperatureUnit studioDefault) {
    if (override != null) {
      return new UnitDecision(override, false);
    }
    if (headerHint != null) {
      return new UnitDecision(headerHint, true);
    }
    if (preambleHint != null) {
      return new UnitDecision(preambleHint, true);
    }
    if (cellHint != null) {
      return new UnitDecision(cellHint, true);
    }
    if (peakTemp > PEAK_FAHRENHEIT_THRESHOLD) {
      return new UnitDecision(TemperatureUnit.F, true);
    }
    if (firstTemp >= 0.0
        && firstTemp <= AMBIENT_CELSIUS_MAX
        && peakTemp >= CELSIUS_PEAK_MIN
        && peakTemp <= PEAK_FAHRENHEIT_THRESHOLD) {
      return new UnitDecision(TemperatureUnit.C, true);
    }
    return new UnitDecision(studioDefault != null ? studioDefault : TemperatureUnit.F, true);
  }

  /** Downsamples a curve to at most {@code maxPoints} points, preserving start, peak, and end. */
  static List<TemperaturePoint> downsample(List<TemperaturePoint> points, int maxPoints) {
    int n = points.size();
    if (n <= maxPoints) {
      return List.copyOf(points);
    }
    int peakIndex = 0;
    double peakTemp = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < n; i++) {
      double t = points.get(i).temp();
      if (t > peakTemp) {
        peakTemp = t;
        peakIndex = i;
      }
    }
    return downsample(points, peakIndex, maxPoints);
  }

  private static List<TemperaturePoint> downsample(
      List<TemperaturePoint> points, int peakIndex, int maxPoints) {
    int n = points.size();
    if (n <= maxPoints) {
      return List.copyOf(points);
    }
    TreeSet<Integer> indices = new TreeSet<>();
    indices.add(0);
    indices.add(n - 1);
    if (peakIndex >= 0 && peakIndex < n) {
      indices.add(peakIndex);
    }
    int slots = maxPoints;
    for (int i = 0; i < slots; i++) {
      int idx = (int) Math.round(i * (double) (n - 1) / (slots - 1));
      indices.add(Math.min(n - 1, Math.max(0, idx)));
    }
    // If adding peakIndex pushed the unique count to maxPoints + 1, drop the non-pinned neighbor
    // closest to peakIndex.
    if (indices.size() > maxPoints) {
      int toDrop = -1;
      int bestDist = Integer.MAX_VALUE;
      for (int idx : indices) {
        if (idx == 0 || idx == n - 1 || idx == peakIndex) {
          continue;
        }
        int dist = Math.abs(idx - peakIndex);
        if (dist < bestDist) {
          bestDist = dist;
          toDrop = idx;
        }
      }
      if (toDrop >= 0) {
        indices.remove(toDrop);
      }
    }
    List<TemperaturePoint> sampled = new ArrayList<>(indices.size());
    for (int idx : indices) {
      sampled.add(points.get(idx));
    }
    return List.copyOf(sampled);
  }

  private static HeaderMatch locateHeader(
      List<String> lines, KilnLogSource source, ColumnMap overrides) {
    int scanLimit = Math.min(MAX_HEADER_SCAN_LINES, lines.size());
    @Nullable HeaderMatch best = null;
    int bestScore = -1;

    for (int delimIdx = 0; delimIdx < CANDIDATE_DELIMITERS.length; delimIdx++) {
      char delimiter = CANDIDATE_DELIMITERS[delimIdx];
      for (int lineIdx = 0; lineIdx < scanLimit; lineIdx++) {
        List<String> cells = parseCsvRow(lines.get(lineIdx), delimiter);
        if (cells.size() < 2 && !notBlank(overrides.temperature())) {
          continue;
        }
        ColumnBinding binding = resolveColumns(cells, source, overrides, false);
        if (binding.tempIndices().isEmpty()) {
          continue;
        }
        int lookaheadValid = countValidLookaheadRows(lines, lineIdx + 1, delimiter, binding);
        if (lookaheadValid == 0) {
          continue;
        }
        int score =
            binding.overrideMatches() * 30
                + (binding.timeIndex() >= 0 ? 15 : 0)
                + Math.min(binding.tempIndices().size(), 4) * 10
                + (binding.setpointIndex() >= 0 ? 5 : 0)
                + (binding.segmentIndex() >= 0 ? 3 : 0)
                + (binding.outputIndices().isEmpty() ? 0 : 3)
                + (binding.errorIndex() >= 0 ? 3 : 0)
                + lookaheadValid * 2
                + (3 - delimIdx);
        if (score > bestScore) {
          bestScore = score;
          best = new HeaderMatch(lineIdx, delimiter, cells, binding);
        }
      }
    }

    if (best == null) {
      if (overrides.hasAny()) {
        // Re-check first few lines to surface a specific missing override column name if possible.
        for (char delimiter : CANDIDATE_DELIMITERS) {
          for (int lineIdx = 0; lineIdx < scanLimit; lineIdx++) {
            List<String> cells = parseCsvRow(lines.get(lineIdx), delimiter);
            if (cells.size() >= 2) {
              ColumnBinding auto = resolveColumns(cells, source, ColumnMap.empty(), false);
              if (!auto.tempIndices().isEmpty()) {
                resolveColumns(cells, source, overrides, true);
              }
            }
          }
        }
      }
      throw new InvalidInputException(
          "Could not find a kiln log header row with a temperature column in the first %d lines."
              .formatted(MAX_HEADER_SCAN_LINES));
    }

    if (overrides.hasAny()) {
      // Strictly verify every requested override exists on the selected header row.
      ColumnBinding strict = resolveColumns(best.headers(), source, overrides, true);
      return new HeaderMatch(best.lineIndex(), best.delimiter(), best.headers(), strict);
    }
    return best;
  }

  private static int countValidLookaheadRows(
      List<String> lines, int start, char delimiter, ColumnBinding binding) {
    int checked = 0;
    int valid = 0;
    int maxTempCols = Math.min(binding.tempIndices().size(), MAX_ZONES);
    for (int i = start; i < lines.size() && checked < 5; i++) {
      checked++;
      List<String> cells = parseCsvRow(lines.get(i), delimiter);
      for (int j = 0; j < maxTempCols; j++) {
        int idx = binding.tempIndices().get(j);
        if (idx < cells.size() && parseNumberWithUnit(cells.get(idx)) != null) {
          valid++;
          break;
        }
      }
    }
    return valid;
  }

  private static ColumnBinding resolveColumns(
      List<String> rawHeaders, KilnLogSource source, ColumnMap overrides, boolean strictOverrides) {
    List<String> normalized = new ArrayList<>(rawHeaders.size());
    List<String> stripped = new ArrayList<>(rawHeaders.size());
    for (String h : rawHeaders) {
      String norm = normalizeHeader(h);
      normalized.add(norm);
      stripped.add(stripUnitSuffix(norm));
    }

    int overrideMatches = 0;
    int timeIdx = -1;
    List<Integer> tempIndices = new ArrayList<>();
    int setpointIdx = -1;
    int segmentIdx = -1;
    List<Integer> outputIndices = new ArrayList<>();
    int errorIdx = -1;
    boolean errorDedicated = false;

    @Nullable String timeOverride = overrides.time();
    if (timeOverride != null && !timeOverride.isBlank()) {
      timeIdx = findExactHeader(normalized, stripped, timeOverride);
      if (timeIdx < 0 && strictOverrides) {
        throw missingOverride(timeOverride);
      }
      if (timeIdx >= 0) {
        overrideMatches++;
      }
    }

    @Nullable String tempOverride = overrides.temperature();
    if (tempOverride != null && !tempOverride.isBlank()) {
      for (String part : splitCommaList(tempOverride)) {
        int idx = findExactHeader(normalized, stripped, part);
        if (idx < 0 && strictOverrides) {
          throw missingOverride(part);
        }
        if (idx >= 0 && !tempIndices.contains(idx)) {
          tempIndices.add(idx);
          overrideMatches++;
        }
      }
    }

    @Nullable String setpointOverride = overrides.setpoint();
    if (setpointOverride != null && !setpointOverride.isBlank()) {
      setpointIdx = findExactHeader(normalized, stripped, setpointOverride);
      if (setpointIdx < 0 && strictOverrides) {
        throw missingOverride(setpointOverride);
      }
      if (setpointIdx >= 0) {
        overrideMatches++;
      }
    }

    @Nullable String segmentOverride = overrides.segment();
    if (segmentOverride != null && !segmentOverride.isBlank()) {
      segmentIdx = findExactHeader(normalized, stripped, segmentOverride);
      if (segmentIdx < 0 && strictOverrides) {
        throw missingOverride(segmentOverride);
      }
      if (segmentIdx >= 0) {
        overrideMatches++;
      }
    }

    @Nullable String outputOverride = overrides.output();
    if (outputOverride != null && !outputOverride.isBlank()) {
      int idx = findExactHeader(normalized, stripped, outputOverride);
      if (idx < 0 && strictOverrides) {
        throw missingOverride(outputOverride);
      }
      if (idx >= 0) {
        outputIndices.add(idx);
        overrideMatches++;
      }
    }

    @Nullable String errorOverride = overrides.error();
    if (errorOverride != null && !errorOverride.isBlank()) {
      errorIdx = findExactHeader(normalized, stripped, errorOverride);
      if (errorIdx < 0 && strictOverrides) {
        throw missingOverride(errorOverride);
      }
      if (errorIdx >= 0) {
        errorDedicated = true;
        overrideMatches++;
      }
    }

    Set<Integer> claimed = new LinkedHashSet<>();
    if (timeIdx >= 0) {
      claimed.add(timeIdx);
    }
    claimed.addAll(tempIndices);
    if (setpointIdx >= 0) {
      claimed.add(setpointIdx);
    }
    if (segmentIdx >= 0) {
      claimed.add(segmentIdx);
    }
    claimed.addAll(outputIndices);
    if (errorIdx >= 0) {
      claimed.add(errorIdx);
    }

    Set<String> timeAliases = source == KilnLogSource.TAP ? TAP_TIME_ALIASES : GENESIS_TIME_ALIASES;

    // 1. Time column
    if (timeIdx < 0) {
      for (int i = 0; i < stripped.size(); i++) {
        if (claimed.contains(i)) {
          continue;
        }
        String key = stripped.get(i);
        if (timeAliases.contains(key)
            || (source == KilnLogSource.OTHER && TAP_TIME_ALIASES.contains(key))
            || key.startsWith("elapsed")
            || key.equals("time")) {
          timeIdx = i;
          claimed.add(i);
          break;
        }
      }
    }

    // 2. Output % columns first so "Z1 Out %" is never mistaken for a zone temperature
    if (outputIndices.isEmpty()) {
      for (int i = 0; i < stripped.size(); i++) {
        if (claimed.contains(i)) {
          continue;
        }
        if (isOutputHeader(normalized.get(i), stripped.get(i))) {
          outputIndices.add(i);
          claimed.add(i);
        }
      }
    }

    // 3. Setpoint / target temperature (claim ALL setpoint/target columns so multi-zone setpoints
    // like "Zone 2 Setpoint" are never misclassified as chamber temperatures)
    for (int i = 0; i < stripped.size(); i++) {
      if (claimed.contains(i)) {
        continue;
      }
      String key = stripped.get(i);
      if (SETPOINT_ALIASES.contains(key)
          || key.contains("setpoint")
          || key.contains("set point")
          || key.contains("target")
          || key.endsWith(" sp")) {
        if (setpointIdx < 0) {
          setpointIdx = i;
        }
        claimed.add(i);
      }
    }

    // 4. Segment column
    if (segmentIdx < 0) {
      for (int i = 0; i < stripped.size(); i++) {
        if (claimed.contains(i)) {
          continue;
        }
        if (SEGMENT_ALIASES.contains(stripped.get(i))) {
          segmentIdx = i;
          claimed.add(i);
          break;
        }
      }
    }

    // 5. Error / event column (prefer dedicated error columns over generic event/status)
    if (errorIdx < 0) {
      for (int i = 0; i < stripped.size(); i++) {
        if (claimed.contains(i)) {
          continue;
        }
        if (DEDICATED_ERROR_ALIASES.contains(stripped.get(i))) {
          errorIdx = i;
          errorDedicated = true;
          claimed.add(i);
          break;
        }
      }
    }
    if (errorIdx < 0) {
      for (int i = 0; i < stripped.size(); i++) {
        if (claimed.contains(i)) {
          continue;
        }
        if (EVENT_STATUS_ALIASES.contains(stripped.get(i))) {
          errorIdx = i;
          errorDedicated = false;
          claimed.add(i);
          break;
        }
      }
    }

    // 6. Zone / primary temperature columns
    if (tempIndices.isEmpty()) {
      List<Integer> zoneMatches = new ArrayList<>();
      List<Integer> singleMatches = new ArrayList<>();
      for (int i = 0; i < stripped.size(); i++) {
        if (claimed.contains(i)) {
          continue;
        }
        String key = stripped.get(i);
        if (isOutputHeader(normalized.get(i), key) || NON_CHAMBER_TEMP.matcher(key).find()) {
          continue;
        }
        if (ZONE_TEMP_ALIASES.contains(key)
            || key.matches("tc\\s*\\d+.*")
            || key.matches("zone\\s*\\d+.*")) {
          zoneMatches.add(i);
        } else if (SINGLE_TEMP_ALIASES.contains(key) || key.contains("temp")) {
          singleMatches.add(i);
        }
      }
      if (!zoneMatches.isEmpty()) {
        tempIndices.addAll(zoneMatches);
      } else {
        tempIndices.addAll(singleMatches);
      }
    }

    return new ColumnBinding(
        timeIdx,
        List.copyOf(tempIndices),
        setpointIdx,
        segmentIdx,
        List.copyOf(outputIndices),
        errorIdx,
        errorDedicated,
        overrideMatches);
  }

  private static boolean isOutputHeader(String normalized, String stripped) {
    if (OUTPUT_ALIASES.contains(stripped) || OUTPUT_ALIASES.contains(normalized)) {
      return true;
    }
    return normalized.contains("%")
        || stripped.contains("output")
        || stripped.contains("power")
        || stripped.contains("duty")
        || stripped.contains("relay")
        || stripped.endsWith(" out")
        || stripped.matches("out\\s*\\d+");
  }

  private static int findExactHeader(
      List<String> normalized, List<String> stripped, String target) {
    String normTarget = normalizeHeader(target);
    String stripTarget = stripUnitSuffix(normTarget);
    for (int i = 0; i < normalized.size(); i++) {
      if (normalized.get(i).equals(normTarget) || stripped.get(i).equals(stripTarget)) {
        return i;
      }
    }
    return -1;
  }

  private static InvalidInputException missingOverride(String columnName) {
    return new InvalidInputException(
        "Column '%s' was not found in the CSV header row.".formatted(sanitize(columnName, 80)));
  }

  private static Preamble parsePreamble(List<String> lines, char delimiter, ZoneId studioZone) {
    @Nullable Instant startedAt = null;
    @Nullable LocalDate dateOnly = null;
    @Nullable String cone = null;
    @Nullable FiringKind kind = null;
    @Nullable String errorCode = null;
    @Nullable TemperatureUnit unit = null;

    for (String line : lines) {
      String trimmed = sanitize(line, 240);
      if (trimmed.isEmpty()) {
        continue;
      }
      if (kind == null) {
        kind = detectFiringKind(trimmed);
      }
      if (cone == null) {
        cone = extractConeFromText(trimmed);
      }

      @Nullable KeyValue kv = parsePreambleKeyValue(line, delimiter);
      if (kv == null) {
        continue;
      }
      String key = kv.key().toLowerCase(Locale.ROOT);
      String value = kv.value();
      if (value.isEmpty()) {
        continue;
      }

      if (startedAt == null
          && (key.contains("date")
              || key.contains("start")
              || key.equals("timestamp")
              || key.equals("logged"))) {
        Instant parsedInstant = parseInstant(value, studioZone);
        if (parsedInstant != null) {
          startedAt = parsedInstant;
        } else if (dateOnly == null) {
          dateOnly = parseLocalDate(value);
        }
      }
      if (cone == null && (key.equals("cone") || key.contains("target cone"))) {
        String candidate = sanitize(value, MAX_CONE_LENGTH);
        if (CONE_VALUE.matcher(candidate).matches()) {
          cone = candidate;
        }
      }
      if (unit == null && (key.contains("unit") || key.equals("scale"))) {
        unit = detectUnitText(value);
      }
      if (errorCode == null
          && (key.contains("error")
              || key.contains("fault")
              || key.contains("alarm")
              || key.equals("status"))) {
        boolean dedicated = !key.equals("status");
        errorCode = extractError(value, dedicated);
      }
    }
    return new Preamble(startedAt, dateOnly, cone, kind, errorCode, unit);
  }

  private static @Nullable KeyValue parsePreambleKeyValue(String line, char delimiter) {
    List<String> cells = parseCsvRow(line, delimiter);
    if (cells.size() >= 2) {
      String first = cells.get(0).strip();
      String withoutTrailingColon =
          first.endsWith(":") ? first.substring(0, first.length() - 1).strip() : first;
      if (!withoutTrailingColon.contains(":")) {
        String key = sanitize(withoutTrailingColon, 60);
        String value = sanitize(cells.get(1), 160);
        if (!key.isEmpty() && !value.isEmpty()) {
          return new KeyValue(key, value);
        }
      }
    }
    int colon = line.indexOf(':');
    if (colon > 0 && colon < line.length() - 1) {
      String key = sanitize(stripSurroundingQuotes(line.substring(0, colon)), 60);
      String value = sanitize(stripSurroundingQuotes(line.substring(colon + 1)), 160);
      if (!key.isEmpty() && !value.isEmpty()) {
        return new KeyValue(key, value);
      }
    }
    return null;
  }

  private static @Nullable TemperatureUnit detectHeaderUnit(
      List<String> headers, ColumnBinding binding) {
    for (int idx : binding.tempIndices()) {
      TemperatureUnit found = detectUnitText(headers.get(idx));
      if (found != null) {
        return found;
      }
    }
    if (binding.setpointIndex() >= 0) {
      return detectUnitText(headers.get(binding.setpointIndex()));
    }
    return null;
  }

  private static @Nullable TemperatureUnit detectUnitText(String text) {
    String clean = text.strip();
    if (clean.equalsIgnoreCase("F") || HEADER_FAHRENHEIT.matcher(clean).find()) {
      return TemperatureUnit.F;
    }
    if (clean.equalsIgnoreCase("C") || HEADER_CELSIUS.matcher(clean).find()) {
      return TemperatureUnit.C;
    }
    return null;
  }

  private static TimeUnitKind detectTimeUnit(String header) {
    String lower = header.toLowerCase(Locale.ROOT);
    if (lower.contains("sec") || lower.contains("(s)")) {
      return TimeUnitKind.SECONDS;
    }
    if (lower.contains("hour") || lower.contains("hr") || lower.contains("(h)")) {
      return TimeUnitKind.HOURS;
    }
    return TimeUnitKind.MINUTES;
  }

  private static boolean looksLikeClockHeader(String header) {
    String lower = header.toLowerCase(Locale.ROOT);
    return lower.contains("timestamp")
        || lower.contains("clock")
        || lower.contains("date")
        || lower.equals("time");
  }

  private static boolean looksLikeElapsedHeader(String header) {
    String lower = header.toLowerCase(Locale.ROOT);
    return lower.contains("elapsed")
        || lower.contains("runtime")
        || lower.contains("run time")
        || lower.contains("duration");
  }

  private static @Nullable ParsedClock parseClockOrElapsed(
      String raw, TimeUnitKind unitKind, boolean headerImpliesClock) {
    if (raw.isEmpty()) {
      return null;
    }
    Matcher clock = CLOCK_OR_ELAPSED.matcher(raw);
    if (clock.matches()) {
      int first = Integer.parseInt(clock.group(1));
      int second = Integer.parseInt(clock.group(2));
      String thirdGroup = clock.group(3);
      if (thirdGroup != null) {
        int third = Integer.parseInt(thirdGroup);
        double minutes = first * 60.0 + second + (third / 60.0);
        return new ParsedClock(minutes, true, first >= 24);
      }
      if (unitKind == TimeUnitKind.HOURS || headerImpliesClock) {
        double minutes = first * 60.0 + second;
        return new ParsedClock(minutes, true, first >= 24);
      }
      double minutes = first + (second / 60.0);
      return new ParsedClock(minutes, true, first >= 24);
    }

    try {
      double value = Double.parseDouble(raw);
      if (!Double.isFinite(value) || value < 0.0) {
        return null;
      }
      double minutes =
          switch (unitKind) {
            case SECONDS -> value / 60.0;
            case HOURS -> value * 60.0;
            case MINUTES -> value;
          };
      return new ParsedClock(minutes, false, false);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static @Nullable Instant parseInstant(String raw, ZoneId studioZone) {
    if (raw.length() < 8) {
      return null;
    }
    try {
      return Instant.parse(raw);
    } catch (DateTimeParseException ignored) {
      // Try offset or local formats next
    }
    try {
      return OffsetDateTime.parse(raw).toInstant();
    } catch (DateTimeParseException ignored) {
      // Try local datetime formats next
    }
    for (DateTimeFormatter formatter : LOCAL_DATE_TIME_FORMATS) {
      try {
        return LocalDateTime.parse(raw, formatter).atZone(studioZone).toInstant();
      } catch (DateTimeParseException ignored) {
        // Try next pattern
      }
    }
    return null;
  }

  private static @Nullable LocalDate parseLocalDate(String raw) {
    for (DateTimeFormatter formatter : LOCAL_DATE_FORMATS) {
      try {
        return LocalDate.parse(raw, formatter);
      } catch (DateTimeParseException ignored) {
        // Try next pattern
      }
    }
    return null;
  }

  private static @Nullable ParsedNumber parseNumberWithUnit(String rawCell) {
    String clean = sanitize(rawCell, 40);
    if (clean.isEmpty()) {
      return null;
    }
    Matcher m = NUMBER_WITH_UNIT.matcher(clean);
    if (!m.matches()) {
      return null;
    }
    try {
      double value = Double.parseDouble(m.group(1));
      if (!Double.isFinite(value)) {
        return null;
      }
      String suffix = m.group(2);
      @Nullable TemperatureUnit unit = null;
      if (suffix != null) {
        unit = suffix.equalsIgnoreCase("C") ? TemperatureUnit.C : TemperatureUnit.F;
      }
      return new ParsedNumber(value, unit);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static @Nullable String extractError(String rawCell, boolean dedicatedColumn) {
    String clean = sanitize(rawCell, MAX_ERROR_CODE_LENGTH);
    if (clean.isEmpty()) {
      return null;
    }
    String lower = clean.toLowerCase(Locale.ROOT);
    if (BENIGN_STATUS_VALUES.contains(lower)) {
      return null;
    }
    if (dedicatedColumn) {
      return clean;
    }
    return ERROR_EVENT_HINT.matcher(clean).find() ? clean : null;
  }

  private static @Nullable String extractConeFromText(String text) {
    Matcher m = CONE_IN_TEXT.matcher(text);
    if (!m.find()) {
      return null;
    }
    return sanitize(m.group(1), MAX_CONE_LENGTH);
  }

  private static @Nullable FiringKind detectFiringKind(String text) {
    String lower = text.toLowerCase(Locale.ROOT);
    if (lower.contains("glaze")) {
      return FiringKind.GLAZE;
    }
    if (lower.contains("bisque")) {
      return FiringKind.BISQUE;
    }
    return null;
  }

  private static List<String> splitNonBlankLines(String text) {
    List<String> lines = new ArrayList<>();
    int start = 0;
    int len = text.length();
    for (int i = 0; i < len; i++) {
      char c = text.charAt(i);
      if (c == '\n' || c == '\r') {
        addIfNonBlank(lines, text, start, i);
        if (c == '\r' && i + 1 < len && text.charAt(i + 1) == '\n') {
          i++;
        }
        start = i + 1;
      }
    }
    addIfNonBlank(lines, text, start, len);
    return lines;
  }

  private static void addIfNonBlank(List<String> lines, String text, int start, int end) {
    for (int i = start; i < end; i++) {
      if (!Character.isWhitespace(text.charAt(i))) {
        if (lines.size() >= MAX_NON_BLANK_LINES) {
          throw new InvalidInputException(
              "Kiln log exceeds the %,d-row limit.".formatted(MAX_DATA_ROWS));
        }
        lines.add(text.substring(start, end));
        return;
      }
    }
  }

  /** Parses one RFC-4180 CSV line using {@code delimiter}. */
  static List<String> parseCsvRow(String line, char delimiter) {
    List<String> cells = new ArrayList<>();
    StringBuilder cell = new StringBuilder();
    boolean quoted = false;
    int len = line.length();
    for (int i = 0; i < len; i++) {
      char c = line.charAt(i);
      if (quoted) {
        if (c == '"') {
          if (i + 1 < len && line.charAt(i + 1) == '"') {
            cell.append('"');
            i++;
          } else {
            quoted = false;
          }
        } else {
          cell.append(c);
        }
      } else if (c == '"' && cell.toString().isBlank()) {
        cell.setLength(0);
        quoted = true;
      } else if (c == delimiter) {
        cells.add(cell.toString().strip());
        cell.setLength(0);
      } else {
        cell.append(c);
      }
    }
    cells.add(cell.toString().strip());
    return cells;
  }

  private static List<String> splitCommaList(String text) {
    List<String> parts = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == ',') {
        String trimmed = current.toString().strip();
        if (!trimmed.isEmpty()) {
          parts.add(trimmed);
        }
        current.setLength(0);
      } else {
        current.append(c);
      }
    }
    String tail = current.toString().strip();
    if (!tail.isEmpty()) {
      parts.add(tail);
    }
    return parts;
  }

  private static String normalizeHeader(String raw) {
    String clean = sanitize(raw, MAX_HEADER_LENGTH).toLowerCase(Locale.ROOT);
    return MULTI_SPACE.matcher(clean).replaceAll(" ");
  }

  private static String stripUnitSuffix(String normalizedHeader) {
    return TRAILING_UNIT.matcher(normalizedHeader).replaceAll("").strip();
  }

  private static String stripSurroundingQuotes(String value) {
    String s = value.strip();
    if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
      return s.substring(1, s.length() - 1).strip();
    }
    return s;
  }

  private static String sanitize(String raw, int maxLength) {
    String noCtrl = CONTROL_CHARS.matcher(raw).replaceAll(" ");
    String collapsed = MULTI_SPACE.matcher(noCtrl).replaceAll(" ").strip();
    if (collapsed.length() <= maxLength) {
      return collapsed;
    }
    return collapsed.substring(0, maxLength).strip();
  }

  private static boolean notBlank(@Nullable String value) {
    return value != null && !value.isBlank();
  }

  private static double average(List<Double> values) {
    double sum = 0.0;
    for (double v : values) {
      sum += v;
    }
    return sum / values.size();
  }

  private static double round1(double value) {
    return Math.round(value * 10.0) / 10.0;
  }

  private enum TimeUnitKind {
    SECONDS,
    MINUTES,
    HOURS
  }

  private record UnitDecision(TemperatureUnit unit, boolean inferred) {}

  private record ParsedNumber(double value, @Nullable TemperatureUnit unit) {}

  private record ParsedClock(double totalMinutes, boolean hasColons, boolean over24Hours) {}

  private record RawSample(
      double avgTemp,
      @Nullable Double setpoint,
      @Nullable List<Double> zones,
      @Nullable Instant instant,
      @Nullable ParsedClock clockVal,
      boolean unparseableTime) {}

  private record KeyValue(String key, String value) {}

  private record Preamble(
      @Nullable Instant startedAt,
      @Nullable LocalDate dateOnly,
      @Nullable String cone,
      @Nullable FiringKind kind,
      @Nullable String errorCode,
      @Nullable TemperatureUnit unit) {}

  private record ColumnBinding(
      int timeIndex,
      List<Integer> tempIndices,
      int setpointIndex,
      int segmentIndex,
      List<Integer> outputIndices,
      int errorIndex,
      boolean errorDedicated,
      int overrideMatches) {}

  private record HeaderMatch(
      int lineIndex, char delimiter, List<String> headers, ColumnBinding binding) {}
}
