package io.github.aaronfilson.studiomanager.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aaronfilson.studiomanager.invoice.SupplyMatcher.AliasKey;
import io.github.aaronfilson.studiomanager.invoice.SupplyMatcher.AliasKind;
import io.github.aaronfilson.studiomanager.invoice.SupplyMatcher.Candidate;
import io.github.aaronfilson.studiomanager.invoice.SupplyMatcher.Match;
import io.github.aaronfilson.studiomanager.invoice.SupplyMatcher.MatchSource;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SupplyMatcherTests {

  private final UUID bMix = UUID.randomUUID();
  private final UUID speckled = UUID.randomUUID();
  private final UUID kilnWash = UUID.randomUUID();
  private final List<Candidate> items =
      List.of(
          new Candidate(bMix, "B-Mix 5"),
          new Candidate(speckled, "Speckled Buff"),
          new Candidate(kilnWash, "Kiln wash"));

  @Test
  void aNameContainedInTheDescriptionMatches() {
    assertThat(match("Laguna", null, "LAGUNA B-MIX 5 CONE 5 CLAY 50LB BOX", Map.of()))
        .contains(new Match(bMix, MatchSource.NAME));
  }

  @Test
  void somethingNotStockedMatchesNothing() {
    assertThat(match("Laguna", null, "Pallet fee", Map.of())).isEmpty();
  }

  @Test
  void aRememberedSkuBeatsTheName() {
    Map<AliasKey, UUID> aliases = Map.of(new AliasKey("laguna", AliasKind.SKU, "wc 401"), speckled);

    assertThat(match("Laguna", "WC-401", "LAGUNA B-MIX 5", aliases))
        .contains(new Match(speckled, MatchSource.ALIAS_SKU));
  }

  @Test
  void aRememberedDescriptionIsForThatVendorOnly() {
    Map<AliasKey, UUID> aliases =
        Map.of(new AliasKey("laguna", AliasKind.DESCRIPTION, "sb 50"), speckled);

    assertThat(match("Laguna Clay", null, "SB 50", aliases)).isEmpty();
    assertThat(match("LAGUNA", null, "sb-50", aliases))
        .contains(new Match(speckled, MatchSource.ALIAS_DESCRIPTION));
  }

  @Test
  void aTieIsLeftForStaff() {
    List<Candidate> twins = List.of(new Candidate(bMix, "Clay"), new Candidate(speckled, "Clay"));

    assertThat(SupplyMatcher.byName("Clay, 50 lb", twins)).isEmpty();
  }

  @Test
  void trigramsArePaddedLikePgTrgm() {
    assertThat(SupplyMatcher.trigrams("cat")).containsExactlyInAnyOrder("  c", " ca", "cat", "at ");
  }

  private java.util.Optional<Match> match(
      String vendor, String sku, String description, Map<AliasKey, UUID> aliases) {
    return SupplyMatcher.match(
        vendor, new InvoiceDraft.Line(description, sku, null, null, null, null), aliases, items);
  }
}
