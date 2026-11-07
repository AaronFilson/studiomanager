package io.github.aaronfilson.studiomanager.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * The invoice eval, run in CI against recorded answers, so it's free and repeatable (docs/planning
 * /16, "Evaluation"). Each folder under {@code src/test/resources/invoices/eval} is one invoice:
 *
 * <ul>
 *   <li>{@code expected.json}: the hand-written answer, in Claude's answer shape;
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

  /** The synthetic set's known scores; real invoices from the owner will set the real floor. */
  private static final double FIELD_FLOOR = 0.85;

  private static final double LINE_FLOOR = 0.85;

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Test
  void recordedClaudeAnswersScoreAtLeastTheFloor() throws IOException {
    List<InvoiceScorer.Score> scores = new ArrayList<>();
    StringBuilder report = new StringBuilder("case | fields | lines | no edits\n");
    for (Path folder : cases()) {
      Path recorded = folder.resolve("claude.json");
      if (!Files.exists(recorded)) {
        continue;
      }
      InvoiceScorer.Score score = score(folder, recorded);
      scores.add(score);
      report.append(
          "%s | %d/%d | %d/%d | %s%n"
              .formatted(
                  folder.getFileName(),
                  score.fieldsRight(),
                  score.fieldsTotal(),
                  score.linesRight(),
                  score.linesTotal(),
                  score.needsNoEdits()));
    }
    System.out.print(report);

    assertThat(scores).isNotEmpty();
    assertThat(fieldAccuracy(scores)).isGreaterThanOrEqualTo(FIELD_FLOOR);
    assertThat(lineAccuracy(scores)).isGreaterThanOrEqualTo(LINE_FLOOR);
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
  void casingAndPunctuationInTextDoNotCount() throws IOException {
    Path folder = CASES.resolve("synthetic-clay-order");

    assertThat(score(folder, folder.resolve("claude.json")).needsNoEdits()).isTrue();
  }

  static List<Path> cases() throws IOException {
    try (Stream<Path> folders = Files.list(CASES)) {
      return folders.filter(Files::isDirectory).sorted().toList();
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
    InvoiceDraft read = InvoiceDrafts.sanitize(draft, LocalDate.of(2026, 11, 6)).draft();
    return InvoiceScorer.score(expected(folder), read);
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
