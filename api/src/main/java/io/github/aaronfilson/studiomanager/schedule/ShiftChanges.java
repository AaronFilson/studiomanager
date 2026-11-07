package io.github.aaronfilson.studiomanager.schedule;

import io.github.aaronfilson.studiomanager.access.AccessService;
import io.github.aaronfilson.studiomanager.access.Duty;
import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.Shift;
import io.github.aaronfilson.studiomanager.studio.MemberView;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The roster: setting up shifts, taking and assigning them, asking for cover, and confirming they
 * were worked.
 *
 * <p>Each change is flushed before an event is published, since the listeners write notices with
 * SQL in the same transaction (ADR 21).
 */
@Service
class ShiftChanges {

  private final RosterShiftRepository shifts;
  private final ScheduleService reads;
  private final CreditedHours hours;
  private final AccessService access;
  private final StudioMembers members;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  ShiftChanges(
      RosterShiftRepository shifts,
      ScheduleService reads,
      CreditedHours hours,
      AccessService access,
      StudioMembers members,
      ApplicationEventPublisher events,
      Clock clock) {
    this.shifts = shifts;
    this.reads = reads;
    this.hours = hours;
    this.access = access;
    this.members = members;
    this.events = events;
    this.clock = clock;
  }

  /**
   * Creates the same slot every week for {@code weeks} weeks, starting {@code firstStart}. Studios
   * set rosters per session (say, ten weeks of "Monitor, Tuesday 6–9pm").
   */
  @Transactional
  public List<Shift> createWeeklyShifts(
      @Nullable String title, @Nullable Duty duty, Instant firstStart, int minutes, int weeks) {
    ScheduleService.requireStaff();
    String cleaned = title == null ? "" : title.strip();
    if (cleaned.isEmpty() || cleaned.length() > 120) {
      throw new InvalidInputException("Give the shift a title of 1 to 120 characters.");
    }
    if (minutes < 15 || minutes > 24 * 60 || weeks < 1 || weeks > 26) {
      throw new InvalidInputException(
          "A shift lasts 15 minutes to 24 hours, repeated for 1 to 26 weeks.");
    }
    List<RosterShift> created = new ArrayList<>(weeks);
    for (int week = 0; week < weeks; week++) {
      Instant start = firstStart.plus(Duration.ofDays(7L * week));
      created.add(new RosterShift(cleaned, duty, start, start.plus(Duration.ofMinutes(minutes))));
    }
    shifts.saveAllAndFlush(created);
    return created.stream().map(reads::shiftView).toList();
  }

  /**
   * Assigns a shift. Members can take an open shift, or one someone has offered, for themselves if
   * they have its duty; staff can assign anyone, or pass null to open it up again. Taking an
   * offered shift tells whoever had it that they're covered.
   */
  @Transactional
  public Shift assign(UUID shiftId, @Nullable UUID accountId, long expectedVersion) {
    TenantContext caller = TenantContext.require();
    RosterShift shift = reads.findShift(shiftId);
    Shift before = reads.shiftView(shift);
    if (before.workedAt() != null) {
      throw new InvalidInputException("This shift has already been worked.");
    }
    if (!ScheduleService.isStaff(caller)) {
      boolean takeable =
          before.open() || (before.offered() && !caller.accountId().equals(before.accountId()));
      if (accountId == null || !accountId.equals(caller.accountId()) || !takeable) {
        throw new AccessDeniedException(
            "Members can only take open or offered shifts for themselves.");
      }
      Duty duty = before.duty();
      if (duty != null && !access.dutiesOf(caller.accountId()).contains(duty)) {
        throw new AccessDeniedException(
            "This shift is for members with the %s duty."
                .formatted(duty.name().toLowerCase(Locale.ROOT).replace('_', ' ')));
      }
    } else if (accountId != null && members.roleOf(accountId).isEmpty()) {
      throw new InvalidInputException("That person isn't a member of this studio.");
    }
    requireVersion(shift, expectedVersion);
    shift.assign(accountId);
    shifts.flush();
    Shift assigned = reads.shiftView(shift);
    UUID previousHolder = before.accountId();
    if (before.offered()
        && previousHolder != null
        && accountId != null
        && !accountId.equals(previousHolder)) {
      events.publishEvent(
          new ShiftCovered(
              caller.requireStudioId(),
              shiftId,
              before.title(),
              before.startsAt(),
              previousHolder,
              assigned.accountName()));
    }
    return assigned;
  }

  /**
   * Asks for cover, or stops asking. Only whoever has the shift (or staff) can; they stay on it
   * until someone takes it, so asking never leaves it uncovered. Everyone who could work it hears
   * about the offer.
   */
  @Transactional
  public Shift offer(UUID shiftId, boolean offered, long expectedVersion) {
    TenantContext caller = TenantContext.require();
    RosterShift shift = reads.findShift(shiftId);
    Shift before = reads.shiftView(shift);
    UUID holder = before.accountId();
    if (holder == null) {
      throw new InvalidInputException(
          "Nobody has this shift; it's already open for anyone to take.");
    }
    if (!ScheduleService.isStaff(caller) && !caller.accountId().equals(holder)) {
      throw new AccessDeniedException("Only whoever has this shift can ask for cover.");
    }
    Instant now = clock.instant();
    if (before.workedAt() != null || !before.endsAt().isAfter(now)) {
      throw new InvalidInputException("This shift is over.");
    }
    requireVersion(shift, expectedVersion);
    shift.setOfferedAt(offered ? now : null);
    shifts.flush();
    Shift result = reads.shiftView(shift);
    if (offered && !before.offered()) {
      events.publishEvent(
          new ShiftOffered(
              caller.requireStudioId(),
              shiftId,
              before.title(),
              before.startsAt(),
              before.endsAt(),
              result.accountName(),
              couldCover(holder, before.duty())));
    }
    return result;
  }

  /** Staff confirm a shift was worked, which credits its hours to the person who worked it. */
  @Transactional
  public Shift markWorked(UUID shiftId, long expectedVersion) {
    TenantContext caller = ScheduleService.requireStaff();
    RosterShift shift = reads.findShift(shiftId);
    Shift before = reads.shiftView(shift);
    UUID holder = before.accountId();
    if (holder == null) {
      throw new InvalidInputException("Nobody is assigned to this shift.");
    }
    if (before.workedAt() != null) {
      throw new InvalidInputException("This shift is already marked as worked.");
    }
    requireVersion(shift, expectedVersion);
    Instant now = clock.instant();
    shift.markWorked(now);
    hours.credit(caller, holder, before.minutes(), before.title(), shiftId, now);
    shifts.flush();
    return reads.shiftView(shift);
  }

  /** Members who could take this shift: those with its duty, or everyone if it has none. */
  private List<UUID> couldCover(UUID holder, @Nullable Duty duty) {
    if (duty == null) {
      return members.list().stream()
          .map(MemberView::accountId)
          .filter(id -> !id.equals(holder))
          .toList();
    }
    return access.allDuties().entrySet().stream()
        .filter(entry -> entry.getValue().contains(duty) && !entry.getKey().equals(holder))
        .map(Map.Entry::getKey)
        .sorted()
        .toList();
  }

  /**
   * Checks the shift is still the version the client saw. {@code @Version} then catches anyone who
   * changes it between the load and the commit.
   */
  private static void requireVersion(RosterShift shift, long expectedVersion) {
    if (shift.version() != expectedVersion) {
      throw ConflictException.staleVersion(
          "Someone else changed this shift at the same time. Reload and try again.");
    }
  }
}
