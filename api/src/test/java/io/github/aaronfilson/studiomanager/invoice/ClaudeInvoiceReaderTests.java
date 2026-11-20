package io.github.aaronfilson.studiomanager.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** The request Claude gets: one schema, one effort, and the date a year-less date is read from. */
class ClaudeInvoiceReaderTests {

  private static final InvoiceFile PDF =
      new InvoiceFile("%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII), InvoiceFileType.PDF);

  @Test
  void theRequestCarriesTheSchemaTheEffortAndTheUploadDate() {
    ClaudeInvoiceReader reader = reader("high");

    MessageCreateParams params = reader.params(PDF, LocalDate.of(2026, 11, 16));

    OutputConfig config = params.outputConfig().orElseThrow();
    assertThat(config.effort()).contains(OutputConfig.Effort.HIGH);
    assertThat(config.format()).isPresent();
    String system = params.system().orElseThrow().toString();
    assertThat(system).contains("crossed out", "documentType", "payments", "rotation");
    assertThat(params.messages().getFirst().toString()).contains("uploaded on 2026-11-16");
  }

  @Test
  void theEffortIsASetting() {
    assertThat(reader("medium").params(PDF, LocalDate.of(2026, 1, 2)).outputConfig())
        .hasValueSatisfying(
            config -> assertThat(config.effort()).contains(OutputConfig.Effort.MEDIUM));
  }

  private static ClaudeInvoiceReader reader(String effort) {
    return new ClaudeInvoiceReader(
        null,
        null,
        JsonMapper.builder().build(),
        "claude-opus-5-5",
        effort,
        new BigDecimal("4.00"),
        new BigDecimal("20.00"),
        Clock.systemUTC());
  }
}
