package io.github.aaronfilson.studiomanager.notify;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import io.github.aaronfilson.studiomanager.kiln.FiringCompleted;
import io.github.aaronfilson.studiomanager.kiln.FiringKind;
import io.github.aaronfilson.studiomanager.maintenance.DueStatus;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceDue;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.MemberView;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;
import io.github.aaronfilson.studiomanager.studio.StudioService;
import io.github.aaronfilson.studiomanager.studio.StudioView;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Members' inbox in each studio. Each notice is also emailed ({@link NotificationEmails}), through
 * the transactional outbox, so nothing is sent for work that rolled back.
 *
 * <p>The research behind this: the most specific complaint about an existing tracker was "ready"
 * emails going out before all of a member's pieces were out. So a firing produces exactly one
 * notice per maker, listing every piece of theirs it finished.
 */
@Service
public class NotificationService {

    private final JdbcClient jdbc;
    private final StudioService studios;
    private final StudioMembers members;
    private final ApplicationEventPublisher events;

    NotificationService(JdbcClient jdbc, StudioService studios, StudioMembers members,
            ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.studios = studios;
        this.members = members;
        this.events = events;
    }

    /** The caller's notifications in the bound studio, newest first. */
    @Transactional(readOnly = true)
    public List<NotificationView> mine() {
        return jdbc.sql("""
                select id, kind, title, body, created_at, read_at from notification
                where account_id = :me order by created_at desc limit 100
                """)
                .param("me", TenantContext.require().accountId())
                .query((rs, row) -> new NotificationView(rs.getObject("id", UUID.class),
                        NotificationView.Kind.valueOf(rs.getString("kind")), rs.getString("title"),
                        rs.getString("body"), rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                        rs.getObject("read_at", OffsetDateTime.class) == null ? null
                                : rs.getObject("read_at", OffsetDateTime.class).toInstant()))
                .list();
    }

    @Transactional
    public void markRead(UUID notificationId) {
        int updated = jdbc.sql("""
                update notification set read_at = coalesce(read_at, :now) where id = :id and account_id = :me
                """)
                .param("now", utc(now()))
                .param("id", notificationId)
                .param("me", TenantContext.require().accountId())
                .update();
        if (updated == 0) {
            throw new NotFoundException("No such notification.");
        }
    }

    /** A notice from staff to every member of the studio, such as a closure. Returns how many were sent. */
    @Transactional
    public int broadcast(String title, String body) {
        TenantContext caller = TenantContext.require();
        if (caller.role() == null || !caller.role().includes(StudioRole.STAFF)) {
            throw new AccessDeniedException("Only staff can send studio notices.");
        }
        String cleanTitle = title.strip();
        String cleanBody = body.strip();
        if (cleanTitle.isEmpty() || cleanTitle.length() > 120 || cleanBody.length() > 2000) {
            throw new InvalidInputException("A notice needs a title of 1 to 120 characters and at most 2,000 characters of text.");
        }
        List<UUID> everyone = members.list().stream().map(MemberView::accountId).toList();
        insert(everyone, NotificationView.Kind.STUDIO_NOTICE, cleanTitle, cleanBody);
        return everyone.size();
    }

    /** One notice per maker for the whole firing, listing each of their pieces it finished. */
    @EventListener
    void onFiringCompleted(FiringCompleted firing) {
        var zone = studios.current().timeZone();
        // "4 Nov 2026": unambiguous in every country the product serves
        var dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH).withZone(zone);
        Map<UUID, List<FiringCompleted.FiredPiece>> byMaker = firing.pieces().stream()
                .collect(Collectors.groupingBy(FiringCompleted.FiredPiece::makerAccountId));
        byMaker.forEach((maker, theirs) -> {
            List<FiringCompleted.FiredPiece> sorted = theirs.stream()
                    .sorted(Comparator.comparingInt(FiringCompleted.FiredPiece::number)).toList();
            int count = sorted.size();
            String noun = count == 1 ? "piece is" : count + " pieces are";
            String title = firing.kind() == FiringKind.GLAZE
                    ? "Your %s ready to collect".formatted(noun)
                    : "Your %s bisque fired and ready to glaze".formatted(noun);
            StringBuilder body = new StringBuilder();
            for (FiringCompleted.FiredPiece piece : sorted) {
                body.append("Piece ").append(piece.number());
                if (piece.description() != null) {
                    body.append(": ").append(piece.description());
                }
                body.append('\n');
            }
            sorted.stream().map(FiringCompleted.FiredPiece::holdUntil).filter(Objects::nonNull).findFirst()
                    .ifPresent(hold -> body.append("Please collect by ").append(dateFormat.format(hold.minusSeconds(1)))
                            .append('.'));
            insert(List.of(maker), NotificationView.Kind.PIECES_READY, title, body.toString().strip());
        });
    }

    /** One notice to each person the task falls to, with what to do and by when. */
    @EventListener
    void onMaintenanceDue(MaintenanceDue due) {
        var dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH).withZone(studios.current().timeZone());
        String title = (due.status() == DueStatus.DUE ? "Due: " : "Due soon: ") + due.taskTitle();
        StringBuilder body = new StringBuilder(due.assetName()).append(": ").append(due.taskTitle()).append('\n');
        if (due.dueAt() != null) {
            body.append(due.status() == DueStatus.DUE && !due.dueAt().isAfter(Instant.now()) ? "It was due on " : "Due by ")
                    .append(dateFormat.format(due.dueAt())).append(".\n");
        }
        if (due.firingsLeft() != null) {
            int left = due.firingsLeft();
            body.append(left <= 0 ? "It's due by firing count." : left == 1 ? "1 firing left." : left + " firings left.")
                    .append('\n');
        }
        if (!due.checklist().isEmpty()) {
            body.append('\n');
            due.checklist().forEach(line -> body.append("- ").append(line).append('\n'));
        }
        body.append("\nMark it done on the Maintenance page when it's finished.");
        insert(due.recipients(), NotificationView.Kind.MAINTENANCE_DUE, clip(title, 120), clip(body.toString(), 2000));
    }

    private static String clip(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    private void insert(Collection<UUID> accountIds, NotificationView.Kind kind, String title, String body) {
        StudioView studio = studios.current();
        Instant now = now();
        for (UUID accountId : accountIds) {
            UUID id = Uuids.v7();
            jdbc.sql("""
                    insert into notification (id, studio_id, account_id, kind, title, body, created_at)
                    values (:id, :studioId, :accountId, :kind, :title, :body, :now)
                    """)
                    .param("id", id)
                    .param("studioId", studio.id())
                    .param("accountId", accountId)
                    .param("kind", kind.name())
                    .param("title", title)
                    .param("body", body)
                    .param("now", utc(now))
                    .update();
            // Recorded in the outbox in this transaction, emailed after it commits
            events.publishEvent(new NotificationCreated(id, studio.id(), studio.name(), accountId, kind, title, body));
        }
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

}
