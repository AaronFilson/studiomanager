package io.github.aaronfilson.studiomanager.firing;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One version of a studio's firing-fee schedule. Versions are never edited, so a piece's fee can
 * always be explained by the version it was priced with.
 *
 * @param minimum the least any piece is charged
 * @param taxCategory the category the ledger taxes these fees under (for example "FIRING_SERVICE");
 *     rates by place and date belong to the ledger, not here
 */
public record FeeSchedule(int version, FeeRule rule, BigDecimal minimum, String taxCategory) {

    public static final String DEFAULT_TAX_CATEGORY = "FIRING_SERVICE";

    public FeeSchedule {
        Objects.requireNonNull(rule, "rule");
        Objects.requireNonNull(minimum, "minimum");
        Objects.requireNonNull(taxCategory, "taxCategory");
        if (version < 0) {
            throw new IllegalArgumentException("A version can't be negative");
        }
        if (minimum.signum() < 0) {
            throw new IllegalArgumentException("A minimum can't be negative");
        }
        if (taxCategory.isBlank() || taxCategory.length() > 40) {
            throw new IllegalArgumentException("A tax category needs 1 to 40 characters");
        }
    }

    /** What a studio has before anyone sets fees: everything fires free. Version 0 is never stored. */
    public static FeeSchedule free() {
        return new FeeSchedule(0, new FeeRule.Flat(BigDecimal.ZERO), BigDecimal.ZERO, DEFAULT_TAX_CATEGORY);
    }

}
