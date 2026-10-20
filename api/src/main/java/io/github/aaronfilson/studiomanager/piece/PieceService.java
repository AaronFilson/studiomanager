package io.github.aaronfilson.studiomanager.piece;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.firing.FeeQuote;
import io.github.aaronfilson.studiomanager.firing.FeeService;
import io.github.aaronfilson.studiomanager.firing.PieceSize;
import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.Money;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.measure.Length;
import io.github.aaronfilson.studiomanager.platform.measure.LengthUnit;
import io.github.aaronfilson.studiomanager.platform.measure.Weight;
import io.github.aaronfilson.studiomanager.platform.measure.WeightUnit;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;
import io.github.aaronfilson.studiomanager.studio.StudioService;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Members' pieces in the bound studio. Staff see and move every piece; members see only their
 * own and can drop off only their own.
 */
@Service
public class PieceService {

    static final int DEFAULT_HOLD_DAYS = 30;

    private static final String SELECT_PIECE = """
            select id, number, maker_account_id, description, clay_body, maker_mark,
                   length_value, width_value, height_value, length_unit, weight_value, weight_unit,
                   stage, fee_amount, fee_currency, fee_schedule_version, fee_tax_category, fee_breakdown,
                   received_at, ready_at, hold_until, version
            from piece
            """;

    private final JdbcClient jdbc;
    private final FeeService fees;
    private final StudioService studios;
    private final StudioMembers members;
    private final AccountDirectory accounts;
    private final ApplicationEventPublisher events;

    PieceService(JdbcClient jdbc, FeeService fees, StudioService studios, StudioMembers members,
            AccountDirectory accounts, ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.fees = fees;
        this.studios = studios;
        this.members = members;
        this.accounts = accounts;
        this.events = events;
    }

    /** Takes in a piece: numbers it, prices it with the current fee schedule, and starts its timeline. */
    @Transactional
    public PieceView intake(PieceIntake intake) {
        TenantContext caller = TenantContext.require();
        UUID maker = intake.makerAccountId() == null ? caller.accountId() : intake.makerAccountId();
        if (!maker.equals(caller.accountId())) {
            requireStaff(caller, "Only staff can take in a piece for someone else.");
            if (members.roleOf(maker).isEmpty()) {
                throw new InvalidInputException("The maker isn't a member of this studio.");
            }
        }
        FeeQuote fee = fees.quote(intake.size());
        int number = jdbc.sql("""
                insert into piece_counter (studio_id, last_number) values (:studioId, 1)
                on conflict (studio_id) do update set last_number = piece_counter.last_number + 1
                returning last_number
                """)
                .param("studioId", caller.studioId())
                .query(Integer.class).single();
        UUID id = Uuids.v7();
        Instant now = now();
        PieceSize size = intake.size();
        jdbc.sql("""
                insert into piece (id, studio_id, number, maker_account_id, description, clay_body, maker_mark,
                                   length_value, width_value, height_value, length_unit, weight_value, weight_unit,
                                   stage, fee_amount, fee_currency, fee_schedule_version, fee_tax_category,
                                   fee_breakdown, received_at, created_by, version)
                values (:id, :studioId, :number, :maker, :description, :clayBody, :makerMark,
                        :length, :width, :height, :lengthUnit, :weight, :weightUnit,
                        'RECEIVED', :feeAmount, :feeCurrency, :feeVersion, :feeTaxCategory,
                        :feeBreakdown, :receivedAt, :createdBy, 0)
                """)
                .param("id", id)
                .param("studioId", caller.studioId())
                .param("number", number)
                .param("maker", maker)
                .param("description", clean(intake.description(), 200, "description"))
                .param("clayBody", clean(intake.clayBody(), 80, "clay body"))
                .param("makerMark", clean(intake.makerMark(), 80, "maker's mark"))
                .param("length", size.hasDimensions() ? size.length().value() : null)
                .param("width", size.hasDimensions() ? size.width().value() : null)
                .param("height", size.hasDimensions() ? size.height().value() : null)
                .param("lengthUnit", size.hasDimensions() ? commonLengthUnit(size).name() : null)
                .param("weight", size.weight() == null ? null : size.weight().value())
                .param("weightUnit", size.weight() == null ? null : size.weight().unit().name())
                .param("feeAmount", fee.amount().amount())
                .param("feeCurrency", fee.amount().currency().getCurrencyCode())
                .param("feeVersion", fee.scheduleVersion() == 0 ? null : fee.scheduleVersion())
                .param("feeTaxCategory", fee.taxCategory())
                .param("feeBreakdown", fee.breakdown())
                .param("receivedAt", utc(now))
                .param("createdBy", caller.accountId())
                .update();
        recordEvent(id, null, PieceStage.RECEIVED, null, caller.accountId(), now);
        events.publishEvent(new PieceReceived(id, number, maker, fee.amount(), fee.taxCategory(), now));
        return get(id);
    }

    /** Pieces, newest first, optionally filtered. Members only ever see their own. */
    @Transactional(readOnly = true)
    public List<PieceView> list(@Nullable PieceStage stage, @Nullable UUID maker) {
        TenantContext caller = TenantContext.require();
        UUID makerFilter = isStaff(caller) ? maker : caller.accountId();
        List<PieceView> pieces = jdbc.sql(SELECT_PIECE + """
                where (cast(:stage as text) is null or stage = :stage)
                  and (cast(:maker as uuid) is null or maker_account_id = :maker)
                order by number desc
                limit 500
                """)
                .param("stage", stage == null ? null : stage.name())
                .param("maker", makerFilter)
                .query(PieceService::mapPiece).list();
        return withNames(pieces, List.of());
    }

    /** One piece and its timeline. A member asking for someone else's piece gets a 404. */
    @Transactional(readOnly = true)
    public PieceView get(UUID pieceId) {
        TenantContext caller = TenantContext.require();
        PieceView piece = jdbc.sql(SELECT_PIECE + "where id = :id")
                .param("id", pieceId)
                .query(PieceService::mapPiece).optional()
                .filter(found -> isStaff(caller) || found.makerAccountId().equals(caller.accountId()))
                .orElseThrow(() -> new NotFoundException("No such piece."));
        List<PieceView.Event> timeline = jdbc.sql("""
                select from_stage, to_stage, note, actor_account_id, occurred_at
                from piece_event where piece_id = :id order by occurred_at, id
                """)
                .param("id", pieceId)
                .query((rs, row) -> new PieceView.Event(stageOrNull(rs.getString("from_stage")),
                        PieceStage.valueOf(rs.getString("to_stage")), rs.getString("note"),
                        rs.getObject("actor_account_id", UUID.class), null,
                        instant(rs, "occurred_at")))
                .list();
        return withNames(List.of(piece), timeline).getFirst();
    }

    /**
     * Moves a piece to {@code target}, if that move is allowed from where it is now. Reaching READY
     * starts the hold: the piece is held through the end of the studio's local day, hold-days later.
     *
     * @param expectedVersion the version the caller last saw, so two people can't overwrite each other
     */
    @Transactional
    public PieceView move(UUID pieceId, PieceStage target, @Nullable String note, long expectedVersion) {
        TenantContext caller = TenantContext.require();
        requireStaff(caller, "Only staff can move pieces.");
        PieceView piece = get(pieceId);
        if (!piece.stage().canMoveTo(target)) {
            throw new InvalidInputException("A piece can't go from %s to %s.".formatted(piece.stage(), target));
        }
        Instant now = now();
        Instant readyAt = piece.readyAt();
        Instant holdUntil = piece.holdUntil();
        if (target == PieceStage.READY) {
            readyAt = now;
            holdUntil = holdEnd(now, holdDays(), studios.current().timeZone());
        }
        int updated = jdbc.sql("""
                update piece set stage = :stage, ready_at = :readyAt, hold_until = :holdUntil, version = version + 1
                where id = :id and version = :version
                """)
                .param("stage", target.name())
                .param("readyAt", readyAt == null ? null : utc(readyAt))
                .param("holdUntil", holdUntil == null ? null : utc(holdUntil))
                .param("id", pieceId)
                .param("version", expectedVersion)
                .update();
        if (updated == 0) {
            throw new ConflictException("Someone else changed this piece at the same time. Reload and try again.");
        }
        recordEvent(pieceId, piece.stage(), target, clean(note, 500, "note"), caller.accountId(), now);
        return get(pieceId);
    }

    /**
     * Pieces in {@code stage}, longest-waiting first, with when each reached that stage. This is
     * what a kiln queue is built from. Staff only.
     */
    @Transactional(readOnly = true)
    public List<WaitingPiece> waitingIn(PieceStage stage) {
        requireStaff(TenantContext.require(), "Only staff can see the kiln queue.");
        List<WaitingPiece> waiting = jdbc.sql("""
                select p.*, e.since
                from (%s) p
                join lateral (select max(occurred_at) as since from piece_event
                              where piece_id = p.id and to_stage = p.stage) e on true
                where p.stage = :stage
                order by e.since, p.number
                limit 1000
                """.formatted(SELECT_PIECE))
                .param("stage", stage.name())
                .query((rs, row) -> new WaitingPiece(mapPiece(rs, row), instant(rs, "since")))
                .list();
        List<PieceView> named = withNames(waiting.stream().map(WaitingPiece::piece).toList(), List.of());
        List<WaitingPiece> result = new ArrayList<>(waiting.size());
        for (int i = 0; i < waiting.size(); i++) {
            result.add(new WaitingPiece(named.get(i), waiting.get(i).since()));
        }
        return result;
    }

    /**
     * Moves every piece in {@code pieceIds} from {@code from} to {@code to} after a firing, with
     * one timeline entry each. All or nothing: if any piece is no longer in {@code from}, nothing
     * moves. Returns the pieces as they are afterwards.
     */
    @Transactional
    public List<PieceView> advanceAll(Collection<UUID> pieceIds, PieceStage from, PieceStage to, String note) {
        TenantContext caller = TenantContext.require();
        requireStaff(caller, "Only staff can move pieces.");
        if (!from.canMoveTo(to)) {
            throw new IllegalArgumentException("Pieces can't go from %s to %s".formatted(from, to));
        }
        Instant now = now();
        Instant holdUntil = to == PieceStage.READY ? holdEnd(now, holdDays(), studios.current().timeZone()) : null;
        int updated = jdbc.sql("""
                update piece
                set stage = :to,
                    ready_at = case when :to = 'READY' then :now else ready_at end,
                    hold_until = case when :to = 'READY' then :holdUntil else hold_until end,
                    version = version + 1
                where id = any(:ids) and stage = :from
                """)
                .param("to", to.name())
                .param("from", from.name())
                .param("now", utc(now))
                .param("holdUntil", holdUntil == null ? null : utc(holdUntil))
                .param("ids", pieceIds.toArray(UUID[]::new))
                .update();
        if (updated != pieceIds.size()) {
            throw new ConflictException("Some of these pieces were moved by someone else. Reload and try again.");
        }
        for (UUID id : pieceIds) {
            recordEvent(id, from, to, note, caller.accountId(), now);
        }
        List<PieceView> pieces = jdbc.sql(SELECT_PIECE + "where id = any(:ids) order by number")
                .param("ids", pieceIds.toArray(UUID[]::new))
                .query(PieceService::mapPiece).list();
        return withNames(pieces, List.of());
    }

    /** How many pieces are in each stage, and how many ready pieces are past their hold. */
    @Transactional(readOnly = true)
    public StageCounts counts() {
        Map<PieceStage, Integer> byStage = new java.util.EnumMap<>(PieceStage.class);
        jdbc.sql("select stage, count(*) as n from piece group by stage")
                .query((rs, row) -> Map.entry(PieceStage.valueOf(rs.getString("stage")), rs.getInt("n")))
                .list()
                .forEach(e -> byStage.put(e.getKey(), e.getValue()));
        int overdue = jdbc.sql("select count(*) from piece where stage = 'READY' and hold_until <= :now")
                .param("now", utc(now()))
                .query(Integer.class).single();
        return new StageCounts(byStage, overdue);
    }

    public record StageCounts(Map<PieceStage, Integer> byStage, int overdue) {

        public int in(PieceStage stage) {
            return byStage.getOrDefault(stage, 0);
        }
    }

    /** A piece waiting in a stage, and since when. */
    public record WaitingPiece(PieceView piece, Instant since) {
    }

    @Transactional(readOnly = true)
    public int holdDays() {
        return jdbc.sql("select hold_days from piece_policy")
                .query(Integer.class).optional()
                .orElse(DEFAULT_HOLD_DAYS);
    }

    @Transactional
    public int setHoldDays(int days) {
        TenantContext caller = TenantContext.require();
        requireStaff(caller, "Only staff can change how long pieces are held.");
        if (days < 1 || days > 365) {
            throw new InvalidInputException("Hold pieces for 1 to 365 days.");
        }
        jdbc.sql("""
                insert into piece_policy (studio_id, hold_days, updated_at) values (:studioId, :days, :now)
                on conflict (studio_id) do update set hold_days = excluded.hold_days, updated_at = excluded.updated_at
                """)
                .param("studioId", caller.studioId())
                .param("days", days)
                .param("now", utc(now()))
                .update();
        return days;
    }

    /** The end of the studio's local day, {@code days} days after {@code readyAt}. */
    static Instant holdEnd(Instant readyAt, int days, ZoneId zone) {
        LocalDate lastDay = readyAt.atZone(zone).toLocalDate().plusDays(days);
        return lastDay.plusDays(1).atStartOfDay(zone).toInstant();
    }

    private void recordEvent(UUID pieceId, @Nullable PieceStage from, PieceStage to, @Nullable String note,
            UUID actor, Instant at) {
        jdbc.sql("""
                insert into piece_event (id, studio_id, piece_id, from_stage, to_stage, note, actor_account_id, occurred_at)
                values (:id, :studioId, :pieceId, :from, :to, :note, :actor, :at)
                """)
                .param("id", Uuids.v7())
                .param("studioId", TenantContext.require().studioId())
                .param("pieceId", pieceId)
                .param("from", from == null ? null : from.name())
                .param("to", to.name())
                .param("note", note)
                .param("actor", actor)
                .param("at", utc(at))
                .update();
    }

    private List<PieceView> withNames(List<PieceView> pieces, List<PieceView.Event> timeline) {
        Set<UUID> ids = new HashSet<>();
        pieces.forEach(piece -> ids.add(piece.makerAccountId()));
        timeline.forEach(event -> {
            if (event.actorAccountId() != null) {
                ids.add(event.actorAccountId());
            }
        });
        Map<UUID, String> names = accounts.displayNames(ids);
        List<PieceView.Event> namedTimeline = timeline.stream()
                .map(e -> new PieceView.Event(e.from(), e.to(), e.note(), e.actorAccountId(),
                        e.actorAccountId() == null ? null : names.get(e.actorAccountId()), e.occurredAt()))
                .toList();
        List<PieceView> named = new ArrayList<>(pieces.size());
        for (PieceView p : pieces) {
            named.add(new PieceView(p.id(), p.number(), p.makerAccountId(), names.get(p.makerAccountId()),
                    p.description(), p.clayBody(), p.makerMark(), p.size(), p.stage(), p.fee(),
                    p.feeScheduleVersion(), p.feeTaxCategory(), p.feeBreakdown(), p.receivedAt(), p.readyAt(),
                    p.holdUntil(), p.version(), namedTimeline));
        }
        return named;
    }

    private static PieceView mapPiece(ResultSet rs, int row) throws SQLException {
        String lengthUnit = rs.getString("length_unit");
        PieceSize size = new PieceSize(
                length(rs.getBigDecimal("length_value"), lengthUnit),
                length(rs.getBigDecimal("width_value"), lengthUnit),
                length(rs.getBigDecimal("height_value"), lengthUnit),
                weight(rs.getBigDecimal("weight_value"), rs.getString("weight_unit")));
        Integer scheduleVersion = rs.getObject("fee_schedule_version", Integer.class);
        return new PieceView(rs.getObject("id", UUID.class), rs.getInt("number"),
                rs.getObject("maker_account_id", UUID.class), null, rs.getString("description"),
                rs.getString("clay_body"), rs.getString("maker_mark"), size, PieceStage.valueOf(rs.getString("stage")),
                new Money(rs.getBigDecimal("fee_amount"), Currency.getInstance(rs.getString("fee_currency"))),
                scheduleVersion, rs.getString("fee_tax_category"),
                rs.getString("fee_breakdown"), instant(rs, "received_at"), instantOrNull(rs, "ready_at"),
                instantOrNull(rs, "hold_until"), rs.getLong("version"), List.of());
    }

    private static @Nullable Length length(@Nullable BigDecimal value, @Nullable String unit) {
        return value == null || unit == null ? null : new Length(value, LengthUnit.valueOf(unit));
    }

    private static @Nullable Weight weight(@Nullable BigDecimal value, @Nullable String unit) {
        return value == null || unit == null ? null : new Weight(value, WeightUnit.valueOf(unit));
    }

    private static LengthUnit commonLengthUnit(PieceSize size) {
        LengthUnit unit = size.length().unit();
        if (size.width().unit() != unit || size.height().unit() != unit) {
            throw new InvalidInputException("Give all three dimensions in the same unit.");
        }
        return unit;
    }

    private static @Nullable String clean(@Nullable String text, int maxLength, String field) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String stripped = text.strip();
        if (stripped.length() > maxLength) {
            throw new InvalidInputException("The %s can be at most %d characters.".formatted(field, maxLength));
        }
        return stripped;
    }

    private static boolean isStaff(TenantContext caller) {
        return caller.role() != null && caller.role().includes(StudioRole.STAFF);
    }

    private static void requireStaff(TenantContext caller, String message) {
        if (!isStaff(caller)) {
            throw new AccessDeniedException(message);
        }
    }

    private static @Nullable PieceStage stageOrNull(@Nullable String stage) {
        return stage == null ? null : PieceStage.valueOf(stage);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, OffsetDateTime.class).toInstant();
    }

    private static @Nullable Instant instantOrNull(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

}
