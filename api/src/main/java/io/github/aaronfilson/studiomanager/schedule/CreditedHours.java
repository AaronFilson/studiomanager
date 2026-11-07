package io.github.aaronfilson.studiomanager.schedule;

import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioPermissions;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.HoursLedger;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Crediting hours: for a shift worked, or a correction by staff. */
@Service
class CreditedHours {

  private final HoursCreditRepository hours;
  private final ScheduleService reads;
  private final StudioMembers members;
  private final Clock clock;
  private final StudioPermissions permissions;

  CreditedHours(
      HoursCreditRepository hours,
      ScheduleService reads,
      StudioMembers members,
      Clock clock,
      StudioPermissions permissions) {
    this.hours = hours;
    this.reads = reads;
    this.members = members;
    this.clock = clock;
    this.permissions = permissions;
  }

  /** A manual correction to someone's hours, positive or negative. */
  @Transactional
  public HoursLedger adjustHours(UUID accountId, int minutes, @Nullable String note) {
    TenantContext caller =
        permissions.require(Permission.SCHEDULE_HOURS_MANAGE, ScheduleService.ONLY_STAFF);
    if (minutes == 0 || Math.abs(minutes) > 6000) {
      throw new InvalidInputException("Adjust by up to 100 hours either way.");
    }
    if (note == null || note.isBlank() || note.strip().length() > 200) {
      throw new InvalidInputException("Say why, in up to 200 characters.");
    }
    if (members.roleOf(accountId).isEmpty()) {
      throw new InvalidInputException("That person isn't a member of this studio.");
    }
    credit(caller, accountId, minutes, note.strip(), null, clock.instant());
    hours.flush();
    return reads.hours(accountId);
  }

  /** Credits {@code minutes} to someone; the note is cut to fit. */
  void credit(
      TenantContext caller,
      UUID accountId,
      int minutes,
      String note,
      @Nullable UUID shiftId,
      Instant at) {
    hours.save(
        new HoursCredit(
            accountId,
            shiftId,
            minutes,
            note.length() > 200 ? note.substring(0, 200) : note,
            caller.accountId(),
            at));
  }
}
