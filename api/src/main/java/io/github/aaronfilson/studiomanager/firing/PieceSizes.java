package io.github.aaronfilson.studiomanager.firing;

import java.math.BigDecimal;

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

import org.jspecify.annotations.Nullable;

/** Converts piece measurements between the HTTP contract and the domain. */
public final class PieceSizes {

    private PieceSizes() {
    }

    public static PieceSize fromDto(@Nullable PieceSizeDto dto) {
        if (dto == null) {
            return PieceSize.unmeasured();
        }
        DimensionsDto dimensions = dto.getDimensions();
        WeightMeasureDto weight = dto.getWeight();
        LengthUnit unit = dimensions == null ? null : LengthUnit.valueOf(dimensions.getUnit().name());
        return new PieceSize(
                dimensions == null ? null : new Length(positive(dimensions.getLength(), "length"), unit),
                dimensions == null ? null : new Length(positive(dimensions.getWidth(), "width"), unit),
                dimensions == null ? null : new Length(positive(dimensions.getHeight(), "height"), unit),
                weight == null ? null : new Weight(positive(weight.getValue(), "weight"),
                        WeightUnit.valueOf(weight.getUnit().name())));
    }

    public static PieceSizeDto toDto(PieceSize size) {
        PieceSizeDto dto = new PieceSizeDto();
        if (size.hasDimensions()) {
            dto.dimensions(new DimensionsDto()
                    .length(plain(size.length().value()))
                    .width(plain(size.width().value()))
                    .height(plain(size.height().value()))
                    .unit(LengthUnitDto.valueOf(size.length().unit().name())));
        }
        if (size.weight() != null) {
            dto.weight(new WeightMeasureDto()
                    .value(plain(size.weight().value()))
                    .unit(WeightUnitDto.valueOf(size.weight().unit().name())));
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
