package io.github.aaronfilson.studiomanager.kiln;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

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

/** The bound studio's kilns and their availability. Their upkeep is in the maintenance module. */
@Service
public class KilnService {

    static final String SELECT_KILN = """
            select id, name, site, kind, max_pieces_per_member, total_firings, down_reason, version
            from kiln
            """;

    private final JdbcClient jdbc;
    private final ApplicationEventPublisher events;

    KilnService(JdbcClient jdbc, ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<KilnView> list() {
        return jdbc.sql(SELECT_KILN + "order by site nulls first, name").query(KilnService::mapKiln).list();
    }

    @Transactional(readOnly = true)
    public KilnView get(UUID kilnId) {
        return jdbc.sql(SELECT_KILN + "where id = :id").param("id", kilnId)
                .query(KilnService::mapKiln).optional()
                .orElseThrow(() -> new NotFoundException("No such kiln."));
    }

    @Transactional
    public KilnView create(KilnSettings settings) {
        TenantContext caller = TenantContext.require();
        UUID id = Uuids.v7();
        try {
            jdbc.sql("""
                    insert into kiln (id, studio_id, name, site, kind, max_pieces_per_member, created_at, version)
                    values (:id, :studioId, :name, :site, :kind, :cap, :now, 0)
                    """)
                    .param("id", id)
                    .param("studioId", caller.studioId())
                    .param("name", settings.name())
                    .param("site", settings.site())
                    .param("kind", settings.kind().name())
                    .param("cap", settings.maxPiecesPerMember())
                    .param("now", utc(now()))
                    .update();
        } catch (DuplicateKeyException e) {
            throw new InvalidInputException("There's already a kiln called '%s'.".formatted(settings.name()));
        }
        events.publishEvent(new KilnCreated(id, caller.studioId()));
        return get(id);
    }

    @Transactional
    public KilnView update(UUID kilnId, KilnSettings settings, long expectedVersion) {
        try {
            int updated = jdbc.sql("""
                    update kiln set name = :name, site = :site, kind = :kind, max_pieces_per_member = :cap,
                                    version = version + 1
                    where id = :id and version = :version
                    """)
                    .param("name", settings.name())
                    .param("site", settings.site())
                    .param("kind", settings.kind().name())
                    .param("cap", settings.maxPiecesPerMember())
                    .param("id", kilnId)
                    .param("version", expectedVersion)
                    .update();
            requireUpdated(updated, kilnId);
        } catch (DuplicateKeyException e) {
            throw new InvalidInputException("There's already a kiln called '%s'.".formatted(settings.name()));
        }
        return get(kilnId);
    }

    /** Takes a kiln out of service (with a reason) or, with a null reason, puts it back. */
    @Transactional
    public KilnView setDown(UUID kilnId, @Nullable String reason, long expectedVersion) {
        String cleaned = reason == null || reason.isBlank() ? null : reason.strip();
        if (cleaned != null && cleaned.length() > 200) {
            throw new InvalidInputException("Keep the reason under 200 characters.");
        }
        requireUpdated(jdbc.sql("""
                update kiln set down_reason = :reason, version = version + 1 where id = :id and version = :version
                """)
                .param("reason", cleaned)
                .param("id", kilnId)
                .param("version", expectedVersion)
                .update(), kilnId);
        return get(kilnId);
    }

    /** What staff can set on a kiln. */
    public record KilnSettings(String name, @Nullable String site, KilnView.KilnType kind,
            @Nullable Integer maxPiecesPerMember) {

        public KilnSettings {
            name = name.strip();
            if (name.isEmpty() || name.length() > 60) {
                throw new InvalidInputException("A kiln needs a name of 1 to 60 characters.");
            }
            site = site == null || site.isBlank() ? null : site.strip();
            if (site != null && site.length() > 60) {
                throw new InvalidInputException("Keep the site name under 60 characters.");
            }
        }

    }

    static KilnView mapKiln(ResultSet rs, int row) throws SQLException {
        return new KilnView(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("site"),
                KilnView.KilnType.valueOf(rs.getString("kind")), rs.getObject("max_pieces_per_member", Integer.class),
                rs.getInt("total_firings"), rs.getString("down_reason"), rs.getLong("version"));
    }

    private void requireUpdated(int updated, UUID kilnId) {
        if (updated == 0) {
            get(kilnId);
            throw new ConflictException("Someone else changed this kiln at the same time. Reload and try again.");
        }
    }

    static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

}
