package io.github.aaronfilson.studiomanager.schedule;

import io.github.aaronfilson.studiomanager.access.AccessService;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceService;
import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.Booking;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.BookingKind;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Booking a resource, and cancelling a booking.
 *
 * <p>Booking a resource locks its row first, so two people can't both take its last seat: the
 * check-then-insert runs one booking at a time per resource.
 */
@Service
class BookingChanges {

  private final BookableResourceRepository resources;
  private final CalendarBookingRepository bookings;
  private final ScheduleService reads;
  private final AccessService access;
  private final StudioMembers members;
  private final MaintenanceService maintenance;

  BookingChanges(
      BookableResourceRepository resources,
      CalendarBookingRepository bookings,
      ScheduleService reads,
      AccessService access,
      StudioMembers members,
      MaintenanceService maintenance) {
    this.resources = resources;
    this.bookings = bookings;
    this.reads = reads;
    this.access = access;
    this.members = members;
    this.maintenance = maintenance;
  }

  /**
   * Books a resource. Members can reserve a seat for themselves, if they hold the resource's
   * required certification; staff can book anything for anyone.
   */
  @Transactional
  public Booking book(
      UUID resourceId,
      BookingKind kind,
      @Nullable String title,
      Instant startsAt,
      Instant endsAt,
      @Nullable UUID forAccount) {
    TenantContext caller = TenantContext.require();
    boolean staff = caller.holds(Permission.SCHEDULE_BOOK_FOR_OTHERS);
    UUID person =
        forAccount == null
            ? (kind == BookingKind.RESERVATION ? caller.accountId() : null)
            : forAccount;
    if (!staff && (kind != BookingKind.RESERVATION || !caller.accountId().equals(person))) {
      throw new AccessDeniedException("Members can only reserve time for themselves.");
    }
    if (!endsAt.isAfter(startsAt)
        || Duration.between(startsAt, endsAt).compareTo(Duration.ofDays(14)) > 0) {
      throw new InvalidInputException("A booking ends after it starts, and lasts at most 14 days.");
    }
    if (person != null && members.roleOf(person).isEmpty()) {
      throw new InvalidInputException("That person isn't a member of this studio.");
    }
    BookableResource resource =
        resources
            .findForBooking(resourceId)
            .orElseThrow(() -> new NotFoundException("No such resource."));
    UUID assetId = resource.assetId();
    String downReason = assetId == null ? null : maintenance.downReasons().get(assetId);
    if (downReason != null && kind != BookingKind.MAINTENANCE && kind != BookingKind.CLOSURE) {
      throw new ConflictException(
          "%s is out of service: %s. Only its maintenance or a closure can be booked now."
              .formatted(resource.name(), downReason));
    }
    UUID certification = resource.requiredCertification();
    if (kind == BookingKind.RESERVATION
        && person != null
        && certification != null
        && !access.isCertified(person, certification, startsAt)) {
      throw new InvalidInputException(
          "%s needs a certification first: %s."
              .formatted(resource.name(), access.type(certification).name()));
    }
    List<BookingKind> overlapping = bookings.findKindsOverlapping(resourceId, startsAt, endsAt);
    String clash = BookingRules.clash(kind, resource.capacity(), overlapping);
    if (clash != null) {
      throw new ConflictException(clash);
    }
    CalendarBooking booking =
        CalendarBooking.made(resource, kind, cleanTitle(title, kind), startsAt, endsAt, person);
    bookings.saveAndFlush(booking);
    return reads.bookingViews(List.of(booking)).getFirst();
  }

  /** Cancels a booking: your own reservation, or anything if you're staff. */
  @Transactional
  public void cancelBooking(UUID bookingId) {
    TenantContext caller = TenantContext.require();
    CalendarBooking booking =
        bookings
            .findById(bookingId)
            .filter(found ->
                caller.holds(Permission.SCHEDULE_BOOK_FOR_OTHERS)
                    || caller.accountId().equals(found.accountId()))
            .orElseThrow(
                () -> new NotFoundException("No such booking, or not one you can cancel."));
    bookings.delete(booking);
    bookings.flush();
  }

  private static String cleanTitle(@Nullable String title, BookingKind kind) {
    if (title != null && !title.isBlank()) {
      String cleaned = title.strip();
      if (cleaned.length() > 120) {
        throw new InvalidInputException("Keep the title under 120 characters.");
      }
      return cleaned;
    }
    return switch (kind) {
      case RESERVATION -> "Reserved";
      case FIRING, MAINTENANCE, CLASS, EVENT, RENTAL, CLOSURE ->
          kind.name().charAt(0) + kind.name().substring(1).toLowerCase(Locale.ROOT);
    };
  }
}
