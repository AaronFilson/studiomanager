package io.github.aaronfilson.studiomanager.kiln;

import java.math.BigDecimal;
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
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;

import org.jspecify.annotations.Nullable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The bound studio's kilns, their availability, and their maintenance log. */
@Service
public class KilnService {

    static final String SELECT_KILN = """
            select id, name, site, kind, max_pieces_per_member, maintenance_every_firings,
                   firings_since_maintenance, total_firings, down_reason, version
            from kiln
            """;

    private final JdbcClient jdbc;
    private final AccountDirectory accounts;

    KilnService(JdbcClient jdbc, AccountDirectory accounts) {
        this.jdbc = jdbc;
        this.accounts = accounts;
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
                    insert into kiln (id, studio_id, name, site, kind, max_pieces_per_member, maintenance_every_firings,
                                      created_at, version)
                    values (:id, :studioId, :name, :site, :kind, :cap, :every, :now, 0)
                    """)
                    .param("id", id)
                    .param("studioId", caller.studioId())
                    .param("name", settings.name())
                    .param("site", settings.site())
                    .param("kind", settings.kind().name())
                    .param("cap", settings.maxPiecesPerMember())
                    .param("every", settings.maintenanceEveryFirings())
                    .param("now", utc(now()))
                    .update();
        } catch (DuplicateKeyException e) {
            throw new InvalidInputException("There's already a kiln called '%s'.".formatted(settings.name()));
        }
        return get(id);
    }

    @Transactional
    public KilnView update(UUID kilnId, KilnSettings settings, long expectedVersion) {
        try {
            int updated = jdbc.sql("""
                    update kiln set name = :name, site = :site, kind = :kind, max_pieces_per_member = :cap,
                                    maintenance_every_firings = :every, version = version + 1
                    where id = :id and version = :version
                    """)
                    .param("name", settings.name())
                    .param("site", settings.site())
                    .param("kind", settings.kind().name())
                    .param("cap", settings.maxPiecesPerMember())
                    .param("every", settings.maintenanceEveryFirings())
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

    /** Logs maintenance and restarts the kiln's count of firings since it was last serviced. */
    @Transactional
    public KilnView recordMaintenance(UUID kilnId, String note, @Nullable BigDecimal cost) {
        TenantContext caller = TenantContext.require();
        if (note.isBlank() || note.strip().length() > 500) {
            throw new InvalidInputException("Describe the work in 1 to 500 characters.");
        }
        KilnView kiln = get(kilnId);
        jdbc.sql("""
                insert into kiln_maintenance (id, studio_id, kiln_id, note, cost, at_firing, performed_by, performed_at)
                values (:id, :studioId, :kilnId, :note, :cost, :atFiring, :by, :now)
                """)
                .param("id", Uuids.v7())
                .param("studioId", caller.studioId())
                .param("kilnId", kilnId)
                .param("note", note.strip())
                .param("cost", cost)
                .param("atFiring", kiln.totalFirings())
                .param("by", caller.accountId())
                .param("now", utc(now()))
                .update();
        jdbc.sql("update kiln set firings_since_maintenance = 0, version = version + 1 where id = :id")
                .param("id", kilnId).update();
        return get(kilnId);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceEntry> maintenanceLog(UUID kilnId) {
        get(kilnId);
        List<MaintenanceEntry> entries = jdbc.sql("""
                select note, cost, at_firing, performed_by, performed_at
                from kiln_maintenance where kiln_id = :id order by performed_at desc limit 200
                """)
                .param("id", kilnId)
                .query((rs, row) -> new MaintenanceEntry(rs.getString("note"), rs.getBigDecimal("cost"),
                        rs.getInt("at_firing"), rs.getObject("performed_by", UUID.class), null,
                        rs.getObject("performed_at", OffsetDateTime.class).toInstant()))
                .list();
        var names = accounts.displayNames(entries.stream().map(MaintenanceEntry::performedBy).toList());
        return entries.stream()
                .map(e -> new MaintenanceEntry(e.note(), e.cost(), e.atFiring(), e.performedBy(),
                        names.get(e.performedBy()), e.performedAt()))
                .toList();
    }

    public record MaintenanceEntry(String note, @Nullable BigDecimal cost, int atFiring, UUID performedBy,
            @Nullable String performedByName, Instant performedAt) {
    }

    /** What staff can set on a kiln. */
    public record KilnSettings(String name, @Nullable String site, KilnView.KilnType kind,
            @Nullable Integer maxPiecesPerMember, @Nullable Integer maintenanceEveryFirings) {

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
                rs.getObject("maintenance_every_firings", Integer.class), rs.getInt("firings_since_maintenance"),
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
