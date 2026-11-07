package io.github.aaronfilson.studiomanager.invoice;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What the platform admin has decided about the invoice readers: whether each platform-paid reader
 * is on, for which studios, under which monthly page caps, and each studio's reader order
 * (docs/planning/20, "Invoice readers"). The admin module keeps it in the settings store (ADR 24);
 * this module only reads it.
 */
public interface PlatformReaderRules {

  /** The rules in force now. Cheap to call: implementations keep a copy. */
  Rules current();

  /** Which studios a platform-paid reader may read for. */
  enum Scope {
    /** No studio. */
    NONE,
    /** Only the chosen studios. */
    CHOSEN,
    /** Every studio whose owner consents. */
    ALL
  }

  /**
   * One platform-paid reader's rule.
   *
   * @param studios the chosen studios; only read when the scope is {@link Scope#CHOSEN}
   * @param studioMonthlyPages pages one studio may use in a calendar month (UTC)
   * @param platformMonthlyPages pages every studio together may use in a calendar month (UTC)
   */
  record ReaderRule(
      boolean enabled,
      Scope scope,
      Set<UUID> studios,
      int studioMonthlyPages,
      int platformMonthlyPages) {

    public ReaderRule {
      studios = Set.copyOf(studios);
    }

    /** Whether the admin lets this reader read for the studio, before consent and caps. */
    public boolean covers(UUID studioId) {
      if (!enabled) {
        return false;
      }
      return switch (scope) {
        case NONE -> false;
        case CHOSEN -> studios.contains(studioId);
        case ALL -> true;
      };
    }

    /** Off for everyone. */
    public static ReaderRule off() {
      return new ReaderRule(false, Scope.NONE, Set.of(), 0, 0);
    }
  }

  /**
   * Every rule.
   *
   * @param readers a rule for each platform-paid reader
   * @param orders each studio's reader order, where the admin set one
   */
  record Rules(
      Map<InvoiceReaderKind, ReaderRule> readers, Map<UUID, List<InvoiceReaderKind>> orders) {

    public Rules {
      readers = Map.copyOf(readers);
      orders = Map.copyOf(orders);
    }

    /** The rule for one reader; off for one with no rule, and never consulted for Claude. */
    public ReaderRule rule(InvoiceReaderKind reader) {
      return readers.getOrDefault(reader, ReaderRule.off());
    }

    /**
     * The studio's reader order: the admin's, or every reader in {@link InvoiceReaderKind} order
     * (Claude first) when there is none.
     */
    public List<InvoiceReaderKind> order(UUID studioId) {
      return orders.getOrDefault(studioId, List.of(InvoiceReaderKind.values()));
    }
  }
}
