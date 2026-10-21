package io.github.aaronfilson.studiomanager.schedule;

import java.time.Instant;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.access.Duty;

import org.jspecify.annotations.Nullable;

/** The calendar and roster's types. */
public final class ScheduleModel {

    private ScheduleModel() {
    }

    public enum ResourceKind {
        KILN,
        WHEEL_ROOM,
        GLAZE_ROOM,
        STUDIO,
        EQUIPMENT,
        OTHER
    }

    /**
     * What a booking is for. A RESERVATION takes one of the resource's seats; every other kind
     * blocks the whole resource (a class in the wheel room, a kiln down for maintenance, a closure).
     */
    public enum BookingKind {
        RESERVATION,
        FIRING,
        MAINTENANCE,
        CLASS,
        EVENT,
        RENTAL,
        CLOSURE;

        public boolean blocksWholeResource() {
            return this != RESERVATION;
        }
    }

    /**
     * @param assetId the maintained asset this is, if any; while it is down, only maintenance and
     *     closures can be booked
     * @param downReason why that asset is out of service; null when it's in service or there's none
     */
    public record Resource(UUID id, String name, ResourceKind kind, int capacity,
            @Nullable UUID requiredCertification, @Nullable String requiredCertificationName, @Nullable UUID assetId,
            @Nullable String downReason, long version) {
    }

    /**
     * A booking on the calendar. For members looking at someone else's reservation, the title
     * is "Reserved" and the person is left out.
     */
    public record Booking(UUID id, UUID resourceId, String resourceName, BookingKind kind, String title,
            Instant startsAt, Instant endsAt, @Nullable UUID accountId, @Nullable String accountName, boolean mine) {
    }

    public record Shift(UUID id, String title, @Nullable Duty duty, Instant startsAt, Instant endsAt,
            @Nullable UUID accountId, @Nullable String accountName, @Nullable Instant workedAt, long version) {

        public boolean open() {
            return accountId == null;
        }

        public int minutes() {
            return (int) java.time.Duration.between(startsAt, endsAt).toMinutes();
        }
    }

    public record HoursEntry(UUID id, int minutes, @Nullable String note, @Nullable UUID shiftId, Instant creditedAt,
            @Nullable String creditedByName) {
    }

    public record HoursLedger(UUID accountId, @Nullable String accountName, int totalMinutes,
            java.util.List<HoursEntry> entries) {
    }

}
