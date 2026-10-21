package io.github.aaronfilson.studiomanager.firing;

import java.math.BigDecimal;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.contract.FeeQuoteDto;
import io.github.aaronfilson.studiomanager.contract.FeeRoundingDto;
import io.github.aaronfilson.studiomanager.contract.FeeRuleTypeDto;
import io.github.aaronfilson.studiomanager.contract.FeeScheduleDto;
import io.github.aaronfilson.studiomanager.contract.FeeUnitDto;
import io.github.aaronfilson.studiomanager.contract.FeesApi;
import io.github.aaronfilson.studiomanager.contract.MoneyDto;
import io.github.aaronfilson.studiomanager.contract.PieceSizeDto;
import io.github.aaronfilson.studiomanager.contract.SaveFeeScheduleRequestDto;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.Money;
import io.github.aaronfilson.studiomanager.platform.measure.WeightUnit;

import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class FeeController implements FeesApi {

    private final FeeService fees;

    FeeController(FeeService fees) {
        this.fees = fees;
    }

    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<FeeScheduleDto> getFeeSchedule(UUID studioId) {
        return ResponseEntity.ok(toDto(fees.current()));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<FeeScheduleDto> saveFeeSchedule(UUID studioId, SaveFeeScheduleRequestDto request) {
        FeeRule rule = toRule(request.getRuleType(), new BigDecimal(request.getRate()), request.getUnit(),
                FeeRule.DimensionRounding.valueOf(request.getRounding().name()));
        String taxCategory = request.getTaxCategory() == null ? FeeSchedule.DEFAULT_TAX_CATEGORY : request.getTaxCategory();
        return ResponseEntity.ok(toDto(fees.save(rule, new BigDecimal(request.getMinimum()), taxCategory)));
    }

    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<FeeQuoteDto> quoteFee(UUID studioId, PieceSizeDto size) {
        FeeQuote quote = fees.quote(PieceSizes.fromDto(size));
        return ResponseEntity.ok(new FeeQuoteDto()
                .amount(money(quote.amount()))
                .scheduleVersion(quote.scheduleVersion())
                .taxCategory(quote.taxCategory())
                .breakdown(quote.breakdown()));
    }

    static MoneyDto money(Money money) {
        return new MoneyDto().amount(money.amount().toPlainString()).currency(money.currency().getCurrencyCode());
    }

    private static FeeRule toRule(FeeRuleTypeDto type, BigDecimal rate, @Nullable FeeUnitDto unit,
            FeeRule.DimensionRounding rounding) {
        return switch (type) {
            case PER_VOLUME -> new FeeRule.PerVolume(rate, switch (requireUnit(unit)) {
                case CUBIC_INCH -> FeeRule.VolumeUnit.CUBIC_INCH;
                case CUBIC_CENTIMETRE -> FeeRule.VolumeUnit.CUBIC_CENTIMETRE;
                case LITRE -> FeeRule.VolumeUnit.LITRE;
                default -> throw new InvalidInputException("A volume rate is per CUBIC_INCH, CUBIC_CENTIMETRE or LITRE.");
            }, rounding);
            case PER_WEIGHT -> new FeeRule.PerWeight(rate, switch (requireUnit(unit)) {
                case POUND -> WeightUnit.POUND;
                case KILOGRAM -> WeightUnit.KILOGRAM;
                default -> throw new InvalidInputException("A weight rate is per POUND or KILOGRAM.");
            }, rounding);
            case FLAT -> {
                if (unit != null) {
                    throw new InvalidInputException("A flat fee has no unit.");
                }
                yield new FeeRule.Flat(rate);
            }
        };
    }

    private static FeeUnitDto requireUnit(@Nullable FeeUnitDto unit) {
        if (unit == null) {
            throw new InvalidInputException("This kind of fee needs a unit.");
        }
        return unit;
    }

    private static FeeScheduleDto toDto(FeeSchedule schedule) {
        FeeScheduleDto dto = new FeeScheduleDto()
                .version(schedule.version())
                .minimum(schedule.minimum().toPlainString())
                .taxCategory(schedule.taxCategory());
        switch (schedule.rule()) {
            case FeeRule.PerVolume rule -> dto.ruleType(FeeRuleTypeDto.PER_VOLUME)
                    .rate(rule.rate().toPlainString())
                    .unit(FeeUnitDto.valueOf(rule.unit().name()))
                    .rounding(FeeRoundingDto.valueOf(rule.rounding().name()));
            case FeeRule.PerWeight rule -> dto.ruleType(FeeRuleTypeDto.PER_WEIGHT)
                    .rate(rule.rate().toPlainString())
                    .unit(FeeUnitDto.valueOf(rule.unit().name()))
                    .rounding(FeeRoundingDto.valueOf(rule.rounding().name()));
            case FeeRule.Flat rule -> dto.ruleType(FeeRuleTypeDto.FLAT)
                    .rate(rule.rate().toPlainString())
                    .rounding(FeeRoundingDto.NONE);
        }
        return dto;
    }

}
