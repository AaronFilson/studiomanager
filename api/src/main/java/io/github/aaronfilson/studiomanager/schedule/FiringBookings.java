package io.github.aaronfilson.studiomanager.schedule;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.kiln.FiringEnded;
import io.github.aaronfilson.studiomanager.kiln.FiringKind;
import io.github.aaronfilson.studiomanager.kiln.FiringStarted;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceService;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;

import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Puts firings on the calendar by themselves. When a kiln starts, every calendar resource that is
 * that kiln (linked to its asset) gets a FIRING booking; when it's unloaded the booking ends then,
 * and a cancelled load's booking goes. Staff no longer type firings in twice, and members see
 * when a kiln is busy.
 *
 * <p>A firing is a fact, not a request, so it's booked even over other bookings: the calendar
 * then shows the clash for staff to sort out.
 */
@Component
class FiringBookings {

    /** Long enough for a glaze firing and cooling; trimmed to the real unload time. */
    static final Duration EXPECTED_LENGTH = Duration.ofHours(24);

    private final JdbcClient jdbc;
    private final MaintenanceService maintenance;

    FiringBookings(JdbcClient jdbc, MaintenanceService maintenance) {
        this.jdbc = jdbc;
        this.maintenance = maintenance;
    }

    @EventListener
    void onStarted(FiringStarted firing) {
        UUID asset = maintenance.assetOfKiln(firing.kilnId()).orElse(null);
        if (asset == null) {
            return;
        }
        List<UUID> resources = jdbc.sql("select id from resource where asset_id = :assetId")
                .param("assetId", asset).query(UUID.class).list();
        String title = "%s firing".formatted(firing.kind() == FiringKind.BISQUE ? "Bisque" : "Glaze");
        for (UUID resource : resources) {
            jdbc.sql("""
                    insert into booking (id, studio_id, resource_id, kind, title, starts_at, ends_at, created_by,
                                         created_at, firing_id)
                    values (:id, :studioId, :resourceId, 'FIRING', :title, :start, :end, :by, :start, :firingId)
                    """)
                    .param("id", Uuids.v7())
                    .param("studioId", firing.studioId())
                    .param("resourceId", resource)
                    .param("title", title)
                    .param("start", firing.startedAt().atOffset(ZoneOffset.UTC))
                    .param("end", firing.startedAt().plus(EXPECTED_LENGTH).atOffset(ZoneOffset.UTC))
                    .param("by", TenantContext.require().accountId())
                    .param("firingId", firing.firingId())
                    .update();
        }
    }

    @EventListener
    void onEnded(FiringEnded firing) {
        if (!firing.completed()) {
            jdbc.sql("delete from booking where firing_id = :firingId").param("firingId", firing.firingId()).update();
            return;
        }
        // At least a minute long, so a firing unloaded at once still shows
        Instant end = firing.endedAt();
        jdbc.sql("""
                update booking set ends_at = greatest(:end, starts_at + interval '1 minute')
                where firing_id = :firingId
                """)
                .param("end", end.atOffset(ZoneOffset.UTC))
                .param("firingId", firing.firingId())
                .update();
    }

}
