package io.github.aaronfilson.studiomanager.firing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.measure.WeightUnit;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.StudioService;

import org.jspecify.annotations.Nullable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The bound studio's firing-fee schedule, and pricing pieces with it. */
@Service
public class FeeService {

    private final JdbcClient jdbc;
    private final StudioService studios;

    FeeService(JdbcClient jdbc, StudioService studios) {
        this.jdbc = jdbc;
        this.studios = studios;
    }

    /** The latest version, or {@link FeeSchedule#free()} if the studio hasn't set fees. */
    @Transactional(readOnly = true)
    public FeeSchedule current() {
        return jdbc.sql("""
                select version, rule_type, rate, unit, rounding, minimum, tax_category
                from fee_schedule
                order by version desc
                limit 1
                """)
                .query(Row.class).optional()
                .map(Row::toSchedule)
                .orElseGet(FeeSchedule::free);
    }

    /**
     * Saves {@code rule}, {@code minimum} and {@code taxCategory} as the next version. Two
     * concurrent saves can't both win: the second hits the unique version and is told to retry.
     */
    @Transactional
    public FeeSchedule save(FeeRule rule, BigDecimal minimum, String taxCategory) {
        TenantContext caller = TenantContext.require();
        FeeSchedule next = new FeeSchedule(current().version() + 1, rule, minimum, taxCategory.strip());
        try {
            jdbc.sql("""
                    insert into fee_schedule (id, studio_id, version, rule_type, rate, unit, rounding, minimum,
                                              tax_category, created_by, created_at)
                    values (:id, :studioId, :version, :ruleType, :rate, :unit, :rounding, :minimum,
                            :taxCategory, :createdBy, :createdAt)
                    """)
                    .param("id", Uuids.v7())
                    .param("studioId", caller.studioId())
                    .param("version", next.version())
                    .param("ruleType", ruleType(rule))
                    .param("rate", rule.rate())
                    .param("unit", unit(rule))
                    .param("rounding", rounding(rule))
                    .param("minimum", minimum)
                    .param("taxCategory", next.taxCategory())
                    .param("createdBy", caller.accountId())
                    .param("createdAt", Instant.now().truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC))
                    .update();
        } catch (DuplicateKeyException race) {
            throw new ConflictException("Someone else changed the fees at the same time. Reload and try again.");
        }
        return next;
    }

    /** Prices a piece with the current schedule, in the studio's currency. */
    @Transactional(readOnly = true)
    public FeeQuote quote(PieceSize size) {
        Currency currency = studios.current().currency();
        return FeeCalculator.quote(current(), size, currency);
    }

    private static String ruleType(FeeRule rule) {
        return switch (rule) {
            case FeeRule.PerVolume _ -> "PER_VOLUME";
            case FeeRule.PerWeight _ -> "PER_WEIGHT";
            case FeeRule.Flat _ -> "FLAT";
        };
    }

    private static @Nullable String unit(FeeRule rule) {
        return switch (rule) {
            case FeeRule.PerVolume volume -> volume.unit().name();
            case FeeRule.PerWeight weight -> weight.unit().name();
            case FeeRule.Flat _ -> null;
        };
    }

    private static String rounding(FeeRule rule) {
        return switch (rule) {
            case FeeRule.PerVolume volume -> volume.rounding().name();
            case FeeRule.PerWeight weight -> weight.rounding().name();
            case FeeRule.Flat _ -> FeeRule.DimensionRounding.NONE.name();
        };
    }

    record Row(int version, String ruleType, BigDecimal rate, @Nullable String unit, String rounding,
            BigDecimal minimum, String taxCategory) {

        FeeSchedule toSchedule() {
            FeeRule.DimensionRounding dimensionRounding = FeeRule.DimensionRounding.valueOf(rounding);
            FeeRule rule = switch (ruleType) {
                case "PER_VOLUME" -> new FeeRule.PerVolume(rate, FeeRule.VolumeUnit.valueOf(unit), dimensionRounding);
                case "PER_WEIGHT" -> new FeeRule.PerWeight(rate, WeightUnit.valueOf(unit), dimensionRounding);
                case "FLAT" -> new FeeRule.Flat(rate);
                default -> throw new IllegalStateException("Unknown rule type " + ruleType);
            };
            return new FeeSchedule(version, rule, minimum, taxCategory);
        }

    }

}
