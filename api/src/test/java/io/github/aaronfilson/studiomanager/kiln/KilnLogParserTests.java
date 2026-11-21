package io.github.aaronfilson.studiomanager.kiln;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class KilnLogParserTests {

  private static final ZoneId CHICAGO = ZoneId.of("America/Chicago");
  private static final ZoneId LONDON = ZoneId.of("Europe/London");

  @Test
  void parsesGenesisFixtureWithPreambleZonesAndInferredFahrenheit() throws IOException {
    String csv = fixture("kilns/genesis-sample.csv");

    KilnLogParser.ParsedLog parsed =
        KilnLogParser.parse(csv, KilnLogSource.GENESIS, null, null, CHICAGO, TemperatureUnit.C);

    assertThat(parsed.source()).isEqualTo(KilnLogSource.GENESIS);
    assertThat(parsed.tempUnit()).isEqualTo(TemperatureUnit.F);
    assertThat(parsed.tempUnitInferred()).isTrue();
    assertThat(parsed.peakTemp()).isEqualByComparingTo(new BigDecimal("2232.00"));
    assertThat(parsed.durationMinutes()).isEqualTo(570);
    assertThat(parsed.detectedCone()).isEqualTo("6");
    assertThat(parsed.detectedKind()).isEqualTo(FiringKind.GLAZE);
    assertThat(parsed.errorCode()).isNull();
    assertThat(parsed.zoneCount()).isEqualTo(3);
    assertThat(parsed.rowCount()).isEqualTo(18);
    assertThat(parsed.startedAt()).isEqualTo(Instant.parse("2026-11-10T14:00:00Z"));
    assertThat(parsed.finishedAt()).isEqualTo(Instant.parse("2026-11-10T23:30:00Z"));
    assertThat(parsed.temperatureCurve()).hasSize(18);
    assertThat(parsed.temperatureCurve().getFirst().elapsedMinutes()).isEqualTo(0.0);
    assertThat(parsed.temperatureCurve().getFirst().zoneTemps()).containsExactly(72.0, 71.0, 72.0);
    assertThat(parsed.temperatureCurve().get(13).temp()).isEqualTo(2232.0);
    assertThat(parsed.temperatureCurve().get(13).setpoint()).isEqualTo(2232.0);
    assertThat(parsed.warnings()).isEmpty();
  }

  @Test
  void extractsControllerErrorCodeFromGenesisErrorFixture() throws IOException {
    String csv = fixture("kilns/genesis-error-sample.csv");

    KilnLogParser.ParsedLog parsed =
        KilnLogParser.parse(csv, KilnLogSource.GENESIS, null, null, CHICAGO, TemperatureUnit.F);

    assertThat(parsed.errorCode()).isEqualTo("E-1");
    assertThat(parsed.detectedCone()).isEqualTo("04");
    assertThat(parsed.detectedKind()).isEqualTo(FiringKind.BISQUE);
    assertThat(parsed.peakTemp()).isEqualByComparingTo(new BigDecimal("1605.00"));
    assertThat(parsed.durationMinutes()).isEqualTo(271);
  }

  @Test
  void parsesTapFixtureWithTimestampsCelsiusHeaderAndBenignEvents() throws IOException {
    String csv = fixture("kilns/tap-sample.csv");

    KilnLogParser.ParsedLog parsed =
        KilnLogParser.parse(csv, KilnLogSource.TAP, null, null, LONDON, TemperatureUnit.F);

    assertThat(parsed.source()).isEqualTo(KilnLogSource.TAP);
    assertThat(parsed.tempUnit()).isEqualTo(TemperatureUnit.C);
    assertThat(parsed.tempUnitInferred()).isTrue();
    assertThat(parsed.peakTemp()).isEqualByComparingTo(new BigDecimal("1222.00"));
    assertThat(parsed.durationMinutes()).isEqualTo(510);
    assertThat(parsed.detectedCone()).isEqualTo("6");
    assertThat(parsed.detectedKind()).isEqualTo(FiringKind.GLAZE);
    // "Firing Start", "Hold Start", "Cooling", "Firing Complete" are not error codes
    assertThat(parsed.errorCode()).isNull();
    assertThat(parsed.zoneCount()).isEqualTo(1);
    assertThat(parsed.startedAt()).isEqualTo(Instant.parse("2026-11-10T09:00:00Z"));
    assertThat(parsed.finishedAt()).isEqualTo(Instant.parse("2026-11-10T17:30:00Z"));
    assertThat(parsed.temperatureCurve().getFirst().zoneTemps()).isNull();
  }

  @Test
  void parsesSemicolonAndTabDelimitersWithBomAndCustomColumnMapping() {
    String semicolonCsv =
        "\uFEFF"
            + "Kiln export\n"
            + "Min;Chamber;Goal;Fault\n"
            + "0;20;25;OK\n"
            + "60;600;610;OK\n"
            + "120;1060;1060;FTH\n";

    KilnLogParser.ColumnMap custom =
        new KilnLogParser.ColumnMap("Min", "Chamber", "Goal", null, null, "Fault");

    KilnLogParser.ParsedLog parsed =
        KilnLogParser.parse(
            semicolonCsv, KilnLogSource.OTHER, null, custom, LONDON, TemperatureUnit.F);

    // Ambient 20 and peak 1060 infer Celsius even with US default
    assertThat(parsed.tempUnit()).isEqualTo(TemperatureUnit.C);
    assertThat(parsed.tempUnitInferred()).isTrue();
    assertThat(parsed.peakTemp()).isEqualByComparingTo(new BigDecimal("1060.00"));
    assertThat(parsed.durationMinutes()).isEqualTo(120);
    assertThat(parsed.errorCode()).isEqualTo("FTH");

    String tabCsv = "Elapsed (s)\tActual\tTarget\n0\t72\t72\n3600\t1200\t1200\n7200\t1945\t1945\n";
    KilnLogParser.ParsedLog tabParsed =
        KilnLogParser.parse(tabCsv, KilnLogSource.OTHER, TemperatureUnit.C, null, CHICAGO, null);
    assertThat(tabParsed.tempUnit()).isEqualTo(TemperatureUnit.C);
    assertThat(tabParsed.tempUnitInferred()).isFalse();
    assertThat(tabParsed.durationMinutes()).isEqualTo(120);
    assertThat(tabParsed.peakTemp()).isEqualByComparingTo(new BigDecimal("1945.00"));
  }

  @Test
  void downsamplesLargeCurvesToAtMost250PointsWhilePreservingStartPeakAndEnd() {
    StringBuilder csv = new StringBuilder("Elapsed,TC1,Setpoint\n");
    int totalRows = 1200;
    int peakRow = 733;
    for (int i = 0; i < totalRows; i++) {
      int totalSec = i * 30;
      int h = totalSec / 3600;
      int m = (totalSec % 3600) / 60;
      int s = totalSec % 60;
      double temp;
      if (i == peakRow) {
        temp = 2248.5;
      } else if (i < peakRow) {
        temp = 70.0 + (2150.0 * i / peakRow);
      } else {
        temp = 2220.0 - (1000.0 * (i - peakRow) / (totalRows - peakRow));
      }
      csv.append("%02d:%02d:%02d,%.1f,%.1f\n".formatted(h, m, s, temp, temp));
    }

    KilnLogParser.ParsedLog parsed =
        KilnLogParser.parse(
            csv.toString(), KilnLogSource.GENESIS, null, null, CHICAGO, TemperatureUnit.F);

    assertThat(parsed.rowCount()).isEqualTo(totalRows);
    assertThat(parsed.temperatureCurve()).hasSizeLessThanOrEqualTo(KilnLogParser.MAX_CURVE_POINTS);
    assertThat(parsed.temperatureCurve().getFirst().elapsedMinutes()).isEqualTo(0.0);
    assertThat(parsed.temperatureCurve().getLast().elapsedMinutes()).isEqualTo(599.5);
    assertThat(parsed.temperatureCurve()).extracting(TemperaturePoint::temp).contains(2248.5);
    assertThat(parsed.peakTemp()).isEqualByComparingTo(new BigDecimal("2248.50"));
  }

  @Test
  void skipsMalformedDataRowsAndSanitizesErrorCodes() {
    String csv =
        """
        Elapsed,Temp,Error
        00:00:00,72,
        00:01:00,BAD_NUM,
        00:02:00,1850,"E-1\t\u0007 thermocouple open circuit on zone 1 exceeded limit"
        00:03:00,1800,
        """;

    KilnLogParser.ParsedLog parsed =
        KilnLogParser.parse(csv, KilnLogSource.OTHER, null, null, CHICAGO, TemperatureUnit.F);

    assertThat(parsed.rowCount()).isEqualTo(3);
    assertThat(parsed.warnings()).hasSize(1);
    assertThat(parsed.errorCode()).isNotNull();
    assertThat(parsed.errorCode().length()).isLessThanOrEqualTo(40);
    assertThat(parsed.errorCode()).doesNotContain("\t").doesNotContain("\u0007");
  }

  @Test
  void rejectsOversizedBinaryMalformedAndUnrecognizedFiles() {
    assertThatThrownBy(() ->
            KilnLogParser.parse(
                "   ", KilnLogSource.GENESIS, null, null, CHICAGO, TemperatureUnit.F))
        .isInstanceOf(InvalidInputException.class)
        .hasMessageContaining("empty");

    assertThatThrownBy(() ->
            KilnLogParser.parse(
                "a\0b", KilnLogSource.GENESIS, null, null, CHICAGO, TemperatureUnit.F))
        .isInstanceOf(InvalidInputException.class)
        .hasMessageContaining("binary");

    String overTwoMb = "Elapsed,Temp\n" + "00:00:00,1000\n".repeat(160_000);
    assertThatThrownBy(() ->
            KilnLogParser.parse(
                overTwoMb, KilnLogSource.GENESIS, null, null, CHICAGO, TemperatureUnit.F))
        .isInstanceOf(InvalidInputException.class)
        .hasMessageContaining("2 MB");

    StringBuilder overFiftyThousandRows = new StringBuilder("t,temp\n");
    for (int i = 0; i < 50_005; i++) {
      overFiftyThousandRows.append(i).append(",500\n");
    }
    assertThatThrownBy(() ->
            KilnLogParser.parse(
                overFiftyThousandRows.toString(),
                KilnLogSource.OTHER,
                null,
                null,
                CHICAGO,
                TemperatureUnit.F))
        .isInstanceOf(InvalidInputException.class)
        .hasMessageContaining("50,000");

    assertThatThrownBy(() ->
            KilnLogParser.parse(
                "Name,Email\nMo,mo@example.test\n",
                KilnLogSource.OTHER,
                null,
                null,
                CHICAGO,
                TemperatureUnit.F))
        .isInstanceOf(InvalidInputException.class)
        .hasMessageContaining("header");

    KilnLogParser.ColumnMap missingCol =
        new KilnLogParser.ColumnMap(null, "NonexistentCol", null, null, null, null);
    assertThatThrownBy(() ->
            KilnLogParser.parse(
                "Elapsed,Temp\n0,100\n1,200\n",
                KilnLogSource.OTHER,
                null,
                missingCol,
                CHICAGO,
                TemperatureUnit.F))
        .isInstanceOf(InvalidInputException.class)
        .hasMessageContaining("NonexistentCol");
  }

  @Test
  void rejectsOutOfRangeTemperaturesDurationsAndTooManyZones() {
    assertThatThrownBy(() ->
            KilnLogParser.parse(
                "Elapsed,Temp\n0,72\n10,10000.0\n",
                KilnLogSource.OTHER,
                null,
                null,
                CHICAGO,
                TemperatureUnit.F))
        .isInstanceOf(InvalidInputException.class)
        .hasMessageContaining("out of range");

    assertThatThrownBy(() ->
            KilnLogParser.parse(
                "Elapsed,Temp\n0,-5\n10,500\n",
                KilnLogSource.OTHER,
                null,
                null,
                CHICAGO,
                TemperatureUnit.F))
        .isInstanceOf(InvalidInputException.class)
        .hasMessageContaining("out of range");

    assertThatThrownBy(() ->
            KilnLogParser.parse(
                "Elapsed,Temp,Setpoint\n0,72,72\n10,500,12000\n",
                KilnLogSource.OTHER,
                null,
                null,
                CHICAGO,
                TemperatureUnit.F))
        .isInstanceOf(InvalidInputException.class)
        .hasMessageContaining("Setpoint");

    // Duration > 1 year (525,600 minutes) via timestamps
    String multiYear = "Timestamp,Temp\n2025-01-01T00:00:00Z,70\n2027-01-01T00:00:00Z,1800\n";
    assertThatThrownBy(() ->
            KilnLogParser.parse(
                multiYear, KilnLogSource.TAP, null, null, CHICAGO, TemperatureUnit.F))
        .isInstanceOf(InvalidInputException.class)
        .hasMessageContaining("525,600");

    // Duration > 1 year via numeric elapsed
    assertThatThrownBy(() ->
            KilnLogParser.parse(
                "Minutes,Temp\n0,70\n600000,1800\n",
                KilnLogSource.OTHER,
                null,
                null,
                CHICAGO,
                TemperatureUnit.F))
        .isInstanceOf(InvalidInputException.class)
        .hasMessageContaining("525,600");

    // More than 16 zone columns rejected
    StringBuilder seventeenZones = new StringBuilder("Elapsed");
    StringBuilder row = new StringBuilder("0");
    for (int z = 1; z <= 17; z++) {
      seventeenZones.append(",TC").append(z);
      row.append(",700");
    }
    String tooManyZonesCsv = seventeenZones + "\n" + row + "\n";
    assertThatThrownBy(() ->
            KilnLogParser.parse(
                tooManyZonesCsv, KilnLogSource.GENESIS, null, null, CHICAGO, TemperatureUnit.F))
        .isInstanceOf(InvalidInputException.class)
        .hasMessageContaining("16");

    // Dense newline payload aborts incrementally at row cap
    String denseNewlines = "1\n".repeat(60_000);
    assertThatThrownBy(() ->
            KilnLogParser.parse(
                denseNewlines, KilnLogSource.OTHER, null, null, CHICAGO, TemperatureUnit.F))
        .isInstanceOf(InvalidInputException.class)
        .hasMessageContaining("50,000");
  }

  @Test
  void ignoresMultiZoneSetpointsNonChamberTempsAndOmitsIncompleteZoneRows() {
    String csv =
        """
        Elapsed,TC1,TC2,TC3,Zone 1 Setpoint,Zone 2 Setpoint,Zone 3 Setpoint,Board Temp,Ambient Temp,CJ Temp,TC1 Offset
        00:00:00,72,70,74,75,75,75,85,71,72,0
        01:00:00,600,,610,605,605,605,95,72,73,0
        02:00:00,1200,1198,1202,1200,1200,1200,110,73,74,0
        """;

    KilnLogParser.ParsedLog parsed =
        KilnLogParser.parse(csv, KilnLogSource.GENESIS, null, null, CHICAGO, TemperatureUnit.F);

    // Only TC1, TC2, TC3 are counted as kiln chamber zones (not Zone 2/3 Setpoint or
    // Board/Ambient/CJ/Offset)
    assertThat(parsed.zoneCount()).isEqualTo(3);
    assertThat(parsed.peakTemp()).isEqualByComparingTo(new BigDecimal("1200.00"));
    assertThat(parsed.temperatureCurve()).hasSize(3);
    assertThat(parsed.temperatureCurve().get(0).zoneTemps()).containsExactly(72.0, 70.0, 74.0);
    // Partial row (blank TC2) computes average from TC1 & TC3 and omits zoneTemps rather than
    // shifting TC3 into Zone 2.
    assertThat(parsed.temperatureCurve().get(1).temp()).isEqualTo(605.0);
    assertThat(parsed.temperatureCurve().get(1).zoneTemps()).isNull();
    assertThat(parsed.temperatureCurve().get(2).zoneTemps())
        .containsExactly(1200.0, 1198.0, 1202.0);
  }

  @Test
  void handlesLeadingSpaceQuotesPreambleColonMidnightRolloverAndDescendingTimestamps() {
    // Leading space before quoted header + preamble key with trailing colon before comma
    String csv =
        """
        Start Time:,2026-11-10 08:00:00
        Cone:,6
        Elapsed, "Temp, Chamber", "Note, Extra"
        0,72,"ok, fine"
        60,1850,"peak"
        """;

    KilnLogParser.ParsedLog parsed =
        KilnLogParser.parse(csv, KilnLogSource.OTHER, null, null, CHICAGO, TemperatureUnit.F);
    assertThat(parsed.startedAt()).isEqualTo(Instant.parse("2026-11-10T14:00:00Z"));
    assertThat(parsed.finishedAt()).isEqualTo(Instant.parse("2026-11-10T15:00:00Z"));
    assertThat(parsed.detectedCone()).isEqualTo("6");
    assertThat(parsed.peakTemp()).isEqualByComparingTo(new BigDecimal("1850.00"));

    // Small backward clock step (DST fall-back or 1s jitter) does NOT trigger +24h rollover,
    // while genuine midnight crossing (23:30 -> 00:30) does.
    String dstJitterCsv =
        """
        Date: 2026-11-01
        Time,Temp
        01:55:00,500
        01:54:59,510
        01:00:00,600
        02:30:00,1200
        """;
    KilnLogParser.ParsedLog dstParsed =
        KilnLogParser.parse(
            dstJitterCsv, KilnLogSource.OTHER, null, null, CHICAGO, TemperatureUnit.F);
    assertThat(dstParsed.durationMinutes()).isEqualTo(35);

    String midnightCsv =
        """
        Date: 2026-11-10
        Time,Temp
        23:30:00,500
        00:30:00,1200
        """;
    KilnLogParser.ParsedLog midnightParsed =
        KilnLogParser.parse(
            midnightCsv, KilnLogSource.OTHER, null, null, CHICAGO, TemperatureUnit.F);
    assertThat(midnightParsed.durationMinutes()).isEqualTo(60);

    // Strictly descending timestamps are reversed into chronological order so
    // finishedAt >= startedAt
    String descendingCsv =
        """
        Timestamp,Temp (C)
        2026-11-10 17:30,820
        2026-11-10 15:30,1222
        2026-11-10 09:00,22
        """;
    KilnLogParser.ParsedLog reversed =
        KilnLogParser.parse(
            descendingCsv, KilnLogSource.TAP, null, null, LONDON, TemperatureUnit.F);
    assertThat(reversed.startedAt()).isEqualTo(Instant.parse("2026-11-10T09:00:00Z"));
    assertThat(reversed.finishedAt()).isEqualTo(Instant.parse("2026-11-10T17:30:00Z"));
    assertThat(reversed.durationMinutes()).isEqualTo(510);
    assertThat(reversed.temperatureCurve().getFirst().temp()).isEqualTo(22.0);
    assertThat(reversed.temperatureCurve().getLast().temp()).isEqualTo(820.0);

    // Scrambled timestamps where end < start are rejected
    String scrambledCsv =
        """
        Timestamp,Temp (C)
        2026-11-10 10:00,100
        2026-11-10 11:00,500
        2026-11-10 09:00,22
        """;
    assertThatThrownBy(() ->
            KilnLogParser.parse(
                scrambledCsv, KilnLogSource.TAP, null, null, LONDON, TemperatureUnit.F))
        .isInstanceOf(InvalidInputException.class)
        .hasMessageContaining("out of order");
  }

  private static String fixture(String path) throws IOException {
    return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
  }
}
