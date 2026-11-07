package io.github.aaronfilson.studiomanager.schedule;

import io.github.aaronfilson.studiomanager.access.AccessService;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceService;
import io.github.aaronfilson.studiomanager.platform.Capped;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.Booking;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.BookingKind;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.HoursEntry;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.HoursLedger;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.Resource;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.Shift;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The studio calendar (resources and bookings) and the roster (shifts and credited hours): the
 * reads other modules and the controller use. Changes are in {@link ResourceChanges}, {@link
 * BookingChanges}, {@link ShiftChanges} and {@link CreditedHours}.
 *
 * <p>Every list here is a fixed number of queries: the rows (with their resource, for bookings),
 * then the names of the people on them.
 */
@Service
public class ScheduleService {

  static final Duration LONGEST_RANGE = Duration.ofDays(62);

  /** The most bookings or shifts one range returns. */
  static final int RANGE_LIMIT = 2000;

  /** The most hours entries {@link #hours} lists. The total counts them all. */
  static final int HOURS_ENTRIES_LIMIT = 1000;

  private final BookableResourceRepository resources;
  private final CalendarBookingRepository bookings;
  private final RosterShiftRepository shifts;
  private final HoursCreditRepository hours;
  private final AccessService access;
  private final AccountDirectory accounts;
  private final MaintenanceService maintenance;

  ScheduleService(
      BookableResourceRepository resources,
      CalendarBookingRepository bookings,
      RosterShiftRepository shifts,
      HoursCreditRepository hours,
      AccessService access,
      AccountDirectory accounts,
      MaintenanceService maintenance) {
    this.resources = resources;
    this.bookings = bookings;
    this.shifts = shifts;
    this.hours = hours;
    this.access = access;
    this.accounts = accounts;
    this.maintenance = maintenance;
  }

  // ---- Resources

  /** Everything bookable, by name, with whether its asset is down. */
  @Transactional(readOnly = true)
  public List<Resource> resources() {
    Map<UUID, String> down = maintenance.downReasons();
    Map<UUID, String> certifications =
        access.types().stream()
            .collect(
                Collectors.toMap(
                    AccessService.CertificationType::id, AccessService.CertificationType::name));
    return resources.findAllByName().stream()
        .map(resource -> {
          UUID certification = resource.requiredCertification();
          UUID assetId = resource.assetId();
          return resource.toView(
              certification == null ? null : certifications.get(certification),
              assetId == null ? null : down.get(assetId));
        })
        .toList();
  }

  // ---- Bookings

  /** Bookings that overlap [{@code from}, {@code to}), at most 62 days. */
  @Transactional(readOnly = true)
  public Capped<Booking> bookings(Instant from, Instant to) {
    requireRange(from, to);
    return Capped.fromFetched(
        bookingViews(bookings.findOverlapping(from, to, Limit.of(Capped.fetchSize(RANGE_LIMIT)))),
        RANGE_LIMIT);
  }

  /**
   * Bookings as the caller may see them. For members looking at someone else's reservation, the
   * title is "Reserved" and the person is left out.
   */
  List<Booking> bookingViews(List<CalendarBooking> found) {
    TenantContext caller = TenantContext.require();
    boolean staff = isStaff(caller);
    Map<UUID, String> names =
        accounts.displayNames(
            found.stream()
                .flatMap(booking -> Stream.ofNullable(booking.accountId()))
                .distinct()
                .toList());
    return found.stream()
        .map(booking -> {
          UUID holder = booking.accountId();
          boolean mine = caller.accountId().equals(holder);
          boolean hidden = !staff && !mine && booking.kind() == BookingKind.RESERVATION;
          Booking view = booking.toView(holder == null ? null : names.get(holder), mine);
          return hidden ? hide(view) : view;
        })
        .toList();
  }

  // ---- Shifts and hours

  @Transactional(readOnly = true)
  public Capped<Shift> shifts(Instant from, Instant to) {
    requireRange(from, to);
    return Capped.fromFetched(
        shiftViews(shifts.findOverlapping(from, to, Limit.of(Capped.fetchSize(RANGE_LIMIT)))),
        RANGE_LIMIT);
  }

  /** Someone's credited hours. Members can see only their own. */
  @Transactional(readOnly = true)
  public HoursLedger hours(UUID accountId) {
    TenantContext caller = TenantContext.require();
    if (!accountId.equals(caller.accountId()) && !isStaff(caller)) {
      throw new AccessDeniedException("You can see only your own hours.");
    }
    List<HoursCredit> credits =
        hours.findNewestFor(accountId, Limit.of(Capped.fetchSize(HOURS_ENTRIES_LIMIT)));
    Set<UUID> people = new HashSet<>();
    people.add(accountId);
    credits.forEach(credit -> people.add(credit.creditedBy()));
    Map<UUID, String> names = accounts.displayNames(people);
    Capped<HoursEntry> entries =
        Capped.fromFetched(credits, HOURS_ENTRIES_LIMIT)
            .map(credit -> credit.toView(names.get(credit.creditedBy())));
    return new HoursLedger(
        accountId,
        names.get(accountId),
        Math.toIntExact(hours.totalMinutes(accountId)),
        entries);
  }

  // ---- For the other services in this package

  RosterShift findShift(UUID shiftId) {
    return shifts.findById(shiftId).orElseThrow(() -> new NotFoundException("No such shift."));
  }

  Shift shiftView(RosterShift shift) {
    return shiftViews(List.of(shift)).getFirst();
  }

  static boolean isStaff(TenantContext caller) {
    StudioRole role = caller.role();
    return role != null && role.includes(StudioRole.STAFF);
  }

  static TenantContext requireStaff() {
    TenantContext caller = TenantContext.require();
    if (!isStaff(caller)) {
      throw new AccessDeniedException("Only staff can do this.");
    }
    return caller;
  }

  private List<Shift> shiftViews(List<RosterShift> found) {
    Map<UUID, String> names =
        accounts.displayNames(
            found.stream()
                .flatMap(shift -> Stream.ofNullable(shift.accountId()))
                .distinct()
                .toList());
    return found.stream()
        .map(shift -> {
          UUID holder = shift.accountId();
          return shift.toView(holder == null ? null : names.get(holder));
        })
        .toList();
  }

  private static Booking hide(Booking booking) {
    return new Booking(
        booking.id(),
        booking.resourceId(),
        booking.resourceName(),
        booking.kind(),
        "Reserved",
        booking.startsAt(),
        booking.endsAt(),
        null,
        null,
        false);
  }

  private static void requireRange(Instant from, Instant to) {
    if (!to.isAfter(from) || Duration.between(from, to).compareTo(LONGEST_RANGE) > 0) {
      throw new InvalidInputException("Ask for a range of up to 62 days.");
    }
  }
}
