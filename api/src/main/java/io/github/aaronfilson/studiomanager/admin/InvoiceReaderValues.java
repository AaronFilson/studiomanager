package io.github.aaronfilson.studiomanager.admin;

import io.github.aaronfilson.studiomanager.invoice.InvoiceReaderKind;
import io.github.aaronfilson.studiomanager.invoice.PlatformReaderRules;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * How the invoice reader rules are stored as JSON in {@code platform_setting}, under one key: lists
 * of plain values, so a row reads the same to a person as to the code.
 */
final class InvoiceReaderValues {

  /** The key of the rules, once the admin has saved them. */
  static final String KEY = "invoice-readers";

  record StoredRule(
      InvoiceReaderKind reader,
      boolean enabled,
      PlatformReaderRules.Scope scope,
      List<UUID> studioIds,
      int studioMonthlyPages,
      int platformMonthlyPages) {

    @SuppressWarnings("ConstantConditions")
    StoredRule {
      studioIds = studioIds == null ? List.of() : List.copyOf(studioIds);
      if (reader == null || scope == null) {
        throw new IllegalArgumentException("A stored reader rule names its reader and scope");
      }
    }

    static StoredRule of(InvoiceReaderKind reader, PlatformReaderRules.ReaderRule rule) {
      return new StoredRule(
          reader,
          rule.enabled(),
          rule.scope(),
          rule.studios().stream().sorted().toList(),
          rule.studioMonthlyPages(),
          rule.platformMonthlyPages());
    }

    PlatformReaderRules.ReaderRule rule() {
      return new PlatformReaderRules.ReaderRule(
          enabled, scope, new HashSet<>(studioIds), studioMonthlyPages, platformMonthlyPages);
    }
  }

  record StoredOrder(UUID studioId, List<InvoiceReaderKind> readers) {

    @SuppressWarnings("ConstantConditions")
    StoredOrder {
      if (studioId == null || readers == null) {
        throw new IllegalArgumentException("A stored order names its studio and readers");
      }
      readers = List.copyOf(readers);
    }
  }

  record StoredRules(List<StoredRule> readers, List<StoredOrder> orders) {

    /** A row written by hand might leave a list out; that reads as empty, not as a failure. */
    @SuppressWarnings("ConstantConditions")
    StoredRules {
      readers = readers == null ? List.of() : List.copyOf(readers);
      orders = orders == null ? List.of() : List.copyOf(orders);
    }

    static StoredRules of(PlatformReaderRules.Rules rules) {
      return new StoredRules(
          rules.readers().entrySet().stream()
              .sorted(Map.Entry.comparingByKey())
              .map(entry -> StoredRule.of(entry.getKey(), entry.getValue()))
              .toList(),
          rules.orders().entrySet().stream()
              .sorted(Map.Entry.comparingByKey())
              .map(entry -> new StoredOrder(entry.getKey(), entry.getValue()))
              .toList());
    }

    PlatformReaderRules.Rules rules() {
      Map<InvoiceReaderKind, PlatformReaderRules.ReaderRule> byReader = new LinkedHashMap<>();
      readers.forEach(stored -> byReader.put(stored.reader(), stored.rule()));
      Map<UUID, List<InvoiceReaderKind>> byStudio = new LinkedHashMap<>();
      orders.forEach(order -> byStudio.put(order.studioId(), List.copyOf(order.readers())));
      return new PlatformReaderRules.Rules(byReader, byStudio);
    }
  }

  private InvoiceReaderValues() {}
}
