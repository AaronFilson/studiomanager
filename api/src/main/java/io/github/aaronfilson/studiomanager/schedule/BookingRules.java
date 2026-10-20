package io.github.aaronfilson.studiomanager.schedule;

import java.util.List;

import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.BookingKind;

import org.jspecify.annotations.Nullable;

/**
 * Whether a new booking fits alongside the ones it overlaps. Pure, so the rules are tested on
 * their own.
 *
 * <p>Reservations share a resource up to its capacity. Anything else (a class, a firing,
 * maintenance, a closure) needs the whole resource. Overlap is counted over the whole interval,
 * which is exact for the fixed sessions studios book and errs on the safe side otherwise.
 */
final class BookingRules {

    private BookingRules() {
    }

    /** Why {@code kind} can't be booked over {@code overlapping}, or null if it can. */
    static @Nullable String clash(BookingKind kind, int capacity, List<BookingKind> overlapping) {
        long blocking = overlapping.stream().filter(BookingKind::blocksWholeResource).count();
        long reservations = overlapping.size() - blocking;
        if (blocking > 0) {
            return "That time is already taken by a class, firing, maintenance or closure.";
        }
        if (kind.blocksWholeResource() && reservations > 0) {
            return "%d %s already booked in that time. Cancel or move %s first."
                    .formatted(reservations, reservations == 1 ? "reservation is" : "reservations are",
                            reservations == 1 ? "it" : "them");
        }
        if (!kind.blocksWholeResource() && reservations >= capacity) {
            return "That time is full: all %d places are taken.".formatted(capacity);
        }
        return null;
    }

}
