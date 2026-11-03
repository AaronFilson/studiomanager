package io.github.aaronfilson.studiomanager.webhook;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * When a delivery that failed is tried again, and when an endpoint that keeps failing is turned
 * off. The waits grow, so an endpoint that's down for an afternoon catches up by itself, while one
 * that's gone for good stops being tried within about four days.
 */
final class Backoff {

    /** The wait after the 1st, 2nd, … failed attempt. */
    static final List<Duration> WAITS = List.of(Duration.ofMinutes(1), Duration.ofMinutes(5),
            Duration.ofMinutes(30), Duration.ofHours(2), Duration.ofHours(6), Duration.ofHours(12),
            Duration.ofHours(24), Duration.ofHours(24), Duration.ofHours(24));

    /** An endpoint failing for this long, with no success in between, is disabled. */
    static final Duration DISABLE_AFTER = Duration.ofDays(3);

    /** Or after this many failures in a row, however quickly they came. */
    static final int DISABLE_AFTER_FAILURES = 50;

    private Backoff() {
    }

    /** When to try again after {@code attempts} failed attempts, or empty to give up. */
    static Optional<Instant> nextAttempt(int attempts, Instant now) {
        if (attempts < 1 || attempts > WAITS.size()) {
            return Optional.empty();
        }
        return Optional.of(now.plus(WAITS.get(attempts - 1)));
    }

    static boolean shouldDisable(int consecutiveFailures, Instant failingSince, Instant now) {
        return consecutiveFailures >= DISABLE_AFTER_FAILURES || !failingSince.plus(DISABLE_AFTER).isAfter(now);
    }

    /** Whether a response means it arrived: any 2xx. */
    static boolean delivered(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

}
