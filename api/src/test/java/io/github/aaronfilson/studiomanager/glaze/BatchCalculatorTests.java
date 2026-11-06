package io.github.aaronfilson.studiomanager.glaze;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.aaronfilson.studiomanager.glaze.BatchCalculator.WeightUnit;
import io.github.aaronfilson.studiomanager.glaze.GlazeBook.Line;
import io.github.aaronfilson.studiomanager.glaze.GlazeBook.LineKind;
import io.github.aaronfilson.studiomanager.glaze.GlazeBook.Unit;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

/**
 * Worked examples, then properties checked against many random recipes (a fixed seed, so
 * repeatable).
 */
class BatchCalculatorTests {

  private static final List<Line> TENMOKU =
      List.of(
          base("Custer Feldspar", "45"),
          base("Silica", "30"),
          base("Whiting", "25"),
          new Line(LineKind.ADDITION, "Red Iron Oxide", new BigDecimal("10"), Unit.PERCENT),
          new Line(LineKind.ADDITION, "Epsom salts", new BigDecimal("2"), Unit.GRAMS));

  @Test
  void aHundredPartRecipeWeighsOutInItsOwnNumbers() {
    var batch = BatchCalculator.scale(TENMOKU, new BigDecimal("10000"), WeightUnit.GRAM);
    assertThat(weights(batch)).containsExactly("4500.0", "3000.0", "2500.0", "1000.0", "2.0");
    assertThat(batch.total()).isEqualByComparingTo("11002.0");
  }

  @Test
  void partsThatDontAddUpToAHundredStillFillTheBatch() {
    var batch =
        BatchCalculator.scale(
            List.of(base("A", "3"), base("B", "1")), new BigDecimal("2"), WeightUnit.KILOGRAM);
    assertThat(weights(batch)).containsExactly("1.500", "0.500");
  }

  @Test
  void poundsAndOuncesRoundForTheScale() {
    var pounds =
        BatchCalculator.scale(
            List.of(base("A", "1"), base("B", "2")), BigDecimal.ONE, WeightUnit.POUND);
    assertThat(weights(pounds)).containsExactly("0.333", "0.667");
    // 5 g of a grams addition, in a batch weighed in ounces
    var ounces =
        BatchCalculator.scale(
            List.of(
                base("A", "100"),
                new Line(LineKind.ADDITION, "Bentonite", new BigDecimal("5"), Unit.GRAMS)),
            BigDecimal.TEN,
            WeightUnit.OUNCE);
    assertThat(weights(ounces)).containsExactly("10.00", "0.18");
  }

  @Test
  void aRecipeOfOnlyAdditionsSharesTheBatch() {
    var batch =
        BatchCalculator.scale(
            List.of(
                new Line(LineKind.ADDITION, "Stain", new BigDecimal("1"), Unit.PARTS),
                new Line(LineKind.ADDITION, "Frit", new BigDecimal("3"), Unit.PARTS)),
            new BigDecimal("400"),
            WeightUnit.GRAM);
    assertThat(weights(batch)).containsExactly("100.0", "300.0");
  }

  @Test
  void refusesANothingOrHugeBatch() {
    assertThatThrownBy(() -> BatchCalculator.scale(TENMOKU, BigDecimal.ZERO, WeightUnit.GRAM))
        .isInstanceOf(InvalidInputException.class);
    assertThatThrownBy(
            () -> BatchCalculator.scale(TENMOKU, new BigDecimal("3000"), WeightUnit.POUND))
        .isInstanceOf(InvalidInputException.class);
  }

  private static final Random RANDOM = new Random(20261105);

  @RepeatedTest(200)
  void theBaseLinesAlwaysAddUpToTheBatchSize() {
    List<Line> lines = randomRecipe();
    BigDecimal size = randomSize();
    BigDecimal base = sum(lines, BatchCalculator.exact(lines, size), true);
    assertThat(base.setScale(6, RoundingMode.HALF_UP))
        .isEqualByComparingTo(size.setScale(6, RoundingMode.HALF_UP));
  }

  @RepeatedTest(200)
  void doublingTheBatchDoublesEveryScaledLine() {
    List<Line> lines = randomRecipe();
    BigDecimal size = randomSize();
    List<BigDecimal> once = BatchCalculator.exact(lines, size);
    List<BigDecimal> twice = BatchCalculator.exact(lines, size.multiply(BigDecimal.TWO));
    for (int i = 0; i < lines.size(); i++) {
      BigDecimal expected =
          lines.get(i).unit() == Unit.GRAMS ? once.get(i) : once.get(i).multiply(BigDecimal.TWO);
      assertThat(twice.get(i).setScale(6, RoundingMode.HALF_UP))
          .isEqualByComparingTo(expected.setScale(6, RoundingMode.HALF_UP));
    }
  }

  @RepeatedTest(200)
  void theUnitOnlyChangesHowTheSameWeightIsWritten() {
    List<Line> lines = randomRecipe();
    BigDecimal kilos = randomSize().divide(BigDecimal.valueOf(1000));
    var inKilos = BatchCalculator.scale(lines, kilos, WeightUnit.KILOGRAM);
    var inGrams =
        BatchCalculator.scale(lines, kilos.multiply(BigDecimal.valueOf(1000)), WeightUnit.GRAM);
    for (int i = 0; i < lines.size(); i++) {
      // Each is rounded for its own scale, so they agree to within the coarser rounding
      BigDecimal gramsFromKilos =
          inKilos.lines().get(i).weight().multiply(BigDecimal.valueOf(1000));
      assertThat(gramsFromKilos.subtract(inGrams.lines().get(i).weight()).abs())
          .isLessThanOrEqualTo(new BigDecimal("0.55"));
    }
  }

  private static List<Line> randomRecipe() {
    List<Line> lines = new ArrayList<>();
    int bases = 1 + RANDOM.nextInt(8);
    for (int i = 0; i < bases; i++) {
      lines.add(base("M" + i, amount()));
    }
    int additions = RANDOM.nextInt(4);
    for (int i = 0; i < additions; i++) {
      Unit unit = Unit.values()[RANDOM.nextInt(3)];
      lines.add(new Line(LineKind.ADDITION, "X" + i, new BigDecimal(amount()), unit));
    }
    return lines;
  }

  private static String amount() {
    return BigDecimal.valueOf(1 + RANDOM.nextInt(999_999), 4).toPlainString();
  }

  private static BigDecimal randomSize() {
    return BigDecimal.valueOf(1 + RANDOM.nextInt(99_999), 1);
  }

  private static BigDecimal sum(List<Line> lines, List<BigDecimal> grams, boolean baseOnly) {
    BigDecimal total = BigDecimal.ZERO;
    for (int i = 0; i < lines.size(); i++) {
      if (!baseOnly || lines.get(i).kind() == LineKind.BASE) {
        total = total.add(grams.get(i));
      }
    }
    return total;
  }

  private static Line base(String material, String parts) {
    return new Line(LineKind.BASE, material, new BigDecimal(parts), Unit.PARTS);
  }

  private static List<String> weights(BatchCalculator.Batch batch) {
    return batch.lines().stream().map(w -> w.weight().toPlainString()).toList();
  }
}
