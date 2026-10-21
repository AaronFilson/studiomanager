package io.github.aaronfilson.studiomanager.schedule;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.access.AccessService;
import io.github.aaronfilson.studiomanager.access.Duty;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceService;
import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.Booking;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.BookingKind;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.HoursEntry;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.HoursLedger;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.Resource;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.ResourceKind;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.Shift;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The studio calendar (resources and bookings) and the roster (shifts and credited hours).
 *
 * <p>Booking a resource locks its row first, so two people can't both take its last seat: the
 * check-then-insert runs one booking at a time per resource.
 */
@Service
public class ScheduleService {

    static final Duration LONGEST_RANGE = Duration.ofDays(62);

    private final JdbcClient jdbc;
    private final AccessService access;
    private final StudioMembers members;
    private final AccountDirectory accounts;
    private final MaintenanceService maintenance;
    private final ApplicationEventPublisher events;

    ScheduleService(JdbcClient jdbc, AccessService access, StudioMembers members, AccountDirectory accounts,
            MaintenanceService maintenance, ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.access = access;
        this.members = members;
        this.accounts = accounts;
        this.maintenance = maintenance;
        this.events = events;
    }

    // ---- Resources

    @Transactional(readOnly = true)
    public List<Resource> resources() {
        Map<UUID, String> down = maintenance.downReasons();
        return jdbc.sql("""
                select r.id, r.name, r.kind, r.capacity, r.required_certification, t.name as certification_name,
                       r.asset_id, r.version
                from resource r left join certification_type t on t.id = r.required_certification
                order by r.name
                """)
                .query((rs, row) -> mapResource(rs, rs.getString("certification_name"), down))
                .list();
    }

    /**
     * Adds something bookable. Linking it to a maintained asset means it can't be booked while the
     * asset is out of service, except for its repair or a closure.
     */
    @Transactional
    public Resource createResource(String name, ResourceKind kind, int capacity, @Nullable UUID requiredCertification,
            @Nullable UUID assetId) {
        TenantContext caller = requireStaff();
        String cleaned = validName(name);
        if (capacity < 1 || capacity > 200) {
            throw new InvalidInputException("Capacity is 1 to 200.");
        }
        if (requiredCertification != null) {
            access.type(requiredCertification);
        }
        if (assetId != null) {
            // Not found for an asset of another studio or one no longer tracked
            maintenance.asset(assetId, Instant.now());
        }
        UUID id = Uuids.v7();
        try {
            jdbc.sql("""
                    insert into resource (id, studio_id, name, kind, capacity, required_certification, asset_id, version)
                    values (:id, :studioId, :name, :kind, :capacity, :cert, :assetId, 0)
                    """)
                    .param("id", id)
                    .param("studioId", caller.studioId())
                    .param("name", cleaned)
                    .param("kind", kind.name())
                    .param("capacity", capacity)
                    .param("cert", requiredCertification)
                    .param("assetId", assetId)
                    .update();
        } catch (DuplicateKeyException e) {
            throw new InvalidInputException("There's already something called '%s' on the calendar.".formatted(cleaned));
        }
        return resource(id);
    }

    // ---- Bookings

    /** Bookings that overlap [{@code from}, {@code to}), at most 62 days. */
    @Transactional(readOnly = true)
    public List<Booking> bookings(Instant from, Instant to) {
        requireRange(from, to);
        TenantContext caller = TenantContext.require();
        boolean staff = isStaff(caller);
        List<Booking> found = jdbc.sql("""
                select b.id, b.resource_id, r.name as resource_name, b.kind, b.title, b.starts_at, b.ends_at, b.account_id
                from booking b join resource r on r.id = b.resource_id
                where b.starts_at < :to and b.ends_at > :from
                order by b.starts_at, r.name
                limit 2000
                """)
                .param("from", utc(from))
                .param("to", utc(to))
                .query(ScheduleService::mapBooking).list();
        Map<UUID, String> names = accounts.displayNames(found.stream().map(Booking::accountId)
                .filter(java.util.Objects::nonNull).toList());
        List<Booking> shown = new ArrayList<>(found.size());
        for (Booking b : found) {
            boolean mine = caller.accountId().equals(b.accountId());
            boolean hidden = !staff && !mine && b.kind() == BookingKind.RESERVATION;
            shown.add(new Booking(b.id(), b.resourceId(), b.resourceName(), b.kind(), hidden ? "Reserved" : b.title(),
                    b.startsAt(), b.endsAt(), hidden ? null : b.accountId(),
                    hidden || b.accountId() == null ? null : names.get(b.accountId()), mine));
        }
        return shown;
    }

    /**
     * Books a resource. Members can reserve a seat for themselves, if they hold the resource's
     * required certification; staff can book anything for anyone.
     */
    @Transactional
    public Booking book(UUID resourceId, BookingKind kind, String title, Instant startsAt, Instant endsAt,
            @Nullable UUID forAccount) {
        TenantContext caller = TenantContext.require();
        boolean staff = isStaff(caller);
        UUID person = forAccount == null ? (kind == BookingKind.RESERVATION ? caller.accountId() : null) : forAccount;
        if (!staff && (kind != BookingKind.RESERVATION || !caller.accountId().equals(person))) {
            throw new AccessDeniedException("Members can only reserve time for themselves.");
        }
        if (!endsAt.isAfter(startsAt) || Duration.between(startsAt, endsAt).compareTo(Duration.ofDays(14)) > 0) {
            throw new InvalidInputException("A booking ends after it starts, and lasts at most 14 days.");
        }
        if (person != null && members.roleOf(person).isEmpty()) {
            throw new InvalidInputException("That person isn't a member of this studio.");
        }
        // Serializes bookings on this resource until the transaction ends
        Map<UUID, String> down = maintenance.downReasons();
        Resource resource = jdbc.sql("""
                select r.id, r.name, r.kind, r.capacity, r.required_certification, r.asset_id, r.version
                from resource r where r.id = :id for update
                """)
                .param("id", resourceId)
                .query((rs, row) -> mapResource(rs, null, down))
                .optional()
                .orElseThrow(() -> new NotFoundException("No such resource."));
        if (resource.downReason() != null && kind != BookingKind.MAINTENANCE && kind != BookingKind.CLOSURE) {
            throw new ConflictException("%s is out of service: %s. Only its maintenance or a closure can be booked now."
                    .formatted(resource.name(), resource.downReason()));
        }
        if (kind == BookingKind.RESERVATION && person != null && resource.requiredCertification() != null
                && !access.isCertified(person, resource.requiredCertification(), startsAt)) {
            throw new InvalidInputException("%s needs a certification first: %s."
                    .formatted(resource.name(), access.type(resource.requiredCertification()).name()));
        }
        List<BookingKind> overlapping = jdbc.sql("""
                select kind from booking where resource_id = :id and starts_at < :end and ends_at > :start
                """)
                .param("id", resourceId)
                .param("start", utc(startsAt))
                .param("end", utc(endsAt))
                .query(String.class).list().stream()
                .map(BookingKind::valueOf)
                .toList();
        String clash = BookingRules.clash(kind, resource.capacity(), overlapping);
        if (clash != null) {
            throw new ConflictException(clash);
        }
        String cleanTitle = title == null || title.isBlank()
                ? (kind == BookingKind.RESERVATION ? "Reserved" : kind.name().charAt(0) + kind.name().substring(1).toLowerCase())
                : title.strip();
        if (cleanTitle.length() > 120) {
            throw new InvalidInputException("Keep the title under 120 characters.");
        }
        UUID id = Uuids.v7();
        jdbc.sql("""
                insert into booking (id, studio_id, resource_id, kind, title, starts_at, ends_at, account_id, created_by, created_at)
                values (:id, :studioId, :resourceId, :kind, :title, :start, :end, :account, :by, :now)
                """)
                .param("id", id)
                .param("studioId", caller.studioId())
                .param("resourceId", resourceId)
                .param("kind", kind.name())
                .param("title", cleanTitle)
                .param("start", utc(startsAt))
                .param("end", utc(endsAt))
                .param("account", person)
                .param("by", caller.accountId())
                .param("now", utc(now()))
                .update();
        return bookings(startsAt, endsAt).stream().filter(b -> b.id().equals(id)).findFirst().orElseThrow();
    }

    /** Cancels a booking: your own reservation, or anything if you're staff. */
    @Transactional
    public void cancelBooking(UUID bookingId) {
        TenantContext caller = TenantContext.require();
        int deleted = jdbc.sql("""
                delete from booking where id = :id and (:staff or account_id = :me)
                """)
                .param("id", bookingId)
                .param("staff", isStaff(caller))
                .param("me", caller.accountId())
                .update();
        if (deleted == 0) {
            throw new NotFoundException("No such booking, or not one you can cancel.");
        }
    }

    // ---- Shifts and hours

    /**
     * Creates the same slot every week for {@code weeks} weeks, starting {@code firstStart}. Studios
     * set rosters per session (say, ten weeks of "Monitor, Tuesday 6–9pm").
     */
    @Transactional
    public List<Shift> createWeeklyShifts(String title, @Nullable Duty duty, Instant firstStart, int minutes, int weeks) {
        TenantContext caller = requireStaff();
        String cleaned = title == null ? "" : title.strip();
        if (cleaned.isEmpty() || cleaned.length() > 120) {
            throw new InvalidInputException("Give the shift a title of 1 to 120 characters.");
        }
        if (minutes < 15 || minutes > 24 * 60 || weeks < 1 || weeks > 26) {
            throw new InvalidInputException("A shift lasts 15 minutes to 24 hours, repeated for 1 to 26 weeks.");
        }
        List<UUID> ids = new ArrayList<>();
        for (int week = 0; week < weeks; week++) {
            Instant start = firstStart.plus(Duration.ofDays(7L * week));
            UUID id = Uuids.v7();
            jdbc.sql("""
                    insert into shift (id, studio_id, title, duty, starts_at, ends_at, created_by, version)
                    values (:id, :studioId, :title, :duty, :start, :end, :by, 0)
                    """)
                    .param("id", id)
                    .param("studioId", caller.studioId())
                    .param("title", cleaned)
                    .param("duty", duty == null ? null : duty.name())
                    .param("start", utc(start))
                    .param("end", utc(start.plus(Duration.ofMinutes(minutes))))
                    .param("by", caller.accountId())
                    .update();
            ids.add(id);
        }
        return ids.stream().map(this::shift).toList();
    }

    @Transactional(readOnly = true)
    public List<Shift> shifts(Instant from, Instant to) {
        requireRange(from, to);
        return withNames(jdbc.sql(SELECT_SHIFT + "where starts_at < :to and ends_at > :from order by starts_at limit 2000")
                .param("from", utc(from))
                .param("to", utc(to))
                .query(ScheduleService::mapShift).list());
    }

    /**
     * Assigns a shift. Members can take an open shift, or one someone has offered, for themselves
     * if they have its duty; staff can assign anyone, or pass null to open it up again. Taking an
     * offered shift tells whoever had it that they're covered.
     */
    @Transactional
    public Shift assign(UUID shiftId, @Nullable UUID accountId, long expectedVersion) {
        TenantContext caller = TenantContext.require();
        Shift shift = shift(shiftId);
        if (shift.workedAt() != null) {
            throw new InvalidInputException("This shift has already been worked.");
        }
        if (!isStaff(caller)) {
            boolean takeable = shift.open() || (shift.offered() && !caller.accountId().equals(shift.accountId()));
            if (accountId == null || !accountId.equals(caller.accountId()) || !takeable) {
                throw new AccessDeniedException("Members can only take open or offered shifts for themselves.");
            }
            if (shift.duty() != null && !access.dutiesOf(caller.accountId()).contains(shift.duty())) {
                throw new AccessDeniedException("This shift is for members with the %s duty."
                        .formatted(shift.duty().name().toLowerCase().replace('_', ' ')));
            }
        } else if (accountId != null && members.roleOf(accountId).isEmpty()) {
            throw new InvalidInputException("That person isn't a member of this studio.");
        }
        int updated = jdbc.sql("""
                update shift set account_id = :account, offered_at = null, version = version + 1
                where id = :id and version = :version
                """)
                .param("account", accountId)
                .param("id", shiftId)
                .param("version", expectedVersion)
                .update();
        if (updated == 0) {
            throw new ConflictException("Someone else changed this shift at the same time. Reload and try again.");
        }
        Shift assigned = shift(shiftId);
        if (shift.offered() && accountId != null && !accountId.equals(shift.accountId())) {
            events.publishEvent(new ShiftCovered(caller.studioId(), shiftId, shift.title(), shift.startsAt(),
                    shift.accountId(), assigned.accountName()));
        }
        return assigned;
    }

    /**
     * Asks for cover, or stops asking. Only whoever has the shift (or staff) can; they stay on it
     * until someone takes it, so asking never leaves it uncovered. Everyone who could work it
     * hears about the offer.
     */
    @Transactional
    public Shift offer(UUID shiftId, boolean offered, long expectedVersion) {
        TenantContext caller = TenantContext.require();
        Shift shift = shift(shiftId);
        if (shift.accountId() == null) {
            throw new InvalidInputException("Nobody has this shift; it's already open for anyone to take.");
        }
        if (!isStaff(caller) && !caller.accountId().equals(shift.accountId())) {
            throw new AccessDeniedException("Only whoever has this shift can ask for cover.");
        }
        if (shift.workedAt() != null || !shift.endsAt().isAfter(Instant.now())) {
            throw new InvalidInputException("This shift is over.");
        }
        int updated = jdbc.sql("""
                update shift set offered_at = :offeredAt, version = version + 1 where id = :id and version = :version
                """)
                .param("offeredAt", offered ? utc(now()) : null)
                .param("id", shiftId)
                .param("version", expectedVersion)
                .update();
        if (updated == 0) {
            throw new ConflictException("Someone else changed this shift at the same time. Reload and try again.");
        }
        Shift result = shift(shiftId);
        if (offered && !shift.offered()) {
            events.publishEvent(new ShiftOffered(caller.studioId(), shiftId, shift.title(), shift.startsAt(),
                    shift.endsAt(), result.accountName(), couldCover(shift)));
        }
        return result;
    }

    /** Members who could take this shift: those with its duty, or everyone if it has none. */
    private List<UUID> couldCover(Shift shift) {
        if (shift.duty() == null) {
            return members.list().stream().map(m -> m.accountId()).filter(id -> !id.equals(shift.accountId())).toList();
        }
        return access.allDuties().entrySet().stream()
                .filter(e -> e.getValue().contains(shift.duty()) && !e.getKey().equals(shift.accountId()))
                .map(Map.Entry::getKey).sorted().toList();
    }

    /** Staff confirm a shift was worked, which credits its hours to the person who worked it. */
    @Transactional
    public Shift markWorked(UUID shiftId, long expectedVersion) {
        TenantContext caller = requireStaff();
        Shift shift = shift(shiftId);
        if (shift.accountId() == null) {
            throw new InvalidInputException("Nobody is assigned to this shift.");
        }
        if (shift.workedAt() != null) {
            throw new InvalidInputException("This shift is already marked as worked.");
        }
        Instant now = now();
        int updated = jdbc.sql("update shift set worked_at = :now, version = version + 1 where id = :id and version = :version")
                .param("now", utc(now))
                .param("id", shiftId)
                .param("version", expectedVersion)
                .update();
        if (updated == 0) {
            throw new ConflictException("Someone else changed this shift at the same time. Reload and try again.");
        }
        credit(caller, shift.accountId(), shift.minutes(), shift.title(), shiftId, now);
        return shift(shiftId);
    }

    /** A manual correction to someone's hours, positive or negative. */
    @Transactional
    public HoursLedger adjustHours(UUID accountId, int minutes, String note) {
        TenantContext caller = requireStaff();
        if (minutes == 0 || Math.abs(minutes) > 6000) {
            throw new InvalidInputException("Adjust by up to 100 hours either way.");
        }
        if (note == null || note.isBlank() || note.strip().length() > 200) {
            throw new InvalidInputException("Say why, in up to 200 characters.");
        }
        if (members.roleOf(accountId).isEmpty()) {
            throw new InvalidInputException("That person isn't a member of this studio.");
        }
        credit(caller, accountId, minutes, note.strip(), null, now());
        return hours(accountId);
    }

    /** Someone's credited hours. Members can see only their own. */
    @Transactional(readOnly = true)
    public HoursLedger hours(UUID accountId) {
        TenantContext caller = TenantContext.require();
        if (!accountId.equals(caller.accountId()) && !isStaff(caller)) {
            throw new AccessDeniedException("You can see only your own hours.");
        }
        List<UUID> creditors = new ArrayList<>();
        List<HoursEntry> raw = jdbc.sql("""
                select id, minutes, note, shift_id, credited_at, credited_by from hours_entry
                where account_id = :id order by credited_at desc limit 500
                """)
                .param("id", accountId)
                .query((rs, row) -> {
                    creditors.add(rs.getObject("credited_by", UUID.class));
                    return new HoursEntry(rs.getObject("id", UUID.class), rs.getInt("minutes"), rs.getString("note"),
                            rs.getObject("shift_id", UUID.class), instant(rs, "credited_at"), null);
                })
                .list();
        Set<UUID> ids = new HashSet<>(creditors);
        ids.add(accountId);
        Map<UUID, String> names = accounts.displayNames(ids);
        List<HoursEntry> entries = new ArrayList<>(raw.size());
        for (int i = 0; i < raw.size(); i++) {
            HoursEntry e = raw.get(i);
            entries.add(new HoursEntry(e.id(), e.minutes(), e.note(), e.shiftId(), e.creditedAt(), names.get(creditors.get(i))));
        }
        int total = jdbc.sql("select coalesce(sum(minutes), 0) from hours_entry where account_id = :id")
                .param("id", accountId).query(Integer.class).single();
        return new HoursLedger(accountId, names.get(accountId), total, entries);
    }

    // ---- Internals

    private static final String SELECT_SHIFT = """
            select id, title, duty, starts_at, ends_at, account_id, worked_at, offered_at, version from shift
            """;

    private Shift shift(UUID id) {
        return withNames(jdbc.sql(SELECT_SHIFT + "where id = :id").param("id", id)
                .query(ScheduleService::mapShift).list())
                .stream().findFirst()
                .orElseThrow(() -> new NotFoundException("No such shift."));
    }

    private static Resource mapResource(ResultSet rs, @Nullable String certificationName, Map<UUID, String> down)
            throws SQLException {
        UUID assetId = rs.getObject("asset_id", UUID.class);
        return new Resource(rs.getObject("id", UUID.class), rs.getString("name"),
                ResourceKind.valueOf(rs.getString("kind")), rs.getInt("capacity"),
                rs.getObject("required_certification", UUID.class), certificationName, assetId,
                assetId == null ? null : down.get(assetId), rs.getLong("version"));
    }

    private Resource resource(UUID id) {
        return resources().stream().filter(r -> r.id().equals(id)).findFirst()
                .orElseThrow(() -> new NotFoundException("No such resource."));
    }

    private void credit(TenantContext caller, UUID accountId, int minutes, String note, @Nullable UUID shiftId, Instant at) {
        jdbc.sql("""
                insert into hours_entry (id, studio_id, account_id, shift_id, minutes, note, credited_by, credited_at)
                values (:id, :studioId, :accountId, :shiftId, :minutes, :note, :by, :at)
                """)
                .param("id", Uuids.v7())
                .param("studioId", caller.studioId())
                .param("accountId", accountId)
                .param("shiftId", shiftId)
                .param("minutes", minutes)
                .param("note", note.length() > 200 ? note.substring(0, 200) : note)
                .param("by", caller.accountId())
                .param("at", utc(at))
                .update();
    }

    private List<Shift> withNames(List<Shift> shifts) {
        Map<UUID, String> names = accounts.displayNames(shifts.stream().map(Shift::accountId)
                .filter(java.util.Objects::nonNull).toList());
        return shifts.stream()
                .map(s -> new Shift(s.id(), s.title(), s.duty(), s.startsAt(), s.endsAt(), s.accountId(),
                        s.accountId() == null ? null : names.get(s.accountId()), s.workedAt(), s.offeredAt(),
                        s.version()))
                .toList();
    }

    private static Shift mapShift(ResultSet rs, int row) throws SQLException {
        String duty = rs.getString("duty");
        OffsetDateTime worked = rs.getObject("worked_at", OffsetDateTime.class);
        OffsetDateTime offered = rs.getObject("offered_at", OffsetDateTime.class);
        return new Shift(rs.getObject("id", UUID.class), rs.getString("title"), duty == null ? null : Duty.valueOf(duty),
                instant(rs, "starts_at"), instant(rs, "ends_at"), rs.getObject("account_id", UUID.class), null,
                worked == null ? null : worked.toInstant(), offered == null ? null : offered.toInstant(),
                rs.getLong("version"));
    }

    private static Booking mapBooking(ResultSet rs, int row) throws SQLException {
        return new Booking(rs.getObject("id", UUID.class), rs.getObject("resource_id", UUID.class),
                rs.getString("resource_name"), BookingKind.valueOf(rs.getString("kind")), rs.getString("title"),
                instant(rs, "starts_at"), instant(rs, "ends_at"), rs.getObject("account_id", UUID.class), null, false);
    }

    private static void requireRange(Instant from, Instant to) {
        if (!to.isAfter(from) || Duration.between(from, to).compareTo(LONGEST_RANGE) > 0) {
            throw new InvalidInputException("Ask for a range of up to 62 days.");
        }
    }

    private static String validName(String name) {
        String cleaned = name == null ? "" : name.strip();
        if (cleaned.isEmpty() || cleaned.length() > 60) {
            throw new InvalidInputException("Give it a name of 1 to 60 characters.");
        }
        return cleaned;
    }

    private static TenantContext requireStaff() {
        TenantContext caller = TenantContext.require();
        if (!isStaff(caller)) {
            throw new AccessDeniedException("Only staff can do this.");
        }
        return caller;
    }

    private static boolean isStaff(TenantContext caller) {
        return caller.role() != null && caller.role().includes(StudioRole.STAFF);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, OffsetDateTime.class).toInstant();
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

}
