package io.github.aaronfilson.studiomanager.invoice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * The part of Azure AI Document Intelligence's analyze result (API 2024-11-30, {@code
 * prebuilt-invoice}) the reader uses: the operation's status, the first document's fields, and the
 * page count. Unknown fields are ignored, so this is also what's kept as the raw answer.
 *
 * <p>Fields are the invoice model's: {@code VendorName}, {@code InvoiceId}, {@code InvoiceDate},
 * {@code SubTotal}, {@code TotalTax}, {@code InvoiceTotal}, and {@code Items}, each an object with
 * {@code Description}, {@code ProductCode}, {@code Quantity}, {@code Unit}, {@code UnitPrice} and
 * {@code Amount}. The model has no shipping field, so shipping stays empty.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record DocumentIntelligenceAnswer(
    @Nullable String status, @Nullable AnalyzeResult analyzeResult, @Nullable Problem error) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  record AnalyzeResult(@Nullable List<Page> pages, @Nullable List<AnalyzedDocument> documents) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Page(@Nullable Integer pageNumber) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record AnalyzedDocument(@Nullable Map<String, Field> fields) {}

  /** A field, typed by {@code type}; only the value for its type is set. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record Field(
      @Nullable String type,
      @Nullable String content,
      @Nullable String valueString,
      @Nullable String valueDate,
      @Nullable BigDecimal valueNumber,
      @Nullable Currency valueCurrency,
      @Nullable List<Field> valueArray,
      @Nullable Map<String, Field> valueObject) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Currency(@Nullable BigDecimal amount, @Nullable String currencyCode) {}

  /** The service's error, without its message, which can quote the document. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record Problem(@Nullable String code) {}

  boolean succeeded() {
    return "succeeded".equals(status);
  }

  boolean failed() {
    return "failed".equals(status) || "canceled".equals(status);
  }

  /** The pages Azure read, which it bills by. */
  int pages() {
    List<Page> read = analyzeResult == null ? null : analyzeResult.pages();
    return read == null ? 0 : read.size();
  }

  /**
   * The answer as it's kept: only the fields {@link #toDraft} reads. The invoice model also returns
   * the customer's name and addresses ({@code CustomerName}, {@code BillingAddress}, ...), which
   * the draft never uses, so they aren't stored (docs/planning/24, Compare change 6).
   */
  DocumentIntelligenceAnswer kept() {
    List<AnalyzedDocument> read = analyzeResult == null ? null : analyzeResult.documents();
    if (analyzeResult == null || read == null) {
      return this;
    }
    List<AnalyzedDocument> documents =
        read.stream().map(document -> new AnalyzedDocument(only(document.fields(), READ))).toList();
    return new DocumentIntelligenceAnswer(
        status, new AnalyzeResult(analyzeResult.pages(), documents), error);
  }

  private static final Set<String> READ =
      Set.of(
          "VendorName",
          "InvoiceId",
          "InvoiceDate",
          "SubTotal",
          "TotalTax",
          "InvoiceTotal",
          "Items");

  private static final Set<String> LINE_READ =
      Set.of("Description", "ProductCode", "Quantity", "Unit", "UnitPrice", "Amount");

  private static @Nullable Map<String, Field> only(
      @Nullable Map<String, Field> fields, Set<String> names) {
    if (fields == null) {
      return null;
    }
    Map<String, Field> kept = new LinkedHashMap<>();
    fields.forEach((name, field) -> {
      if (names.contains(name)) {
        kept.put(name, "Items".equals(name) ? keptItems(field) : field);
      }
    });
    return kept;
  }

  private static Field keptItems(Field items) {
    List<Field> lines = items.valueArray();
    return new Field(
        items.type(),
        null,
        null,
        null,
        null,
        null,
        lines == null
            ? null
            : lines.stream()
                .map(line ->
                    new Field(
                        line.type(),
                        line.content(),
                        line.valueString(),
                        line.valueDate(),
                        line.valueNumber(),
                        line.valueCurrency(),
                        null,
                        only(line.valueObject(), LINE_READ)))
                .toList(),
        null);
  }

  InvoiceDraft toDraft() {
    List<AnalyzedDocument> documents = analyzeResult == null ? null : analyzeResult.documents();
    Map<String, Field> fields =
        documents == null || documents.isEmpty() ? null : documents.getFirst().fields();
    if (fields == null) {
      return InvoiceDraft.empty();
    }
    Field items = fields.get("Items");
    List<Field> lines = items == null ? null : items.valueArray();
    return new InvoiceDraft(
        text(fields.get("VendorName")),
        text(fields.get("InvoiceId")),
        ClaudeInvoiceAnswer.date(date(fields.get("InvoiceDate"))),
        amount(fields.get("SubTotal")),
        null,
        amount(fields.get("TotalTax")),
        amount(fields.get("InvoiceTotal")),
        lines == null ? List.of() : lines.stream().map(DocumentIntelligenceAnswer::line).toList());
  }

  private static InvoiceDraft.Line line(Field item) {
    Map<String, Field> parts = item.valueObject();
    if (parts == null) {
      String content = PrintedValues.text(item.content());
      return new InvoiceDraft.Line(content == null ? "" : content, null, null, null, null, null);
    }
    String description = text(parts.get("Description"));
    return new InvoiceDraft.Line(
        description == null ? "" : description,
        text(parts.get("ProductCode")),
        amount(parts.get("Quantity")),
        text(parts.get("Unit")),
        amount(parts.get("UnitPrice")),
        amount(parts.get("Amount")));
  }

  private static @Nullable String text(@Nullable Field field) {
    if (field == null) {
      return null;
    }
    String value = PrintedValues.text(field.valueString());
    return value == null ? PrintedValues.text(field.content()) : value;
  }

  private static @Nullable String date(@Nullable Field field) {
    return field == null ? null : field.valueDate();
  }

  /** A currency or number value when Azure gives one, else the printed text. */
  private static @Nullable BigDecimal amount(@Nullable Field field) {
    if (field == null) {
      return null;
    }
    Currency currency = field.valueCurrency();
    BigDecimal amount = currency == null ? null : currency.amount();
    if (amount == null) {
      amount = field.valueNumber();
    }
    return amount == null ? PrintedValues.amount(field.content()) : amount;
  }
}
