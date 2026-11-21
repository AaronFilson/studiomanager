package io.github.aaronfilson.studiomanager.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * The invoice eval, run in CI against recorded answers, so it's free and repeatable (docs/planning
 * /16, "Evaluation"; v2 fields and splits in docs/planning/24). Each folder under {@code
 * src/test/resources/invoices/eval} is one invoice:
 *
 * <ul>
 *   <li>{@code expected.json}: the hand-written (or generated) answer, in Claude's v1/v2 shape;
 *   <li>{@code case.json}: source, license, tags and split ({@code dev} or {@code test});
 *   <li>{@code claude.json}: what Claude answered, recorded by {@code LiveInvoiceEval};
 *   <li>{@code google.json}, {@code azure.json}: what Google Document AI and Azure AI Document
 *       Intelligence answered, recorded by {@code LiveDocumentServiceEval};
 *   <li>{@code invoice.pdf}, {@code .jpg} or {@code .png}: the invoice itself, for the live eval.
 * </ul>
 *
 * <p>Each recorded answer goes through the same mapping and gate as a live one, and is scored with
 * {@link InvoiceScorer}. A change to the mapping, the gate or the scorer shows up here as a change
 * in accuracy.
 */
class InvoiceEvalTests {

  static final Path CASES = Path.of("src/test/resources/invoices/eval");

  /** Known floors per split; real invoices from the owner tighten these via LiveInvoiceEval. */
  private static final double FIELD_FLOOR = 0.85;

  private static final double LINE_FLOOR = 0.85;

  private static final JsonMapper JSON = JsonMapper.builder().build();

  /** Metadata in {@code case.json}. Defaults to {@code dev} if a case predates splits. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record CaseMeta(
      @Nullable String source,
      @Nullable String license,
      @Nullable List<String> tags,
      @Nullable String split) {

    String effectiveSplit() {
      return "test".equalsIgnoreCase(split) ? "test" : "dev";
    }
  }

  @Test
  void recordedClaudeAnswersScoreAtLeastTheFloorPerSplit() throws IOException {
    List<InvoiceScorer.Score> allScores = new ArrayList<>();
    Map<String, List<InvoiceScorer.Score>> bySplit = new LinkedHashMap<>();
    bySplit.put("test", new ArrayList<>());
    bySplit.put("dev", new ArrayList<>());
    List<InvoiceScorer.Checked> checks = new ArrayList<>();
    StringBuilder report =
        new StringBuilder("case | split | fields | lines | no edits | false alarms | missed\n");
    for (Path folder : cases()) {
      Path recorded = folder.resolve("claude.json");
      if (!Files.exists(recorded)) {
        continue;
      }
      String split = caseMeta(folder).effectiveSplit();
      InvoiceDraft expected = expected(folder);
      InvoiceDraft read = read(answer(recorded).toDraft());
      InvoiceScorer.Score score = InvoiceScorer.score(expected, read);
      InvoiceScorer.Checked checked = InvoiceScorer.checks(expected, read, score);
      allScores.add(score);
      bySplit.computeIfAbsent(split, ignored -> new ArrayList<>()).add(score);
      checks.add(checked);
      report.append(
          "%s | %s | %d/%d | %d/%d | %s | %d | %s%n"
              .formatted(
                  folder.getFileName(),
                  split,
                  score.fieldsRight(),
                  score.fieldsTotal(),
                  score.linesRight(),
                  score.linesTotal(),
                  score.needsNoEdits(),
                  checked.falseAlarms(),
                  checked.missed()));
    }
    for (Map.Entry<String, List<InvoiceScorer.Score>> entry : bySplit.entrySet()) {
      report.append(
          "split=%s (%d cases): fields=%.1f%% lines=%.1f%%%n"
              .formatted(
                  entry.getKey(),
                  entry.getValue().size(),
                  100 * fieldAccuracy(entry.getValue()),
                  100 * lineAccuracy(entry.getValue())));
    }
    System.out.print(report);

    assertThat(allScores).isNotEmpty();
    assertThat(bySplit.get("dev")).isNotEmpty();
    assertThat(bySplit.get("test")).isNotEmpty();
    assertThat(fieldAccuracy(allScores)).isGreaterThanOrEqualTo(FIELD_FLOOR);
    assertThat(lineAccuracy(allScores)).isGreaterThanOrEqualTo(LINE_FLOOR);
    for (Map.Entry<String, List<InvoiceScorer.Score>> entry : bySplit.entrySet()) {
      assertThat(fieldAccuracy(entry.getValue()))
          .as("field accuracy for split %s", entry.getKey())
          .isGreaterThanOrEqualTo(FIELD_FLOOR);
      assertThat(lineAccuracy(entry.getValue()))
          .as("line accuracy for split %s", entry.getKey())
          .isGreaterThanOrEqualTo(LINE_FLOOR);
    }
    // What staff are told: no problem raised on a right draft, and a wrong one always says so
    assertThat(checks)
        .allSatisfy(checked -> {
          assertThat(checked.falseAlarms()).isZero();
          assertThat(checked.missed()).isFalse();
        });
  }

  @Test
  void v2FieldsAreScoredWhenPresentInExpected() {
    InvoiceDraft expected =
        new InvoiceDraft(
            InvoiceDraft.DocumentType.ORDER_FORM,
            "Cascade Clay",
            null,
            "CTR-1",
            LocalDate.of(2026, 10, 18),
            "10/18",
            InvoiceDraft.DateYearSource.FROM_PAGE,
            null,
            new BigDecimal("100.00"),
            null,
            new BigDecimal("8.00"),
            new BigDecimal("60.00"),
            new BigDecimal("108.00"),
            BigDecimal.ZERO,
            List.of(
                new InvoiceDraft.Payment(
                    LocalDate.of(2026, 10, 18),
                    new BigDecimal("60.00"),
                    InvoiceDraft.PaymentMethod.CARD,
                    InvoiceDraft.PaymentSource.SLIP),
                new InvoiceDraft.Payment(
                    LocalDate.of(2026, 10, 19),
                    new BigDecimal("48.00"),
                    InvoiceDraft.PaymentMethod.CARD,
                    InvoiceDraft.PaymentSource.RECEIPT)),
            List.of(),
            null,
            List.of(),
            90,
            List.of(
                new InvoiceDraft.Line(
                    "Fireclay",
                    null,
                    new BigDecimal("100"),
                    "lb",
                    null,
                    new BigDecimal("80.00"),
                    InvoiceDraft.LineKind.ITEM,
                    List.of(),
                    false),
                new InvoiceDraft.Line(
                    "Freight",
                    null,
                    null,
                    null,
                    null,
                    new BigDecimal("20.00"),
                    InvoiceDraft.LineKind.SHIPPING,
                    List.of(),
                    false)));

    InvoiceScorer.Score exact = InvoiceScorer.score(expected, expected);
    // 7 core + documentType + invoiceDateYear + payments = 10
    assertThat(exact.fieldsTotal()).isEqualTo(10);
    assertThat(exact.fieldsRight()).isEqualTo(10);
    assertThat(exact.linesRight()).isEqualTo(2);
    assertThat(exact.needsNoEdits()).isTrue();
    assertThat(InvoiceScorer.checks(expected, expected, exact))
        .isEqualTo(new InvoiceScorer.Checked(0, false));

    // Wrong documentType, assumed year, missing payments, wrong line kind
    InvoiceDraft wrongV2 =
        new InvoiceDraft(
            InvoiceDraft.DocumentType.INVOICE,
            "Cascade Clay",
            null,
            "CTR-1",
            LocalDate.of(2026, 10, 18),
            "10/18",
            InvoiceDraft.DateYearSource.ASSUMED,
            null,
            new BigDecimal("100.00"),
            null,
            new BigDecimal("8.00"),
            new BigDecimal("60.00"),
            null,
            null,
            List.of(),
            List.of(),
            null,
            List.of(),
            0,
            List.of(
                new InvoiceDraft.Line(
                    "Fireclay",
                    null,
                    new BigDecimal("100"),
                    "lb",
                    null,
                    new BigDecimal("80.00"),
                    InvoiceDraft.LineKind.ITEM,
                    List.of(),
                    false),
                new InvoiceDraft.Line(
                    "Freight",
                    null,
                    null,
                    null,
                    null,
                    new BigDecimal("20.00"),
                    InvoiceDraft.LineKind.ITEM,
                    List.of(),
                    false)));
    InvoiceScorer.Score mismatch = InvoiceScorer.score(expected, wrongV2);
    assertThat(mismatch.fieldsTotal()).isEqualTo(10);
    assertThat(mismatch.fieldsRight()).isEqualTo(7);
    assertThat(mismatch.linesRight()).isEqualTo(1);
    assertThat(mismatch.needsNoEdits()).isFalse();
  }

  @Test
  void v2HallucinatedPaymentsArePenalized() {
    InvoiceDraft expected =
        new InvoiceDraft(
            InvoiceDraft.DocumentType.INVOICE,
            "Cascade Clay",
            "INV-42",
            null,
            LocalDate.of(2026, 10, 18),
            null,
            null,
            null,
            new BigDecimal("100.00"),
            null,
            null,
            new BigDecimal("100.00"),
            null,
            null,
            List.of(),
            List.of(),
            null,
            List.of(),
            0,
            List.of(
                new InvoiceDraft.Line(
                    "Fireclay",
                    null,
                    new BigDecimal("100"),
                    "lb",
                    null,
                    new BigDecimal("100.00"),
                    InvoiceDraft.LineKind.ITEM,
                    List.of(),
                    false)));

    InvoiceScorer.Score clean = InvoiceScorer.score(expected, expected);
    // 7 core + documentType = 8 (empty payments on both sides don't inflate fieldsTotal)
    assertThat(clean.fieldsTotal()).isEqualTo(8);
    assertThat(clean.fieldsRight()).isEqualTo(8);
    assertThat(clean.needsNoEdits()).isTrue();

    InvoiceDraft hallucinated =
        new InvoiceDraft(
            InvoiceDraft.DocumentType.INVOICE,
            "Cascade Clay",
            "INV-42",
            null,
            LocalDate.of(2026, 10, 18),
            null,
            null,
            null,
            new BigDecimal("100.00"),
            null,
            null,
            new BigDecimal("100.00"),
            null,
            null,
            List.of(
                new InvoiceDraft.Payment(
                    LocalDate.of(2026, 10, 18),
                    new BigDecimal("100.00"),
                    InvoiceDraft.PaymentMethod.CARD,
                    InvoiceDraft.PaymentSource.INVOICE)),
            List.of(),
            null,
            List.of(),
            0,
            expected.lines());

    InvoiceScorer.Score penalized = InvoiceScorer.score(expected, hallucinated);
    assertThat(penalized.fieldsTotal()).isEqualTo(9);
    assertThat(penalized.fieldsRight()).isEqualTo(8);
    assertThat(penalized.needsNoEdits()).isFalse();
  }

  @Test
  void committedSyntheticCasesCoverPackingListAndSplitPayments() {
    InvoiceDraft counter = expected(CASES.resolve("synth-handwritten-counter-order"));
    assertThat(counter.documentType()).isEqualTo(InvoiceDraft.DocumentType.ORDER_FORM);
    assertThat(counter.payableAmount()).isEqualByComparingTo("177.70");
    assertThat(InvoiceChecks.problems(counter)).isEmpty();
    assertThat(InvoiceChecks.explanations(counter)).hasSize(1);

    InvoiceDraft packing = expected(CASES.resolve("synth-packing-list"));
    assertThat(packing.documentType()).isEqualTo(InvoiceDraft.DocumentType.PACKING_LIST);
    assertThat(InvoiceChecks.problems(packing)).isEmpty();
  }

  @Test
  void theChecksScoreFalseAlarmsAndMisses() {
    InvoiceDraft right =
        draft(
            InvoiceChecksTests.line("Fireclay", "1", "17.50", "17.50"),
            InvoiceChecksTests.line("Dolomite", "1", "32.00", "32.00"),
            InvoiceChecksTests.line("Talc", "1", "21.50", "21.50"));
    // The written total covers only part of it: a real problem, on the expected draft too
    InvoiceDraft partPaid =
        new InvoiceDraft(
            "Clay Co", null, null, null, null, null, new BigDecimal("40.00"), right.lines());

    assertThat(InvoiceScorer.checks(partPaid, partPaid, InvoiceScorer.score(partPaid, partPaid)))
        .isEqualTo(new InvoiceScorer.Checked(0, false));

    InvoiceDraft noTotal =
        new InvoiceDraft("Clay Co", null, null, null, null, null, null, right.lines());
    InvoiceDraft wrongQuantity =
        draft(
            InvoiceChecksTests.line("Fireclay", "2", "8.75", "17.50"),
            right.lines().get(1),
            right.lines().get(2));

    // A missing total on an otherwise right read is flagged, but it isn't right, so not an alarm
    assertThat(InvoiceScorer.checks(right, noTotal, InvoiceScorer.score(right, noTotal)).missed())
        .isFalse();
    // A wrong quantity whose arithmetic still adds up raises nothing: a miss
    assertThat(
            InvoiceScorer.checks(right, wrongQuantity, InvoiceScorer.score(right, wrongQuantity))
                .missed())
        .isTrue();
  }

  /**
   * Google's and Azure's recorded answers ({@code google.json}, {@code azure.json}), through the
   * same gate and scorer as Claude's. Azure's invoice model has no shipping field, so it can't
   * score that one.
   */
  @Test
  void recordedDocumentServiceAnswersScoreAtLeastTheFloor() throws IOException {
    GoogleInvoiceReader google = DocumentServiceReaderTests.google();
    AzureInvoiceReader azure = DocumentServiceReaderTests.azure();
    List<InvoiceScorer.Score> googleScores = new ArrayList<>();
    List<InvoiceScorer.Score> azureScores = new ArrayList<>();
    for (Path folder : cases()) {
      Path googleAnswer = folder.resolve("google.json");
      if (Files.exists(googleAnswer)) {
        googleScores.add(
            scoreDraft(folder, google.toResult(Files.readString(googleAnswer)).draft()));
      }
      Path azureAnswer = folder.resolve("azure.json");
      if (Files.exists(azureAnswer)) {
        azureScores.add(
            scoreDraft(folder, azure.toResult(azure.parse(Files.readString(azureAnswer))).draft()));
      }
    }

    assertThat(googleScores).isNotEmpty();
    assertThat(azureScores).isNotEmpty();
    assertThat(fieldAccuracy(googleScores)).isGreaterThanOrEqualTo(FIELD_FLOOR);
    assertThat(lineAccuracy(googleScores)).isGreaterThanOrEqualTo(LINE_FLOOR);
    assertThat(fieldAccuracy(azureScores)).isGreaterThanOrEqualTo(FIELD_FLOOR);
    assertThat(lineAccuracy(azureScores)).isGreaterThanOrEqualTo(LINE_FLOOR);
  }

  @Test
  void theScorerCountsAMissedFieldAndAWrongLine() throws IOException {
    Path folder = CASES.resolve("synthetic-glaze-materials");

    InvoiceScorer.Score score = score(folder, folder.resolve("claude.json"));

    // Shipping and tax were left blank, and the discount line lost its quantity and price
    assertThat(score.fieldsRight()).isEqualTo(5);
    assertThat(score.linesRight()).isEqualTo(4);
    assertThat(score.linesTotal()).isEqualTo(5);
    assertThat(score.needsNoEdits()).isFalse();
  }

  @Test
  void aMissedLineCostsOneLineNotEveryLineAfterIt() {
    InvoiceDraft expected =
        draft(
            InvoiceChecksTests.line("Fireclay", "1", "17.50", "17.50"),
            InvoiceChecksTests.line("Dolomite", "1", "32.00", "32.00"),
            InvoiceChecksTests.line("Talc", "1", "21.50", "21.50"));
    InvoiceDraft read =
        draft(
            InvoiceChecksTests.line("Fireclay", "1", "17.50", "17.50"),
            InvoiceChecksTests.line("Talc", "1", "21.50", "21.50"));

    InvoiceScorer.Score score = InvoiceScorer.score(expected, read);

    assertThat(score.linesRight()).isEqualTo(2);
    assertThat(score.linesTotal()).isEqualTo(3);
    assertThat(score.needsNoEdits()).isFalse();
  }

  private static InvoiceDraft draft(InvoiceDraft.Line... lines) {
    return new InvoiceDraft(
        "Clay Co", null, null, null, null, null, new BigDecimal("71.00"), List.of(lines));
  }

  @Test
  void casingAndPunctuationInTextDoNotCount() throws IOException {
    Path folder = CASES.resolve("synthetic-clay-order");

    assertThat(score(folder, folder.resolve("claude.json")).needsNoEdits()).isTrue();
  }

  /**
   * The owner's own invoices, which git ignores as a whole: absent in CI and on any other checkout.
   * Only the live evals read them (docs/planning/24, "Two sets").
   */
  static final Path PRIVATE_CASES = Path.of("src/test/resources/invoices/private");

  static List<Path> cases() throws IOException {
    return casesIn(CASES);
  }

  static List<Path> privateCases() throws IOException {
    return Files.isDirectory(PRIVATE_CASES) ? casesIn(PRIVATE_CASES) : List.of();
  }

  static CaseMeta caseMeta(Path folder) {
    Path file = folder.resolve("case.json");
    if (!Files.exists(file)) {
      return new CaseMeta(null, null, List.of(), "dev");
    }
    return JSON.readValue(file.toFile(), CaseMeta.class);
  }

  private static List<Path> casesIn(Path root) throws IOException {
    try (Stream<Path> folders = Files.list(root)) {
      return folders
          .filter(Files::isDirectory)
          .filter(folder -> Files.exists(folder.resolve("expected.json")))
          .sorted()
          .toList();
    }
  }

  static InvoiceDraft expected(Path folder) {
    return answer(folder.resolve("expected.json")).toDraft();
  }

  static ClaudeInvoiceAnswer answer(Path file) {
    return JSON.readValue(file.toFile(), ClaudeInvoiceAnswer.class);
  }

  private static InvoiceScorer.Score score(Path folder, Path recorded) {
    return scoreDraft(folder, answer(recorded).toDraft());
  }

  private static InvoiceScorer.Score scoreDraft(Path folder, InvoiceDraft draft) {
    return InvoiceScorer.score(expected(folder), read(draft));
  }

  /** A recorded answer through the same gate as a live one. */
  private static InvoiceDraft read(InvoiceDraft draft) {
    return InvoiceDrafts.sanitize(draft, LocalDate.of(2026, 11, 6)).draft();
  }

  static double fieldAccuracy(List<InvoiceScorer.Score> scores) {
    int right = scores.stream().mapToInt(InvoiceScorer.Score::fieldsRight).sum();
    int total = scores.stream().mapToInt(InvoiceScorer.Score::fieldsTotal).sum();
    return total == 0 ? 1 : (double) right / total;
  }

  static double lineAccuracy(List<InvoiceScorer.Score> scores) {
    int right = scores.stream().mapToInt(InvoiceScorer.Score::linesRight).sum();
    int total = scores.stream().mapToInt(InvoiceScorer.Score::linesTotal).sum();
    return total == 0 ? 1 : (double) right / total;
  }
}
