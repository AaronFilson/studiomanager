package io.github.aaronfilson.studiomanager.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.databind.json.JsonMapper;

/**
 * The Google Document AI and Azure AI Document Intelligence mappings, on answers recorded in the
 * services' own shapes ({@code google.json} and {@code azure.json} in an eval case).
 */
class DocumentServiceReaderTests {

  private static final Path CLAY = InvoiceEvalTests.CASES.resolve("synthetic-clay-order");
  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Test
  void googleEntitiesBecomeTheDraft() throws IOException {
    ReaderResult result = google().toResult(Files.readString(CLAY.resolve("google.json")));

    InvoiceDraft draft = result.draft();
    assertThat(draft.vendorName()).isEqualTo("Riverbend Clay Supply");
    assertThat(draft.invoiceNumber()).isEqualTo("RB-20417");
    assertThat(draft.invoiceDate()).isEqualTo(LocalDate.of(2026, 10, 14));
    assertThat(draft.subtotal()).isEqualByComparingTo("212.50");
    assertThat(draft.shipping()).isEqualByComparingTo("35.00");
    assertThat(draft.tax()).isEqualByComparingTo("17.53");
    assertThat(draft.total()).isEqualByComparingTo("265.03");
    assertThat(draft.lines()).hasSize(2);
    InvoiceDraft.Line first = draft.lines().getFirst();
    assertThat(first.description()).isEqualTo("B-Mix 5 cone 5 clay, 50 lb box");
    assertThat(first.sku()).isEqualTo("WC-401");
    assertThat(first.quantity()).isEqualByComparingTo("5");
    assertThat(first.unit()).isEqualTo("box");
    assertThat(first.unitPrice()).isEqualByComparingTo("32.50");
    assertThat(first.lineTotal()).isEqualByComparingTo("162.50");
    // Only printed text on the second line: "$25.00" is read as 25.00
    assertThat(draft.lines().get(1).unitPrice()).isEqualByComparingTo("25.00");
    assertThat(draft.lines().get(1).lineTotal()).isEqualByComparingTo("50.00");
  }

  @Test
  void googleCountsPagesAndPricesThemInBlocksOfTen() throws IOException {
    ReaderResult result = google().toResult(Files.readString(CLAY.resolve("google.json")));

    assertThat(result.usage().pages()).isEqualTo(1);
    assertThat(result.usage().costUsd()).isEqualByComparingTo("0.10");
    assertThat(DocumentServiceCalls.googleCost(10, new BigDecimal("0.10")))
        .isEqualByComparingTo("0.10");
    assertThat(DocumentServiceCalls.googleCost(11, new BigDecimal("0.10")))
        .isEqualByComparingTo("0.20");
  }

  @Test
  void googleRawAnswerKeepsOnlyWhatWasMapped() throws IOException {
    ReaderResult result = google().toResult(Files.readString(CLAY.resolve("google.json")));

    assertThat(result.raw()).contains("supplier_name").doesNotContain("mimeType", "confidence");
  }

  @Test
  void anAnswerThatIsNotADocumentIsUnreadable() {
    assertThatThrownBy(() -> google().toResult("{\"error\": {}}"))
        .isInstanceOfSatisfying(
            InvoiceReadException.class,
            e -> assertThat(e.failure()).isEqualTo(InvoiceReadException.Failure.UNREADABLE));
    assertThatThrownBy(() -> google().toResult("not json"))
        .isInstanceOf(InvoiceReadException.class);
  }

  @Test
  void azureFieldsBecomeTheDraft() throws IOException {
    AzureInvoiceReader reader = azure();
    ReaderResult result =
        reader.toResult(reader.parse(Files.readString(CLAY.resolve("azure.json"))));

    InvoiceDraft draft = result.draft();
    assertThat(draft.vendorName()).isEqualTo("Riverbend Clay Supply");
    assertThat(draft.invoiceNumber()).isEqualTo("RB-20417");
    assertThat(draft.invoiceDate()).isEqualTo(LocalDate.of(2026, 10, 14));
    assertThat(draft.subtotal()).isEqualByComparingTo("212.50");
    // The invoice model has no shipping field
    assertThat(draft.shipping()).isNull();
    assertThat(draft.tax()).isEqualByComparingTo("17.53");
    assertThat(draft.total()).isEqualByComparingTo("265.03");
    assertThat(draft.lines()).hasSize(2);
    InvoiceDraft.Line second = draft.lines().get(1);
    assertThat(second.description()).isEqualTo("Speckled Buff, 25 lb bag");
    assertThat(second.sku()).isEqualTo("WC-415");
    assertThat(second.quantity()).isEqualByComparingTo("2");
    assertThat(second.unitPrice()).isEqualByComparingTo("25.00");
    assertThat(second.lineTotal()).isEqualByComparingTo("50");
    assertThat(result.usage().pages()).isEqualTo(1);
    assertThat(result.usage().costUsd()).isEqualByComparingTo("0");
    assertThat(result.raw())
        .contains("VendorName")
        .doesNotContain("\"words\"", "Riverbend Clay Supply ...");
  }

  @Test
  void azureStatusesAreRead() {
    AzureInvoiceReader reader = azure();

    assertThat(reader.parse("{\"status\": \"running\"}").succeeded()).isFalse();
    assertThat(reader.parse("{\"status\": \"failed\", \"error\": {\"code\": \"x\"}}").failed())
        .isTrue();
  }

  @Test
  void azurePollsOnlyItsOwnEndpoint() {
    String endpoint = "https://studiomanager-di.cognitiveservices.azure.com";

    assertThat(
            AzureInvoiceReader.operationUri(
                    endpoint, endpoint + "/documentintelligence/documentModels/x/analyzeResults/1")
                .getHost())
        .isEqualTo("studiomanager-di.cognitiveservices.azure.com");
    assertThatThrownBy(
            () -> AzureInvoiceReader.operationUri(endpoint, "https://elsewhere.example/x"))
        .isInstanceOf(InvoiceReadException.class);
    assertThatThrownBy(
            () -> AzureInvoiceReader.operationUri(endpoint, endpoint + ".elsewhere.example/x"))
        .isInstanceOf(InvoiceReadException.class);
    assertThatThrownBy(() -> AzureInvoiceReader.operationUri(endpoint, null))
        .isInstanceOf(InvoiceReadException.class);
  }

  @Test
  void failedCallsBecomeFailuresWithoutTheBody() {
    InvoiceReadException refused =
        DocumentServiceCalls.failure(
            "Google Document AI",
            HttpClientErrorException.create(
                HttpStatus.FORBIDDEN, "Forbidden", null, "secret invoice text".getBytes(), null));
    assertThat(refused.failure()).isEqualTo(InvoiceReadException.Failure.REFUSED_CREDENTIALS);
    assertThat(refused.getMessage()).doesNotContain("secret");

    assertThat(
            DocumentServiceCalls.failure(
                    "x",
                    HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "", null, null, null))
                .failure())
        .isEqualTo(InvoiceReadException.Failure.UNREADABLE);
    assertThat(
            DocumentServiceCalls.failure(
                    "x",
                    HttpClientErrorException.create(
                        HttpStatus.TOO_MANY_REQUESTS, "", null, null, null))
                .failure())
        .isEqualTo(InvoiceReadException.Failure.UNAVAILABLE);
    assertThat(
            DocumentServiceCalls.failure(
                    "x",
                    HttpServerErrorException.create(
                        HttpStatus.SERVICE_UNAVAILABLE, "", null, null, null))
                .failure())
        .isEqualTo(InvoiceReadException.Failure.UNAVAILABLE);
    assertThat(DocumentServiceCalls.failure("x", new ResourceAccessException("down")).failure())
        .isEqualTo(InvoiceReadException.Failure.UNAVAILABLE);
  }

  static GoogleInvoiceReader google() {
    return new GoogleInvoiceReader(
        new GoogleReaderProperties(null, null, null, null, null, new BigDecimal("0.10")),
        null,
        JSON);
  }

  static AzureInvoiceReader azure() {
    return new AzureInvoiceReader(new AzureReaderProperties(null, BigDecimal.ZERO), null, JSON);
  }
}
