package io.github.aaronfilson.studiomanager.access;

/**
 * A job someone does in a studio on top of their role. The research found these run studios as
 * much as owners do: technicians, open-studio monitors, work-exchange members who trade hours for
 * membership, co-op volunteer officers (kiln chair, treasurer) and instructors.
 */
public enum Duty {
    TECHNICIAN,
    MONITOR,
    WORK_EXCHANGE,
    VOLUNTEER_OFFICER,
    INSTRUCTOR
}
