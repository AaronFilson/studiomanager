package io.github.aaronfilson.studiomanager.invoice;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Links an invoice line to one of the studio's supply items (docs/planning/16, step 4). In order:
 *
 * <ol>
 *   <li>an alias for the vendor and the line's SKU, learned from an earlier confirmed invoice;
 *   <li>an alias for the vendor and the line's description;
 *   <li>the item whose name is most contained in the description, by trigram similarity.
 * </ol>
 *
 * <p>The remembered aliases go before similarity because staff confirmed them. Similarity is done
 * here rather than with {@code pg_trgm}: a studio has tens of items, so one query for all of them
 * and a loop is cheap, and the extension would need allow-listing on Azure's managed Postgres (ADR
 * 18).
 */
final class SupplyMatcher {

  /** How much of an item's name must appear in the description to count as a match. */
  static final double NAME_THRESHOLD = 0.6;

  private SupplyMatcher() {}

  /** What an alias is keyed on, besides the vendor. */
  enum AliasKind {
    SKU,
    DESCRIPTION
  }

  /** A remembered alias: this vendor's SKU or description means that supply item. */
  record AliasKey(String vendorKey, AliasKind kind, String lineKey) {}

  /** A supply item the line could be. */
  record Candidate(UUID itemId, String name) {}

  /** How a line was matched, as stored on the line. */
  enum MatchSource {
    ALIAS_SKU,
    ALIAS_DESCRIPTION,
    NAME,
    STAFF
  }

  record Match(UUID itemId, MatchSource source) {}

  static Optional<Match> match(
      @Nullable String vendorName,
      InvoiceDraft.Line line,
      Map<AliasKey, UUID> aliases,
      List<Candidate> items) {
    String vendorKey = normalize(vendorName);
    if (!vendorKey.isEmpty()) {
      String skuKey = normalize(line.sku());
      if (!skuKey.isEmpty()) {
        UUID bySku = aliases.get(new AliasKey(vendorKey, AliasKind.SKU, skuKey));
        if (bySku != null) {
          return Optional.of(new Match(bySku, MatchSource.ALIAS_SKU));
        }
      }
      UUID byDescription =
          aliases.get(
              new AliasKey(vendorKey, AliasKind.DESCRIPTION, normalize(line.description())));
      if (byDescription != null) {
        return Optional.of(new Match(byDescription, MatchSource.ALIAS_DESCRIPTION));
      }
    }
    return byName(line.description(), items).map(id -> new Match(id, MatchSource.NAME));
  }

  /**
   * The item whose name best appears in the description, if it clears the threshold and nothing
   * else is as good: a tie is left for staff rather than guessed.
   */
  static Optional<UUID> byName(String description, List<Candidate> items) {
    Set<String> described = trigrams(normalize(description));
    UUID best = null;
    double bestScore = 0;
    boolean tied = false;
    for (Candidate item : items) {
      double score = containment(trigrams(normalize(item.name())), described);
      if (score > bestScore) {
        best = item.itemId();
        bestScore = score;
        tied = false;
      } else if (score == bestScore && score > 0) {
        tied = true;
      }
    }
    return best != null && bestScore >= NAME_THRESHOLD && !tied
        ? Optional.of(best)
        : Optional.empty();
  }

  /** Lower case, letters and digits only, single spaces: how aliases are keyed and compared. */
  static String normalize(@Nullable String text) {
    if (text == null) {
      return "";
    }
    return text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
  }

  /** The share of {@code name}'s trigrams that also appear in {@code described}. */
  private static double containment(Set<String> name, Set<String> described) {
    if (name.isEmpty()) {
      return 0;
    }
    long shared = name.stream().filter(described::contains).count();
    return (double) shared / name.size();
  }

  /**
   * Trigrams the way {@code pg_trgm} makes them: each word padded with two spaces before, one
   * after.
   */
  static Set<String> trigrams(String normalized) {
    Set<String> grams = new HashSet<>();
    for (String word : normalized.split(" ", -1)) {
      if (word.isEmpty()) {
        continue;
      }
      String padded = "  " + word + " ";
      for (int start = 0; start + 3 <= padded.length(); start++) {
        grams.add(padded.substring(start, start + 3));
      }
    }
    return grams;
  }
}
