package io.github.aaronfilson.studiomanager.kiln;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.aaronfilson.studiomanager.piece.PieceService;
import io.github.aaronfilson.studiomanager.piece.PieceService.WaitingPiece;
import io.github.aaronfilson.studiomanager.piece.PieceView;
import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kiln loads, from building one out of the queue to unloading it.
 *
 * <p>Unloading is one transaction: the pieces move on, the kiln's counters go up, and
 * {@link FiringCompleted} is published so the notify module writes each maker one notice for
 * the whole batch. If any part fails, none of it happens.
 */
@Service
public class FiringService {

    private static final String SELECT_FIRING = """
            select f.id, f.kiln_id, k.name as kiln_name, f.kind, f.cone, f.status, f.notes, f.created_at,
                   f.started_at, f.finished_at, f.version,
                   (select count(*) from firing_piece fp where fp.firing_id = f.id) as piece_count
            from firing f join kiln k on k.id = f.kiln_id
            """;

    private final JdbcClient jdbc;
    private final KilnService kilns;
    private final PieceService pieces;
    private final ApplicationEventPublisher events;

    FiringService(JdbcClient jdbc, KilnService kilns, PieceService pieces, ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.kilns = kilns;
        this.pieces = pieces;
        this.events = events;
    }

    /**
     * The queue for {@code kind} firings in {@code kilnId}: every waiting piece not already in a
     * load, longest-waiting first, marked by whether the kiln's per-member cap lets it into the next load.
     */
    @Transactional(readOnly = true)
    public List<KilnQueue.Entry<WaitingPiece>> queue(UUID kilnId, FiringKind kind) {
        KilnView kiln = kilns.get(kilnId);
        Set<UUID> loaded = new HashSet<>(jdbc.sql("select piece_id from firing_piece where open")
                .query(UUID.class).list());
        List<WaitingPiece> waiting = pieces.waitingIn(kind.takes()).stream()
                .filter(w -> !loaded.contains(w.piece().id()))
                .toList();
        return KilnQueue.plan(waiting, w -> w.piece().makerAccountId(), kiln.maxPiecesPerMember());
    }

    /** Starts a load with the given pieces. They must be waiting for this kind of firing and within the cap. */
    @Transactional
    public FiringView load(UUID kilnId, FiringKind kind, @Nullable String cone, List<UUID> pieceIds) {
        TenantContext caller = TenantContext.require();
        KilnView kiln = kilns.get(kilnId);
        if (!kiln.available()) {
            throw new InvalidInputException("%s is out of service: %s".formatted(kiln.name(), kiln.downReason()));
        }
        Set<UUID> chosen = new LinkedHashSet<>(pieceIds);
        if (chosen.isEmpty()) {
            throw new InvalidInputException("Choose at least one piece to load.");
        }
        Map<UUID, KilnQueue.Entry<WaitingPiece>> queued = queue(kilnId, kind).stream()
                .collect(Collectors.toMap(e -> e.piece().piece().id(), Function.identity()));
        for (UUID id : chosen) {
            if (!queued.containsKey(id)) {
                throw new InvalidInputException("A piece you chose isn't waiting for a %s firing, or is already loaded."
                        .formatted(kind.name().toLowerCase()));
            }
        }
        if (kiln.maxPiecesPerMember() != null) {
            Map<UUID, Long> perMaker = chosen.stream()
                    .collect(Collectors.groupingBy(id -> queued.get(id).piece().piece().makerAccountId(),
                            Collectors.counting()));
            if (perMaker.values().stream().anyMatch(count -> count > kiln.maxPiecesPerMember())) {
                throw new InvalidInputException("This kiln takes at most %d pieces per member in one load."
                        .formatted(kiln.maxPiecesPerMember()));
            }
        }
        UUID firingId = Uuids.v7();
        try {
            jdbc.sql("""
                    insert into firing (id, studio_id, kiln_id, kind, cone, status, created_by, created_at, version)
                    values (:id, :studioId, :kilnId, :kind, :cone, 'LOADING', :by, :now, 0)
                    """)
                    .param("id", firingId)
                    .param("studioId", caller.studioId())
                    .param("kilnId", kilnId)
                    .param("kind", kind.name())
                    .param("cone", cone == null || cone.isBlank() ? null : cone.strip())
                    .param("by", caller.accountId())
                    .param("now", KilnService.utc(KilnService.now()))
                    .update();
            for (UUID pieceId : chosen) {
                jdbc.sql("""
                        insert into firing_piece (firing_id, piece_id, studio_id, open)
                        values (:firingId, :pieceId, :studioId, true)
                        """)
                        .param("firingId", firingId)
                        .param("pieceId", pieceId)
                        .param("studioId", caller.studioId())
                        .update();
            }
        } catch (DuplicateKeyException e) {
            throw new ConflictException("That kiln already has a load in progress, or a piece was just loaded elsewhere.");
        }
        return get(firingId);
    }

    /** Marks a load as firing. */
    @Transactional
    public FiringView start(UUID firingId, long expectedVersion) {
        FiringView firing = get(firingId);
        if (firing.status() != FiringView.Status.LOADING) {
            throw new InvalidInputException("Only a load that's still loading can be started.");
        }
        setStatus(firingId, FiringView.Status.FIRING, "started_at", expectedVersion, null);
        FiringView started = get(firingId);
        events.publishEvent(new FiringStarted(TenantContext.require().studioId(), firingId, started.kilnId(),
                started.kilnName(), started.kind(), started.startedAt()));
        return started;
    }

    /** Unloads a firing: moves its pieces on, counts the firing, and notifies makers once per batch. */
    @Transactional
    public FiringView complete(UUID firingId, @Nullable String notes, long expectedVersion) {
        FiringView firing = get(firingId);
        if (firing.status() != FiringView.Status.FIRING) {
            throw new InvalidInputException("Only a firing in progress can be unloaded.");
        }
        setStatus(firingId, FiringView.Status.COMPLETE, "finished_at", expectedVersion, notes);
        List<UUID> pieceIds = pieceIdsOf(firingId);
        jdbc.sql("update firing_piece set open = false where firing_id = :id").param("id", firingId).update();
        List<PieceView> moved = pieces.advanceAll(pieceIds, firing.kind().takes(), firing.kind().leaves(),
                "%s firing in %s".formatted(firing.kind() == FiringKind.BISQUE ? "Bisque" : "Glaze", firing.kilnName()));
        jdbc.sql("""
                update kiln set total_firings = total_firings + 1, version = version + 1
                where id = :id
                """)
                .param("id", firing.kilnId())
                .update();
        events.publishEvent(new FiringCompleted(TenantContext.require().studioId(), firingId, firing.kind(),
                firing.kilnName(), moved.stream()
                        .map(p -> new FiringCompleted.FiredPiece(p.id(), p.number(), p.makerAccountId(),
                                p.description(), p.holdUntil()))
                        .toList()));
        FiringView completed = get(firingId);
        events.publishEvent(new FiringEnded(TenantContext.require().studioId(), firingId, firing.kilnId(),
                completed.finishedAt(), true));
        return completed;
    }

    /** Abandons a load before or during firing; its pieces go back to the queue unchanged. */
    @Transactional
    public FiringView cancel(UUID firingId, @Nullable String notes, long expectedVersion) {
        FiringView firing = get(firingId);
        if (!firing.status().open()) {
            throw new InvalidInputException("This firing is already finished.");
        }
        setStatus(firingId, FiringView.Status.CANCELLED, "finished_at", expectedVersion, notes);
        jdbc.sql("update firing_piece set open = false where firing_id = :id").param("id", firingId).update();
        FiringView cancelled = get(firingId);
        events.publishEvent(new FiringEnded(TenantContext.require().studioId(), firingId, firing.kilnId(),
                cancelled.finishedAt(), false));
        return cancelled;
    }

    /** Every load in progress (loading or firing), across the studio's kilns. */
    @Transactional(readOnly = true)
    public List<FiringView> open() {
        return jdbc.sql(SELECT_FIRING + "where f.status in ('LOADING', 'FIRING') order by k.name")
                .query(FiringService::mapFiring).list();
    }

    @Transactional(readOnly = true)
    public List<FiringView> listForKiln(UUID kilnId) {
        kilns.get(kilnId);
        return jdbc.sql(SELECT_FIRING + "where f.kiln_id = :kilnId order by f.created_at desc limit 100")
                .param("kilnId", kilnId)
                .query(FiringService::mapFiring).list();
    }

    @Transactional(readOnly = true)
    public FiringView get(UUID firingId) {
        FiringView firing = jdbc.sql(SELECT_FIRING + "where f.id = :id")
                .param("id", firingId)
                .query(FiringService::mapFiring).optional()
                .orElseThrow(() -> new NotFoundException("No such firing."));
        List<UUID> ids = pieceIdsOf(firingId);
        List<PieceView> loaded = ids.stream().map(pieces::get).toList();
        return new FiringView(firing.id(), firing.kilnId(), firing.kilnName(), firing.kind(), firing.cone(),
                firing.status(), firing.notes(), firing.createdAt(), firing.startedAt(), firing.finishedAt(),
                firing.pieceCount(), firing.version(), loaded);
    }

    private List<UUID> pieceIdsOf(UUID firingId) {
        return jdbc.sql("select piece_id from firing_piece where firing_id = :id order by piece_id")
                .param("id", firingId)
                .query(UUID.class).list();
    }

    private void setStatus(UUID firingId, FiringView.Status status, String timestampColumn, long expectedVersion,
            @Nullable String notes) {
        String cleanedNotes = notes == null || notes.isBlank() ? null : notes.strip();
        if (cleanedNotes != null && cleanedNotes.length() > 1000) {
            throw new InvalidInputException("Keep notes under 1,000 characters.");
        }
        // timestampColumn is one of two constants chosen above, never user input
        int updated = jdbc.sql("""
                update firing set status = :status, %s = :now, notes = coalesce(:notes, notes), version = version + 1
                where id = :id and version = :version
                """.formatted(timestampColumn))
                .param("status", status.name())
                .param("now", KilnService.utc(KilnService.now()))
                .param("notes", cleanedNotes)
                .param("id", firingId)
                .param("version", expectedVersion)
                .update();
        if (updated == 0) {
            throw new ConflictException("Someone else changed this firing at the same time. Reload and try again.");
        }
    }

    private static FiringView mapFiring(ResultSet rs, int row) throws SQLException {
        return new FiringView(rs.getObject("id", UUID.class), rs.getObject("kiln_id", UUID.class),
                rs.getString("kiln_name"), FiringKind.valueOf(rs.getString("kind")), rs.getString("cone"),
                FiringView.Status.valueOf(rs.getString("status")), rs.getString("notes"),
                instant(rs, "created_at"), instantOrNull(rs, "started_at"), instantOrNull(rs, "finished_at"),
                rs.getInt("piece_count"), rs.getLong("version"), List.of());
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, OffsetDateTime.class).toInstant();
    }

    private static @Nullable Instant instantOrNull(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

}
