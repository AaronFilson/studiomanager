package io.github.aaronfilson.studiomanager.maintenance;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.aaronfilson.studiomanager.access.AccessService;
import io.github.aaronfilson.studiomanager.access.Duty;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.Asset;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.ProblemReport;
import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.MemberView;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Problems members notice, such as a wheel that grinds or a vent that's stopped. Anyone in the
 * studio can report one; the staff and technicians hear about it, and staff resolve it.
 *
 * <p>The technician postings in the research name catching problems early as part of the job,
 * and members are usually the first to notice. A report from the phone at the wheel is the
 * cheapest way to get that to whoever fixes it.
 */
@Service
public class ProblemReportService {

    private final JdbcClient jdbc;
    private final MaintenanceService maintenance;
    private final AccessService access;
    private final StudioMembers members;
    private final AccountDirectory accounts;
    private final ApplicationEventPublisher events;

    ProblemReportService(JdbcClient jdbc, MaintenanceService maintenance, AccessService access, StudioMembers members,
            AccountDirectory accounts, ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.maintenance = maintenance;
        this.access = access;
        this.members = members;
        this.accounts = accounts;
        this.events = events;
    }

    /** Everything a member can report a problem with: every tracked asset, with whether it's down. */
    @Transactional(readOnly = true)
    public List<Asset> reportable() {
        return maintenance.assets(Instant.now());
    }

    @Transactional
    public ProblemReport report(@Nullable UUID assetId, String description) {
        String cleaned = description.strip();
        if (cleaned.isEmpty() || cleaned.length() > 1000) {
            throw new InvalidInputException("Describe the problem in up to 1,000 characters.");
        }
        Asset asset = assetId == null ? null : maintenance.asset(assetId, Instant.now());
        TenantContext caller = TenantContext.require();
        UUID id = Uuids.v7();
        jdbc.sql("""
                insert into problem_report (id, studio_id, asset_id, description, reported_by, reported_at, version)
                values (:id, :studioId, :assetId, :description, :by, :now, 0)
                """)
                .param("id", id)
                .param("studioId", caller.studioId())
                .param("assetId", assetId)
                .param("description", cleaned)
                .param("by", caller.accountId())
                .param("now", utc(now()))
                .update();
        events.publishEvent(new ProblemReported(caller.studioId(), id, asset == null ? null : asset.name(), cleaned,
                accounts.displayNames(List.of(caller.accountId())).get(caller.accountId()), recipients(caller.accountId())));
        return find(id);
    }

    /** Staff see every report; anyone else only their own. Newest first, at most 200. */
    @Transactional(readOnly = true)
    public List<ProblemReport> reports(boolean openOnly) {
        TenantContext caller = TenantContext.require();
        return query("""
                where (:all or p.reported_by = :me) and (not :openOnly or p.resolved_at is null)
                order by p.reported_at desc limit 200
                """, Map.of("all", isStaff(caller), "me", caller.accountId(), "openOnly", openOnly));
    }

    /** Closes a report, saying what was done or why nothing was. Staff only. */
    @Transactional
    public ProblemReport resolve(UUID reportId, @Nullable String resolution, long expectedVersion) {
        TenantContext caller = TenantContext.require();
        if (!isStaff(caller)) {
            throw new AccessDeniedException("Only staff can resolve a problem report.");
        }
        find(reportId);
        String cleaned = resolution == null || resolution.isBlank() ? null : resolution.strip();
        if (cleaned != null && cleaned.length() > 1000) {
            throw new InvalidInputException("Keep the resolution to 1,000 characters.");
        }
        int updated = jdbc.sql("""
                update problem_report set resolved_by = :by, resolved_at = :now, resolution = :resolution,
                       version = version + 1
                where id = :id and version = :version and resolved_at is null
                """)
                .param("by", caller.accountId())
                .param("now", utc(now()))
                .param("resolution", cleaned)
                .param("id", reportId)
                .param("version", expectedVersion)
                .update();
        if (updated == 0) {
            throw new ConflictException("Someone else changed or resolved this report. Reload and try again.");
        }
        return find(reportId);
    }

    private ProblemReport find(UUID reportId) {
        TenantContext caller = TenantContext.require();
        return query("where p.id = :id and (:all or p.reported_by = :me)",
                Map.of("id", reportId, "all", isStaff(caller), "me", caller.accountId()))
                .stream().findFirst()
                .orElseThrow(() -> new NotFoundException("No such report."));
    }

    private List<ProblemReport> query(String where, Map<String, Object> params) {
        record Row(UUID id, @Nullable UUID assetId, String description, UUID reportedBy, Instant reportedAt,
                @Nullable UUID resolvedBy, @Nullable Instant resolvedAt, @Nullable String resolution, long version) {
        }
        List<Row> rows = jdbc.sql("""
                select p.id, p.asset_id, p.description, p.reported_by, p.reported_at, p.resolved_by, p.resolved_at,
                       p.resolution, p.version
                from problem_report p
                """ + where)
                .params(params)
                .query((rs, n) -> new Row(rs.getObject("id", UUID.class), rs.getObject("asset_id", UUID.class),
                        rs.getString("description"), rs.getObject("reported_by", UUID.class),
                        rs.getObject("reported_at", OffsetDateTime.class).toInstant(),
                        rs.getObject("resolved_by", UUID.class),
                        rs.getObject("resolved_at", OffsetDateTime.class) == null ? null
                                : rs.getObject("resolved_at", OffsetDateTime.class).toInstant(),
                        rs.getString("resolution"), rs.getLong("version")))
                .list();
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> assetNames = maintenance.assets(Instant.now()).stream()
                .collect(Collectors.toMap(Asset::id, Asset::name));
        Map<UUID, String> names = accounts.displayNames(rows.stream()
                .flatMap(r -> Stream.of(r.reportedBy(), r.resolvedBy())).filter(Objects::nonNull).distinct().toList());
        return rows.stream()
                .map(r -> new ProblemReport(r.id(), r.assetId(), r.assetId() == null ? null : assetNames.get(r.assetId()),
                        r.description(), r.reportedBy(), names.get(r.reportedBy()), r.reportedAt(), r.resolvedBy(),
                        r.resolvedBy() == null ? null : names.get(r.resolvedBy()), r.resolvedAt(), r.resolution(),
                        r.version()))
                .toList();
    }

    /** The staff and every technician, without the reporter: they already know. */
    private List<UUID> recipients(UUID reporter) {
        Stream<UUID> staff = members.list().stream().filter(m -> m.role().includes(StudioRole.STAFF))
                .map(MemberView::accountId);
        Stream<UUID> technicians = access.allDuties().entrySet().stream()
                .filter(e -> e.getValue().contains(Duty.TECHNICIAN)).map(Map.Entry::getKey);
        return Stream.concat(staff, technicians).distinct().filter(id -> !id.equals(reporter)).sorted().toList();
    }

    private static boolean isStaff(TenantContext caller) {
        return caller.role() != null && caller.role().includes(StudioRole.STAFF);
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

}
