package io.github.aaronfilson.studiomanager.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.json.JsonMapper;

/**
 * The live eval: reads every eval invoice that has its file with Claude, records each answer as the
 * case's {@code claude.json} (which {@link InvoiceEvalTests} then scores in CI), and writes a dated
 * report. It spends real tokens, so it runs only when asked:
 *
 * <pre>
 * STUDIOMANAGER_EVAL_ANTHROPIC_KEY=sk-ant-... [STUDIOMANAGER_EVAL_MODEL=claude-opus-5-5] \
 *   [STUDIOMANAGER_EVAL_EFFORT=high] bash scripts/api.sh test --tests '*LiveInvoiceEval'
 * </pre>
 *
 * <p>Two sets (docs/planning/24, "Two sets"): the public cases in {@code invoices/eval}, reported
 * per case in {@code docs/evals}; and the owner's own in {@code invoices/private}, which git
 * ignores. Their per-case report stays in that folder, and the public report gives only their
 * totals, with no case names.
 */
@EnabledIfEnvironmentVariable(named = "STUDIOMANAGER_EVAL_ANTHROPIC_KEY", matches = "sk-ant-.+")
class LiveInvoiceEval {

  private static final Map<String, InvoiceFileType> FILES =
      Map.of(
          "invoice.pdf", InvoiceFileType.PDF,
          "invoice.jpg", InvoiceFileType.JPEG,
          "invoice.png", InvoiceFileType.PNG);

  private static final String MODEL =
      Optional.ofNullable(System.getenv("STUDIOMANAGER_EVAL_MODEL")).orElse("claude-opus-5-5");

  private static final String EFFORT =
      Optional.ofNullable(System.getenv("STUDIOMANAGER_EVAL_EFFORT")).orElse("medium");

  /** One set's results. */
  private record Run(
      List<String> rows, List<InvoiceScorer.Score> scores, List<Long> millis, BigDecimal cost) {}

  @Test
  void readEveryInvoiceAndWriteTheReport() throws IOException {
    JsonMapper json = JsonMapper.builder().build();
    ClaudeInvoiceReader reader =
        new ClaudeInvoiceReader(
            null,
            null,
            json,
            MODEL,
            EFFORT,
            new BigDecimal("4.00"),
            new BigDecimal("20.00"),
            Clock.systemUTC());
    AnthropicClient client =
        AnthropicOkHttpClient.builder()
            .apiKey(System.getenv("STUDIOMANAGER_EVAL_ANTHROPIC_KEY"))
            .timeout(Duration.ofMinutes(3))
            .build();
    Run publicRun;
    Run privateRun;
    try {
      publicRun = run(reader, client, InvoiceEvalTests.cases());
      privateRun = run(reader, client, InvoiceEvalTests.privateCases());
    } finally {
      client.close();
    }
    assertThat(publicRun.scores().size() + privateRun.scores().size())
        .as("eval invoices with a file")
        .isPositive();
    String title = "%s, effort %s, %s".formatted(MODEL, EFFORT, LocalDate.now());
    String name = "%s-invoices-claude-%s-%s.md".formatted(LocalDate.now(), MODEL, EFFORT);
    if (!privateRun.scores().isEmpty()) {
      Path reports = InvoiceEvalTests.PRIVATE_CASES.resolve("reports");
      Files.createDirectories(reports);
      Files.writeString(reports.resolve(name), report(title + " (private set)", privateRun, ""));
    }
    if (!publicRun.scores().isEmpty()) {
      String privateTotals =
          privateRun.scores().isEmpty()
              ? ""
              : """

              The owner's private set (%d invoices, not named here): field accuracy %.1f%%, line
              accuracy %.1f%%, %d need no edits.
              """
                  .formatted(
                      privateRun.scores().size(),
                      100 * InvoiceEvalTests.fieldAccuracy(privateRun.scores()),
                      100 * InvoiceEvalTests.lineAccuracy(privateRun.scores()),
                      privateRun.scores().stream()
                          .filter(InvoiceScorer.Score::needsNoEdits)
                          .count());
      Path folder = Path.of("../docs/evals");
      Files.createDirectories(folder);
      Files.writeString(folder.resolve(name), report(title, publicRun, privateTotals));
    }
  }

  private static Run run(ClaudeInvoiceReader reader, AnthropicClient client, List<Path> cases)
      throws IOException {
    List<String> rows = new ArrayList<>();
    List<InvoiceScorer.Score> scores = new ArrayList<>();
    List<Long> millis = new ArrayList<>();
    BigDecimal cost = BigDecimal.ZERO;
    for (Path folder : cases) {
      Optional<Map.Entry<String, InvoiceFileType>> file =
          FILES.entrySet().stream()
              .filter(entry -> Files.exists(folder.resolve(entry.getKey())))
              .findFirst();
      if (file.isEmpty()) {
        continue;
      }
      byte[] content = Files.readAllBytes(folder.resolve(file.get().getKey()));
      long started = System.nanoTime();
      ReaderResult result;
      try {
        result = reader.readWith(client, new InvoiceFile(content, file.get().getValue()));
      } catch (InvoiceReadException e) {
        rows.add("| %s | failed: %s |||||".formatted(folder.getFileName(), e.getMessage()));
        continue;
      }
      long elapsed = Duration.ofNanos(System.nanoTime() - started).toMillis();
      Files.writeString(folder.resolve("claude.json"), result.raw());
      InvoiceDraft read = InvoiceDrafts.sanitize(result.draft(), LocalDate.now()).draft();
      InvoiceScorer.Score score = InvoiceScorer.score(InvoiceEvalTests.expected(folder), read);
      scores.add(score);
      millis.add(elapsed);
      BigDecimal spent =
          result.usage().costUsd() == null ? BigDecimal.ZERO : result.usage().costUsd();
      cost = cost.add(spent);
      rows.add(
          "| %s | %d/%d | %d/%d | %s | %d | %.1f s | $%s |"
              .formatted(
                  folder.getFileName(),
                  score.fieldsRight(),
                  score.fieldsTotal(),
                  score.linesRight(),
                  score.linesTotal(),
                  score.needsNoEdits() ? "yes" : "no",
                  InvoiceChecks.problems(read).size(),
                  elapsed / 1000.0,
                  spent.setScale(4, RoundingMode.HALF_UP).toPlainString()));
    }
    return new Run(rows, scores, millis, cost);
  }

  private static String report(String title, Run run, String privateTotals) {
    List<Long> sorted = run.millis().stream().sorted().toList();
    long median = sorted.isEmpty() ? 0 : sorted.get(sorted.size() / 2);
    long noEdits = run.scores().stream().filter(InvoiceScorer.Score::needsNoEdits).count();
    return """
    # Invoice eval: Claude (%s)

    Written by `LiveInvoiceEval`. Scores: numbers and dates exact, text normalized; a line is
    right when its quantity, unit price and total all match (docs/planning/16, "Evaluation").
    "Problems" is how many messages the checks raised on what was read.

    - **Field accuracy:** %.1f%%
    - **Line accuracy:** %.1f%%
    - **Needs no edits:** %d of %d
    - **Median time per invoice:** %.1f s
    - **Cost:** $%s in all, from the configured prices
    %s
    | Case | Fields | Lines | No edits | Problems | Time | Cost |
    |---|---|---|---|---|---|---|
    %s
    """
        .formatted(
            title,
            100 * InvoiceEvalTests.fieldAccuracy(run.scores()),
            100 * InvoiceEvalTests.lineAccuracy(run.scores()),
            noEdits,
            run.scores().size(),
            median / 1000.0,
            run.cost().setScale(4, RoundingMode.HALF_UP).toPlainString(),
            privateTotals,
            String.join("\n", run.rows()));
  }
}
