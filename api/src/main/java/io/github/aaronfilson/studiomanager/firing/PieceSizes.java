package io.github.aaronfilson.studiomanager.firing;

import io.github.aaronfilson.studiomanager.contract.DimensionsDto;
import io.github.aaronfilson.studiomanager.contract.LengthUnitDto;
import io.github.aaronfilson.studiomanager.contract.PieceSizeDto;
import io.github.aaronfilson.studiomanager.contract.WeightMeasureDto;
import io.github.aaronfilson.studiomanager.contract.WeightUnitDto;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.measure.Length;
import io.github.aaronfilson.studiomanager.platform.measure.LengthUnit;
import io.github.aaronfilson.studiomanager.platform.measure.Weight;
import io.github.aaronfilson.studiomanager.platform.measure.WeightUnit;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/** Converts piece measurements between the HTTP contract and the domain. */
public final class PieceSizes {

  private PieceSizes() {}

  public static PieceSize fromDto(@Nullable PieceSizeDto dto) {
    if (dto == null) {
      return PieceSize.unmeasured();
    }
    DimensionsDto dimensions = dto.getDimensions();
    WeightMeasureDto weight = dto.getWeight();
    Weight measured =
        weight == null
            ? null
            : new Weight(
                positive(weight.getValue(), "weight"), WeightUnit.valueOf(weight.getUnit().name()));
    if (dimensions == null) {
      return new PieceSize(null, null, null, measured);
    }
    LengthUnit unit = LengthUnit.valueOf(dimensions.getUnit().name());
    return new PieceSize(
        new Length(positive(dimensions.getLength(), "length"), unit),
        new Length(positive(dimensions.getWidth(), "width"), unit),
        new Length(positive(dimensions.getHeight(), "height"), unit),
        measured);
  }

  public static PieceSizeDto toDto(PieceSize size) {
    PieceSizeDto dto = new PieceSizeDto();
    PieceSize.Box box = size.box();
    if (box != null) {
      dto.dimensions(
          new DimensionsDto()
              .length(plain(box.length().value()))
              .width(plain(box.width().value()))
              .height(plain(box.height().value()))
              .unit(LengthUnitDto.valueOf(box.length().unit().name())));
    }
    Weight weight = size.weight();
    if (weight != null) {
      dto.weight(
          new WeightMeasureDto()
              .value(plain(weight.value()))
              .unit(WeightUnitDto.valueOf(weight.unit().name())));
    }
    return dto;
  }

  private static BigDecimal positive(String value, String what) {
    BigDecimal decimal = new BigDecimal(value);
    if (decimal.signum() <= 0) {
      throw new InvalidInputException("The %s must be more than zero.".formatted(what));
    }
    return decimal;
  }

  private static String plain(BigDecimal value) {
    return value.stripTrailingZeros().toPlainString();
  }
}
