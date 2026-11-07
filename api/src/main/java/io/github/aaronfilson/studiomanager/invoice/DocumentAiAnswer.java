package io.github.aaronfilson.studiomanager.invoice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * The part of Google Document AI's {@code ProcessResponse} the Invoice Parser reader uses: the
 * entities and the page count. Unknown fields are ignored, so this is also what's kept as the raw
 * answer, without the document's full text or layout.
 *
 * <p>Entity types are the Invoice Parser's: {@code supplier_name}, {@code invoice_id}, {@code
 * invoice_date}, {@code net_amount}, {@code freight_amount}, {@code total_tax_amount}, {@code
 * total_amount}, and {@code line_item} with {@code line_item/...} properties.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record DocumentAiAnswer(@Nullable Document document) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Document(@Nullable List<Entity> entities, @Nullable List<Page> pages) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Page(@Nullable Integer pageNumber) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Entity(
      @Nullable String type,
      @Nullable String mentionText,
      @Nullable NormalizedValue normalizedValue,
      @Nullable List<Entity> properties) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record NormalizedValue(
      @Nullable String text, @Nullable Money moneyValue, @Nullable DateValue dateValue) {}

  /** {@code google.type.Money}: whole units as an int64 in a string, and nanos. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record Money(@Nullable String currencyCode, @Nullable String units, @Nullable Integer nanos) {}

  /** {@code google.type.Date}. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record DateValue(@Nullable Integer year, @Nullable Integer month, @Nullable Integer day) {}

  /** The pages Google read, which it bills by. */
  int pages() {
    List<Page> read = document == null ? null : document.pages();
    return read == null ? 0 : read.size();
  }

  InvoiceDraft toDraft() {
    List<Entity> entities = entitiesOf(document == null ? null : document.entities());
    return new InvoiceDraft(
        text(first(entities, "supplier_name")),
        text(first(entities, "invoice_id")),
        date(first(entities, "invoice_date")),
        amount(first(entities, "net_amount")),
        amount(first(entities, "freight_amount")),
        amount(first(entities, "total_tax_amount")),
        amount(first(entities, "total_amount")),
        entities.stream()
            .filter(entity -> "line_item".equals(entity.type()))
            .map(DocumentAiAnswer::line)
            .toList());
  }

  private static InvoiceDraft.Line line(Entity item) {
    List<Entity> properties = entitiesOf(item.properties());
    String description = text(first(properties, "line_item/description"));
    if (description == null) {
      description = PrintedValues.text(item.mentionText());
    }
    return new InvoiceDraft.Line(
        description == null ? "" : description,
        text(first(properties, "line_item/product_code")),
        amount(first(properties, "line_item/quantity")),
        text(first(properties, "line_item/unit")),
        amount(first(properties, "line_item/unit_price")),
        amount(first(properties, "line_item/amount")));
  }

  private static List<Entity> entitiesOf(@Nullable List<Entity> entities) {
    return entities == null ? List.of() : entities;
  }

  private static Optional<Entity> first(List<Entity> entities, String type) {
    return entities.stream().filter(entity -> type.equals(entity.type())).findFirst();
  }

  private static @Nullable String text(Optional<Entity> entity) {
    return entity.map(Entity::mentionText).map(PrintedValues::text).orElse(null);
  }

  /** The normalized money value when Google gives one, else the normalized or printed text. */
  private static @Nullable BigDecimal amount(Optional<Entity> found) {
    if (found.isEmpty()) {
      return null;
    }
    Entity entity = found.get();
    NormalizedValue normalized = entity.normalizedValue();
    if (normalized != null) {
      BigDecimal money = money(normalized.moneyValue());
      if (money != null) {
        return money;
      }
      BigDecimal fromText = ClaudeInvoiceAnswer.decimal(normalized.text());
      if (fromText != null) {
        return fromText;
      }
    }
    return PrintedValues.amount(entity.mentionText());
  }

  private static @Nullable BigDecimal money(@Nullable Money money) {
    if (money == null) {
      return null;
    }
    String printedUnits = money.units();
    BigDecimal units =
        printedUnits == null ? BigDecimal.ZERO : ClaudeInvoiceAnswer.decimal(printedUnits);
    if (units == null) {
      return null;
    }
    Integer nanos = money.nanos();
    BigDecimal amount =
        units.add(BigDecimal.valueOf(nanos == null ? 0 : nanos, 9)).stripTrailingZeros();
    return amount.scale() < 0 ? amount.setScale(0) : amount;
  }

  private static @Nullable LocalDate date(Optional<Entity> found) {
    NormalizedValue normalized = found.map(Entity::normalizedValue).orElse(null);
    if (normalized == null) {
      return null;
    }
    DateValue value = normalized.dateValue();
    if (value != null) {
      Integer year = value.year();
      Integer month = value.month();
      Integer day = value.day();
      if (year != null && month != null && day != null) {
        try {
          return LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
          return null;
        }
      }
    }
    return ClaudeInvoiceAnswer.date(normalized.text());
  }
}
