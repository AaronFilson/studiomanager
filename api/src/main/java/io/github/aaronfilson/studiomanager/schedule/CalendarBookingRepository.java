package io.github.aaronfilson.studiomanager.schedule;

import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.BookingKind;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Bookings, always with their resource when listed, since the calendar shows its name. */
interface CalendarBookingRepository extends JpaRepository<CalendarBooking, UUID> {

  /** Bookings that overlap [{@code from}, {@code to}), soonest first. */
  @Query(
      """
      select b from CalendarBooking b join fetch b.resource r
      where b.startsAt < :to and b.endsAt > :from
      order by b.startsAt, r.name
      """)
  List<CalendarBooking> findOverlapping(Instant from, Instant to, Limit limit);

  /** What's already booked on one resource in [{@code startsAt}, {@code endsAt}). */
  @Query(
      """
      select b.kind from CalendarBooking b
      where b.resource.id = :resourceId and b.startsAt < :endsAt and b.endsAt > :startsAt
      """)
  List<BookingKind> findKindsOverlapping(UUID resourceId, Instant startsAt, Instant endsAt);

  /** A firing's bookings: one on each resource that is its kiln. */
  List<CalendarBooking> findByFiringId(UUID firingId);
}
