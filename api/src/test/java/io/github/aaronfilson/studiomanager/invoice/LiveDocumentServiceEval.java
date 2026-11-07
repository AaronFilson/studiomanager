package io.github.aaronfilson.studiomanager.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredentialBuilder;
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
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.json.JsonMapper;

/**
 * The live eval for the platform's readers: reads every eval invoice that has its file with Google
 * Document AI or Azure AI Document Intelligence, records each answer as the case's {@code
 * google.json} or {@code azure.json} (which {@link InvoiceEvalTests} then scores in CI), and writes
 * a dated report into {@code docs/evals}. Each runs only when its credentials are given:
 *
 * <pre>
 * STUDIOMANAGER_EVAL_GOOGLE_TOKEN=$(gcloud auth print-access-token) \
 * STUDIOMANAGER_EVAL_GOOGLE_LOCATION=us \
 * STUDIOMANAGER_EVAL_GOOGLE_PROCESSOR=projects/123/locations/us/processors/abc \
 *   bash scripts/api.sh test --tests '*LiveDocumentServiceEval'
 *
 * az login; STUDIOMANAGER_EVAL_AZURE_ENDPOINT=https://name.cognitiveservices.azure.com \
 *   bash scripts/api.sh test --tests '*LiveDocumentServiceEval'
 * </pre>
 *
 * The Azure one signs in with {@code DefaultAzureCredential}, so whoever runs it needs {@code
 * Cognitive Services User} on the account.
 */
class LiveDocumentServiceEval {

  private static final Map<String, InvoiceFileType> FILES =
      Map.of(
          "invoice.pdf", InvoiceFileType.PDF,
          "invoice.jpg", InvoiceFileType.JPEG,
          "invoice.png", InvoiceFileType.PNG);

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Test
  @EnabledIfEnvironmentVariable(named = "STUDIOMANAGER_EVAL_GOOGLE_TOKEN", matches = ".+")
  void readEveryInvoiceWithGoogle() throws IOException {
    GoogleInvoiceReader reader =
        new GoogleInvoiceReader(
            new GoogleReaderProperties(
                System.getenv("STUDIOMANAGER_EVAL_GOOGLE_LOCATION"),
                System.getenv("STUDIOMANAGER_EVAL_GOOGLE_PROCESSOR"),
                null,
                null,
                null,
                new BigDecimal("0.10")),
            null,
            JSON);
    String token = System.getenv("STUDIOMANAGER_EVAL_GOOGLE_TOKEN");
    run("google", GoogleInvoiceReader.MODEL, file -> reader.readWith(token, file));
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "STUDIOMANAGER_EVAL_AZURE_ENDPOINT", matches = "https://.+")
  void readEveryInvoiceWithAzure() throws IOException {
    AzureInvoiceReader reader =
        new AzureInvoiceReader(
            new AzureReaderProperties(
                System.getenv("STUDIOMANAGER_EVAL_AZURE_ENDPOINT"), BigDecimal.ZERO),
            null,
            JSON);
    String token =
        new DefaultAzureCredentialBuilder()
            .build()
            .getTokenSync(
                new TokenRequestContext().addScopes("https://cognitiveservices.azure.com/.default"))
            .getToken();
    run("azure", AzureInvoiceReader.MODEL, file -> reader.readWith(token, file));
  }

  private static void run(String name, String model, Function<InvoiceFile, ReaderResult> read)
      throws IOException {
    List<String> rows = new ArrayList<>();
    List<InvoiceScorer.Score> scores = new ArrayList<>();
    BigDecimal cost = BigDecimal.ZERO;
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
      ReaderResult result = read.apply(new InvoiceFile(content, file.get().getValue()));
      long elapsed = Duration.ofNanos(System.nanoTime() - started).toMillis();
      Files.writeString(folder.resolve(name + ".json"), result.raw());
      InvoiceScorer.Score score =
          InvoiceScorer.score(
              InvoiceEvalTests.expected(folder),
              InvoiceDrafts.sanitize(result.draft(), LocalDate.now()).draft());
      scores.add(score);
      BigDecimal spent =
          result.usage().costUsd() == null ? BigDecimal.ZERO : result.usage().costUsd();
      cost = cost.add(spent);
      rows.add(
          "| %s | %d/%d | %d/%d | %s | %.1f s | %d | $%s |"
              .formatted(
                  folder.getFileName(),
                  score.fieldsRight(),
                  score.fieldsTotal(),
                  score.linesRight(),
                  score.linesTotal(),
                  score.needsNoEdits() ? "yes" : "no",
                  elapsed / 1000.0,
                  result.usage().pages(),
                  spent.setScale(4, RoundingMode.HALF_UP).toPlainString()));
    }
    assertThat(scores).as("eval invoices with a file").isNotEmpty();
    String report =
        """
        # Invoice eval: %s (%s), %s

        Written by `LiveDocumentServiceEval`, scored like the Claude eval (docs/planning/16,
        "Evaluation").

        - **Field accuracy:** %.1f%%
        - **Line accuracy:** %.1f%%
        - **Cost:** $%s in all, from the configured prices

        | Case | Fields | Lines | No edits | Time | Pages | Cost |
        |---|---|---|---|---|---|---|
        %s
        """
            .formatted(
                name,
                model,
                LocalDate.now(),
                100 * InvoiceEvalTests.fieldAccuracy(scores),
                100 * InvoiceEvalTests.lineAccuracy(scores),
                cost.setScale(4, RoundingMode.HALF_UP).toPlainString(),
                String.join("\n", rows));
    Path reports = Path.of("../docs/evals");
    Files.createDirectories(reports);
    Files.writeString(
        reports.resolve("%s-invoices-%s.md".formatted(LocalDate.now(), name)), report);
  }
}
