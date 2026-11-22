package io.github.aaronfilson.studiomanager.glaze;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aaronfilson.studiomanager.glaze.BatchCalculator.WeightUnit;
import io.github.aaronfilson.studiomanager.glaze.GlazeBook.Line;
import io.github.aaronfilson.studiomanager.glaze.GlazeBook.LineKind;
import io.github.aaronfilson.studiomanager.glaze.GlazeBook.Unit;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.Size;

/**
 * Property-based tests with shrinking (Plan 14, Plan 15 &amp; Plan 19) for dry glaze batch scaling:
 * base weights sum to batch size, doubling the batch doubles proportional lines while preserving
 * fixed gram additions, and kg/g conversions agree within scale rounding.
 */
class BatchCalculatorPropertyTests {

  @Property
  void baseLinesSumToBatchSizeAndScaleLinearly(
      @ForAll @Size(min = 1, max = 8)
          List<@BigRange(min = "0.5", max = "100.0") BigDecimal> baseParts,
      @ForAll @Size(min = 0, max = 3)
          List<@BigRange(min = "0.1", max = "20.0") BigDecimal> percentAdds,
      @ForAll @BigRange(min = "0.5", max = "25.0") BigDecimal fixedGrams,
      @ForAll @BigRange(min = "50.0", max = "20000.0") BigDecimal batchGrams) {
    List<Line> recipe = buildRecipe(baseParts, percentAdds, fixedGrams);

    List<BigDecimal> once = BatchCalculator.exact(recipe, batchGrams);
    BigDecimal baseSum = BigDecimal.ZERO;
    for (int i = 0; i < recipe.size(); i++) {
      if (recipe.get(i).kind() == LineKind.BASE) {
        baseSum = baseSum.add(once.get(i));
      }
    }
    assertThat(baseSum.setScale(6, RoundingMode.HALF_UP))
        .isEqualByComparingTo(batchGrams.setScale(6, RoundingMode.HALF_UP));

    List<BigDecimal> twice = BatchCalculator.exact(recipe, batchGrams.multiply(BigDecimal.TWO));
    for (int i = 0; i < recipe.size(); i++) {
      BigDecimal expected =
          recipe.get(i).unit() == Unit.GRAMS ? once.get(i) : once.get(i).multiply(BigDecimal.TWO);
      assertThat(twice.get(i).setScale(6, RoundingMode.HALF_UP))
          .isEqualByComparingTo(expected.setScale(6, RoundingMode.HALF_UP));
    }
  }

  @Property
  void kilogramsAndGramsAgreeWithinDisplayRounding(
      @ForAll @Size(min = 1, max = 6)
          List<@BigRange(min = "1.0", max = "100.0") BigDecimal> baseParts,
      @ForAll @BigRange(min = "0.5", max = "50.0") BigDecimal kilos) {
    List<Line> recipe = buildRecipe(baseParts, List.of(), null);
    var inKilos = BatchCalculator.scale(recipe, kilos, WeightUnit.KILOGRAM);
    var inGrams =
        BatchCalculator.scale(recipe, kilos.multiply(BigDecimal.valueOf(1000)), WeightUnit.GRAM);
    for (int i = 0; i < recipe.size(); i++) {
      BigDecimal gramsFromKilos =
          inKilos.lines().get(i).weight().multiply(BigDecimal.valueOf(1000));
      assertThat(gramsFromKilos.subtract(inGrams.lines().get(i).weight()).abs())
          .isLessThanOrEqualTo(new BigDecimal("0.55"));
    }
  }

  private static List<Line> buildRecipe(
      List<BigDecimal> baseParts, List<BigDecimal> percentAdds, BigDecimal fixedGrams) {
    List<Line> lines = new ArrayList<>();
    for (int i = 0; i < baseParts.size(); i++) {
      lines.add(new Line(LineKind.BASE, "Base-" + i, baseParts.get(i), Unit.PARTS));
    }
    for (int i = 0; i < percentAdds.size(); i++) {
      lines.add(new Line(LineKind.ADDITION, "Add-" + i, percentAdds.get(i), Unit.PERCENT));
    }
    if (fixedGrams != null) {
      lines.add(new Line(LineKind.ADDITION, "Epsom salts", fixedGrams, Unit.GRAMS));
    }
    return lines;
  }
}
