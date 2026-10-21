package io.github.aaronfilson.studiomanager.maintenance;

import java.time.Duration;
import java.time.Instant;

import org.jspecify.annotations.Nullable;

/**
 * When a recurring task falls due: every N days, every N firings, or whichever comes first.
 *
 * <p>"Due soon" is a tenth of the interval ahead, so a yearly check warns two weeks out (the cap)
 * and a weekly one a day out. For firings it's a tenth too, at least one, so "every 10 firings"
 * warns one firing ahead.
 */
final class DueRule {

    static final Duration MAX_DAYS_WARNING = Duration.ofDays(14);

    private DueRule() {
    }

    /**
     * @param totalFirings the kiln's firings so far; null for an asset that isn't a kiln
     */
    static Due evaluate(@Nullable Integer everyDays, @Nullable Integer everyFirings, Instant lastDoneAt,
            int lastDoneFirings, @Nullable Integer totalFirings, Instant now) {
        Instant dueAt = everyDays == null ? null : lastDoneAt.plus(Duration.ofDays(everyDays));
        Integer firingsLeft = everyFirings == null || totalFirings == null ? null
                : everyFirings - (totalFirings - lastDoneFirings);

        DueStatus byDays = DueStatus.OK;
        if (dueAt != null && everyDays != null) {
            Duration warning = clamp(Duration.ofDays(everyDays).dividedBy(10), Duration.ofDays(1), MAX_DAYS_WARNING);
            if (!dueAt.isAfter(now)) {
                byDays = DueStatus.DUE;
            } else if (!dueAt.minus(warning).isAfter(now)) {
                byDays = DueStatus.DUE_SOON;
            }
        }
        DueStatus byFirings = DueStatus.OK;
        if (firingsLeft != null && everyFirings != null) {
            if (firingsLeft <= 0) {
                byFirings = DueStatus.DUE;
            } else if (firingsLeft <= Math.max(1, everyFirings / 10)) {
                byFirings = DueStatus.DUE_SOON;
            }
        }
        return new Due(byDays.compareTo(byFirings) >= 0 ? byDays : byFirings, dueAt, firingsLeft);
    }

    private static Duration clamp(Duration value, Duration min, Duration max) {
        return value.compareTo(min) < 0 ? min : value.compareTo(max) > 0 ? max : value;
    }

    /**
     * @param dueAt when the days interval runs out; null without one
     * @param firingsLeft firings until the firings interval runs out; null without one
     */
    record Due(DueStatus status, @Nullable Instant dueAt, @Nullable Integer firingsLeft) {
    }

}
