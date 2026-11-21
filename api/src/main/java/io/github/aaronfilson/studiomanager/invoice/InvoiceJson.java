package io.github.aaronfilson.studiomanager.invoice;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plain JSON codecs for v2 JSONB columns and stored compared drafts. Uses string-encoded dates and
 * decimals so values round-trip identically without locale or binary-float drift.
 */
final class InvoiceJson {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
  private static final TypeReference<List<StoredPayment>> PAYMENT_LIST = new TypeReference<>() {};

  private InvoiceJson() {}

  private record StoredPayment(
      @Nullable String date,
      @Nullable String amount,
      @Nullable String method,
      @Nullable String source) {}

  static String writeStrings(List<String> values) {
    return JSON.writeValueAsString(values);
  }

  static List<String> readStrings(@Nullable String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    @Nullable List<String> parsed = JSON.readValue(raw, STRING_LIST);
    return parsed == null ? List.of() : parsed.stream().filter(Objects::nonNull).toList();
  }

  static String writePayments(List<InvoiceDraft.Payment> payments) {
    List<StoredPayment> stored =
        payments.stream()
            .map(p ->
                new StoredPayment(
                    p.date() == null ? null : p.date().toString(),
                    p.amount().toPlainString(),
                    p.method() == null ? null : p.method().name(),
                    p.source() == null ? null : p.source().name()))
            .toList();
    return JSON.writeValueAsString(stored);
  }

  static List<InvoiceDraft.Payment> readPayments(@Nullable String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    @Nullable List<StoredPayment> stored = JSON.readValue(raw, PAYMENT_LIST);
    if (stored == null) {
      return List.of();
    }
    return stored.stream()
        .filter(Objects::nonNull)
        .flatMap(p -> {
          BigDecimal amount = ClaudeInvoiceAnswer.decimal(p.amount());
          if (amount == null) {
            return Stream.empty();
          }
          return Stream.of(
              new InvoiceDraft.Payment(
                  ClaudeInvoiceAnswer.date(p.date()),
                  amount,
                  ClaudeInvoiceAnswer.paymentMethod(p.method()),
                  ClaudeInvoiceAnswer.paymentSource(p.source())));
        })
        .toList();
  }

  static String writeDraft(InvoiceDraft draft) {
    return JSON.writeValueAsString(ClaudeInvoiceAnswer.fromDraft(draft));
  }

  static String writeDraftPretty(InvoiceDraft draft) {
    return JSON.writerWithDefaultPrettyPrinter()
        .writeValueAsString(ClaudeInvoiceAnswer.fromDraft(draft));
  }

  static InvoiceDraft readDraft(String raw) {
    return Objects.requireNonNull(JSON.readValue(raw, ClaudeInvoiceAnswer.class)).toDraft();
  }

  static String writeObjectPretty(Object value) {
    return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(value);
  }
}
