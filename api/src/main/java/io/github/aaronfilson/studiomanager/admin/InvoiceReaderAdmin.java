package io.github.aaronfilson.studiomanager.admin;

import io.github.aaronfilson.studiomanager.invoice.InvoiceReaderKind;
import io.github.aaronfilson.studiomanager.invoice.InvoiceReaders;
import io.github.aaronfilson.studiomanager.invoice.PlatformReaderRules;
import io.github.aaronfilson.studiomanager.invoice.PlatformReaderUsage;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import java.time.Instant;
import java.util.HashMap;
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
      Map<UUID, String> studioNames,
      Map<InvoiceReaderKind, Boolean> setUp) {}

  private final DatabasePlatformReaderRules rules;
  private final PlatformReaderUsage usage;
  private final PlatformSettings settings;
  private final InvoiceReaders readers;

  InvoiceReaderAdmin(
      DatabasePlatformReaderRules rules,
      PlatformReaderUsage usage,
      PlatformSettings settings,
      InvoiceReaders readers) {
    this.rules = rules;
    this.usage = usage;
    this.settings = settings;
    this.readers = readers;
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
        snapshot.rules(),
        snapshot.saved(),
        usage.monthStart(),
        usage.byStudioThisMonth(),
        names,
        readers.setUp());
  }

  /**
   * After the demo studio's reset: every rule and order that named one of the old studios names the
   * new one instead, so the platform's readers keep reading for it. With nothing saved, the
   * defaults ("on for the demo studio only") are saved with the new studio chosen, so they cover it
   * whatever {@code invoice_demo_studio_id} says. Must be called bound as a platform admin.
   */
  void studioReplaced(Set<UUID> old, UUID replacement) {
    DatabasePlatformReaderRules.Snapshot snapshot = rules.reread();
    boolean saved = snapshot.saved();
    PlatformReaderRules.Rules current = saved ? snapshot.rules() : rules.defaults().rules();
    Map<InvoiceReaderKind, PlatformReaderRules.ReaderRule> readerRules = new HashMap<>();
    complete(current.readers())
        .forEach((reader, rule) -> {
          Set<UUID> studios = new HashSet<>(rule.studios());
          if (!saved) {
            studios = Set.of(replacement);
          } else if (studios.removeAll(old)) {
            studios.add(replacement);
          }
          readerRules.put(
              reader,
              new PlatformReaderRules.ReaderRule(
                  !saved || rule.enabled(),
                  saved ? rule.scope() : PlatformReaderRules.Scope.CHOSEN,
                  studios,
                  rule.studioMonthlyPages(),
                  rule.platformMonthlyPages()));
        });
    Map<UUID, List<InvoiceReaderKind>> orders = new HashMap<>();
    current
        .orders()
        .forEach((studio, order) -> orders.put(old.contains(studio) ? replacement : studio, order));
    PlatformReaderRules.Rules replaced = new PlatformReaderRules.Rules(readerRules, orders);
    if (snapshot.saved() && replaced.equals(current)) {
      return;
    }
    settings.save(InvoiceReaderValues.KEY, rules.write(replaced));
    rules.reread();
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
