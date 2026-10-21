package io.github.aaronfilson.studiomanager.schedule;

import java.time.ZoneOffset;

import io.github.aaronfilson.studiomanager.contract.BookingDto;
import io.github.aaronfilson.studiomanager.contract.BookingKindDto;
import io.github.aaronfilson.studiomanager.contract.DutyDto;
import io.github.aaronfilson.studiomanager.contract.ShiftDto;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.Booking;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.Shift;

/** Converts calendar and roster entries to the HTTP contract. */
public final class ScheduleDtos {

    private ScheduleDtos() {
    }

    public static BookingDto toDto(Booking booking) {
        return new BookingDto()
                .id(booking.id())
                .resourceId(booking.resourceId())
                .resourceName(booking.resourceName())
                // fromValue, not valueOf: the generator renames the CLASS constant
                .kind(BookingKindDto.fromValue(booking.kind().name()))
                .title(booking.title())
                .startsAt(booking.startsAt().atOffset(ZoneOffset.UTC))
                .endsAt(booking.endsAt().atOffset(ZoneOffset.UTC))
                .accountId(booking.accountId())
                .accountName(booking.accountName())
                .mine(booking.mine());
    }

    public static ShiftDto toDto(Shift shift) {
        return new ShiftDto()
                .id(shift.id())
                .title(shift.title())
                .duty(shift.duty() == null ? null : DutyDto.valueOf(shift.duty().name()))
                .startsAt(shift.startsAt().atOffset(ZoneOffset.UTC))
                .endsAt(shift.endsAt().atOffset(ZoneOffset.UTC))
                .accountId(shift.accountId())
                .accountName(shift.accountName())
                .open(shift.open())
                .minutes(shift.minutes())
                .workedAt(shift.workedAt() == null ? null : shift.workedAt().atOffset(ZoneOffset.UTC))
                .version(shift.version());
    }

}
