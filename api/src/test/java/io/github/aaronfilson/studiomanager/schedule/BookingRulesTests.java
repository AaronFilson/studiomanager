package io.github.aaronfilson.studiomanager.schedule;

import static io.github.aaronfilson.studiomanager.schedule.ScheduleModel.BookingKind.CLASS;
import static io.github.aaronfilson.studiomanager.schedule.ScheduleModel.BookingKind.CLOSURE;
import static io.github.aaronfilson.studiomanager.schedule.ScheduleModel.BookingKind.RESERVATION;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class BookingRulesTests {

    @Test
    void reservationsShareUpToCapacity() {
        assertThat(BookingRules.clash(RESERVATION, 3, List.of(RESERVATION, RESERVATION))).isNull();
        assertThat(BookingRules.clash(RESERVATION, 3, List.of(RESERVATION, RESERVATION, RESERVATION)))
                .isEqualTo("That time is full: all 3 places are taken.");
    }

    @Test
    void aClassNeedsTheWholeRoom() {
        assertThat(BookingRules.clash(CLASS, 8, List.of())).isNull();
        assertThat(BookingRules.clash(CLASS, 8, List.of(RESERVATION)))
                .isEqualTo("1 reservation is already booked in that time. Cancel or move it first.");
    }

    @Test
    void nothingGoesOverABlockingBooking() {
        assertThat(BookingRules.clash(RESERVATION, 8, List.of(CLOSURE))).isNotNull();
        assertThat(BookingRules.clash(CLASS, 8, List.of(CLASS))).isNotNull();
    }

}
