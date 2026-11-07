package io.github.aaronfilson.studiomanager.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * report into {@code docs/evals}. It spends real tokens, so it runs only when asked:
 *
 * <pre>
 * STUDIOMANAGER_EVAL_ANTHROPIC_KEY=sk-ant-... bash scripts/api.sh test --tests '*LiveInvoiceEval'
 * </pre>
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

  @Test
  void readEveryInvoiceAndWriteTheReport() throws IOException {
    JsonMapper json = JsonMapper.builder().build();
    ClaudeInvoiceReader reader =
        new ClaudeInvoiceReader(
            null, null, json, MODEL, new BigDecimal("4.00"), new BigDecimal("20.00"));
    List<String> rows = new ArrayList<>();
    List<InvoiceScorer.Score> scores = new ArrayList<>();
    List<Long> millis = new ArrayList<>();
    BigDecimal cost = BigDecimal.ZERO;
    AnthropicClient client =
        AnthropicOkHttpClient.builder()
            .apiKey(System.getenv("STUDIOMANAGER_EVAL_ANTHROPIC_KEY"))
            .timeout(Duration.ofMinutes(3))
            .build();
    try {
      for (Path folder : InvoiceEvalTests.cases()) {
        Optional<Map.Entry<String, InvoiceFileType>> file =
            FILES.entrySet().stream()
                .filter(entry -> Files.exists(folder.resolve(entry.getKey())))
                .findFirst();
        if (file.isEmpty()) {
          continue;
        }
        byte[] content = Files.readAllBytes(folder.resolve(file.get().getKey()));
        long started = System.nanoTime();
        ReaderResult result =
            reader.readWith(client, new InvoiceFile(content, file.get().getValue()));
        long elapsed = Duration.ofNanos(System.nanoTime() - started).toMillis();
        Files.writeString(folder.resolve("claude.json"), result.raw());
        InvoiceScorer.Score score =
            InvoiceScorer.score(
                InvoiceEvalTests.expected(folder),
                InvoiceDrafts.sanitize(result.draft(), LocalDate.now()).draft());
        scores.add(score);
        millis.add(elapsed);
        BigDecimal spent =
            result.usage().costUsd() == null ? BigDecimal.ZERO : result.usage().costUsd();
        cost = cost.add(spent);
        rows.add(
            "| %s | %d/%d | %d/%d | %s | %.1f s | $%s |"
                .formatted(
                    folder.getFileName(),
                    score.fieldsRight(),
                    score.fieldsTotal(),
                    score.linesRight(),
                    score.linesTotal(),
                    score.needsNoEdits() ? "yes" : "no",
                    elapsed / 1000.0,
                    spent.setScale(4, RoundingMode.HALF_UP).toPlainString()));
      }
    } finally {
      client.close();
    }
    assertThat(scores).as("eval invoices with a file").isNotEmpty();
    writeReport(rows, scores, millis, cost);
  }

  private static void writeReport(
      List<String> rows, List<InvoiceScorer.Score> scores, List<Long> millis, BigDecimal cost)
      throws IOException {
    List<Long> sorted = millis.stream().sorted().toList();
    long median = sorted.get(sorted.size() / 2);
    long noEdits = scores.stream().filter(InvoiceScorer.Score::needsNoEdits).count();
    String report =
        """
        # Invoice eval: Claude (%s), %s

        Written by `LiveInvoiceEval`. Scores: numbers and dates exact, text normalized; a line is
        right when its quantity, unit price and total all match (docs/planning/16, "Evaluation").

        - **Field accuracy:** %.1f%%
        - **Line accuracy:** %.1f%%
        - **Needs no edits:** %d of %d
        - **Median time per invoice:** %.1f s
        - **Cost:** $%s in all, from the configured prices

        | Case | Fields | Lines | No edits | Time | Cost |
        |---|---|---|---|---|---|
        %s
        """
            .formatted(
                MODEL,
                LocalDate.now(),
                100 * InvoiceEvalTests.fieldAccuracy(scores),
                100 * InvoiceEvalTests.lineAccuracy(scores),
                noEdits,
                scores.size(),
                median / 1000.0,
                cost.setScale(4, RoundingMode.HALF_UP).toPlainString(),
                String.join("\n", rows));
    Path folder = Path.of("../docs/evals");
    Files.createDirectories(folder);
    Files.writeString(folder.resolve("%s-invoices-claude.md".formatted(LocalDate.now())), report);
  }
}
