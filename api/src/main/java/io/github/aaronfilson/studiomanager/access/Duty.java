package io.github.aaronfilson.studiomanager.access;

import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import java.util.Set;

/**
 * A job someone does in a studio on top of their role. The research found these run studios as much
 * as owners do: technicians, open-studio monitors, work-exchange members who trade hours for
 * membership, co-op volunteer officers (kiln chair, treasurer) and instructors.
 */
public enum Duty {
  TECHNICIAN,
  MONITOR,
  WORK_EXCHANGE,
  VOLUNTEER_OFFICER,
  INSTRUCTOR;

  /**
   * The permissions this duty adds to its holder's role. Fixed in code; owners can't edit them
   * (plan 21, decision 6). Each request adds them to the holder's permissions ({@link DutyGrants}).
   */
  public Set<Permission> grants() {
    return switch (this) {
      case TECHNICIAN ->
          Set.of(Permission.MAINTENANCE_COMPLETE_ASSIGNED, Permission.MAINTENANCE_REPORTS_RESOLVE);
      case MONITOR ->
          Set.of(Permission.SCHEDULE_SHIFTS_TAKE_DUTY, Permission.PIECES_INTAKE_FOR_OTHERS);
      case WORK_EXCHANGE ->
          Set.of(Permission.SCHEDULE_SHIFTS_TAKE_DUTY, Permission.SCHEDULE_HOURS_LOG_OWN);
      case VOLUNTEER_OFFICER -> Set.of(Permission.SCHEDULE_SHIFTS_TAKE_DUTY);
      case INSTRUCTOR -> Set.of(Permission.SCHEDULE_SHIFTS_TAKE_DUTY);
    };
  }
}
