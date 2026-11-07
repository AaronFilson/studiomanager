package io.github.aaronfilson.studiomanager.admin;

import io.github.aaronfilson.studiomanager.invoice.InvoiceReaderKind;
import io.github.aaronfilson.studiomanager.invoice.PlatformReaderRules;
import io.github.aaronfilson.studiomanager.invoice.PlatformReaderUsage;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * What the admin page does with the invoice readers: turn each platform-paid reader on or off, pick
 * its studios and caps, set a studio's reader order, and see this month's pages and spend. Saved
 * through {@link PlatformSettings}, so the database checks the caller and keeps the history.
 */
@Service
class InvoiceReaderAdmin {

  static final int MOST_PAGES = 1_000_000;
  static final int MOST_CHOSEN_STUDIOS = 500;
  static final int MOST_ORDERS = 1_000;

  /**
   * The readers as the admin page shows them.
   *
   * @param saved false while the configured defaults are in force
   */
  record Overview(
      PlatformReaderRules.Rules rules,
      boolean saved,
      Instant monthStart,
      List<PlatformReaderUsage.StudioUse> usage,
      Map<UUID, String> studioNames) {}

  private final DatabasePlatformReaderRules rules;
  private final PlatformReaderUsage usage;
  private final PlatformSettings settings;

  InvoiceReaderAdmin(
      DatabasePlatformReaderRules rules, PlatformReaderUsage usage, PlatformSettings settings) {
    this.rules = rules;
    this.usage = usage;
    this.settings = settings;
  }

  Overview overview() {
    return overview(rules.fresh());
  }

  Overview save(PlatformReaderRules.Rules changed) {
    Set<UUID> known =
        settings.studios().stream()
            .map(PlatformSettings.StudioChoice::studioId)
            .collect(Collectors.toSet());
    check(changed, known);
    settings.save(InvoiceReaderValues.KEY, rules.write(changed));
    return overview(rules.reread());
  }

  /** Back to the configured defaults. Nothing is saved if they're already in force. */
  Overview reset() {
    if (rules.reread().saved()) {
      settings.save(InvoiceReaderValues.KEY, null);
    }
    return overview(rules.reread());
  }

  private Overview overview(DatabasePlatformReaderRules.Snapshot snapshot) {
    Map<UUID, String> names =
        settings.studios().stream()
            .collect(
                Collectors.toMap(
                    PlatformSettings.StudioChoice::studioId,
                    PlatformSettings.StudioChoice::studioName,
                    (first, second) -> first));
    return new Overview(
        snapshot.rules(), snapshot.saved(), usage.monthStart(), usage.byStudioThisMonth(), names);
  }

  private static void check(PlatformReaderRules.Rules changed, Set<UUID> known) {
    for (Map.Entry<InvoiceReaderKind, PlatformReaderRules.ReaderRule> entry :
        changed.readers().entrySet()) {
      InvoiceReaderKind reader = entry.getKey();
      PlatformReaderRules.ReaderRule rule = entry.getValue();
      if (!reader.paidByPlatform()) {
        throw new InvalidInputException(
            "Only the platform's own readers have rules; Claude runs on the studio's key.");
      }
      if (rule.studioMonthlyPages() < 0 || rule.studioMonthlyPages() > MOST_PAGES) {
        throw new InvalidInputException("A studio's monthly pages are between 0 and 1,000,000.");
      }
      if (rule.platformMonthlyPages() < 0 || rule.platformMonthlyPages() > MOST_PAGES) {
        throw new InvalidInputException(
            "The platform's monthly pages are between 0 and 1,000,000.");
      }
      if (rule.studios().size() > MOST_CHOSEN_STUDIOS) {
        throw new InvalidInputException("A reader can have at most 500 chosen studios.");
      }
      if (!known.containsAll(rule.studios())) {
        throw new InvalidInputException("A chosen studio doesn't exist.");
      }
    }
    if (changed.orders().size() > MOST_ORDERS) {
      throw new InvalidInputException("At most 1,000 studios can have their own order.");
    }
    for (Map.Entry<UUID, List<InvoiceReaderKind>> order : changed.orders().entrySet()) {
      List<InvoiceReaderKind> readers = order.getValue();
      if (readers.isEmpty() || new HashSet<>(readers).size() != readers.size()) {
        throw new InvalidInputException(
            "A studio's order names at least one reader, each at most once.");
      }
      if (!known.contains(order.getKey())) {
        throw new InvalidInputException("A studio with an order doesn't exist.");
      }
    }
  }

  /** The rules keyed by reader, with an "off" rule for any platform reader left out. */
  static Map<InvoiceReaderKind, PlatformReaderRules.ReaderRule> complete(
      Map<InvoiceReaderKind, PlatformReaderRules.ReaderRule> given) {
    return List.of(InvoiceReaderKind.values()).stream()
        .filter(InvoiceReaderKind::paidByPlatform)
        .collect(
            Collectors.toMap(
                Function.identity(),
                reader -> given.getOrDefault(reader, PlatformReaderRules.ReaderRule.off())));
  }
}
