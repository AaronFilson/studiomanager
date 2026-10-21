package io.github.aaronfilson.studiomanager.today;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import io.github.aaronfilson.studiomanager.kiln.FiringService;
import io.github.aaronfilson.studiomanager.kiln.FiringView;
import io.github.aaronfilson.studiomanager.kiln.KilnService;
import io.github.aaronfilson.studiomanager.kiln.KilnView;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceService;
import io.github.aaronfilson.studiomanager.piece.PieceService;
import io.github.aaronfilson.studiomanager.piece.PieceStage;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.Booking;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.Shift;
import io.github.aaronfilson.studiomanager.schedule.ScheduleService;
import io.github.aaronfilson.studiomanager.studio.StudioService;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel;
import io.github.aaronfilson.studiomanager.supplies.SupplyService;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Studio today": the handover view. Whoever is covering the studio (a manager, a monitor, a
 * volunteer) sees what is in each kiln, what is waiting, what needs attention, and who is on.
 * The research found a hired manager is a single point of failure; this is the answer to
 * "what does the person covering for me need to know?"
 */
@Service
public class TodayService {

    static final Duration GAP_HORIZON = Duration.ofDays(7);

    private final StudioService studios;
    private final KilnService kilns;
    private final FiringService firings;
    private final PieceService pieces;
    private final ScheduleService schedule;
    private final SupplyService supplies;
    private final MaintenanceService maintenance;

    TodayService(StudioService studios, KilnService kilns, FiringService firings, PieceService pieces,
            ScheduleService schedule, SupplyService supplies, MaintenanceService maintenance) {
        this.studios = studios;
        this.kilns = kilns;
        this.firings = firings;
        this.pieces = pieces;
        this.schedule = schedule;
        this.supplies = supplies;
        this.maintenance = maintenance;
    }

    @Transactional(readOnly = true)
    public Today today(Instant now) {
        TenantContext caller = TenantContext.require();
        if (caller.role() == null || !caller.role().includes(StudioRole.STAFF)) {
            throw new AccessDeniedException("The studio overview is for staff.");
        }
        ZoneId zone = studios.current().timeZone();
        LocalDate date = now.atZone(zone).toLocalDate();
        Instant dayStart = date.atStartOfDay(zone).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();

        List<KilnView> allKilns = kilns.list();
        PieceService.StageCounts counts = pieces.counts();
        List<Shift> upcoming = schedule.shifts(now, now.plus(GAP_HORIZON));
        return new Today(date, allKilns, firings.open(),
                counts.in(PieceStage.RECEIVED), counts.in(PieceStage.GLAZED), counts.in(PieceStage.READY),
                counts.overdue(), counts.in(PieceStage.UNCLAIMED),
                maintenance.dueTasks(now),
                allKilns.stream().filter(k -> !k.available()).toList(),
                schedule.bookings(dayStart, dayEnd),
                schedule.shifts(dayStart, dayEnd),
                upcoming.stream().filter(Shift::open).toList(),
                supplies.needingOrder(),
                supplies.openRequests().size());
    }

    /**
     * @param maintenanceDue tasks due or due soon on any asset, most urgent first
     * @param uncoveredShifts open shifts in the next seven days, so gaps are seen before they happen
     * @param lowStock items at or below par with nothing on order
     */
    public record Today(LocalDate date, List<KilnView> kilns, List<FiringView> openFirings, int waitingForBisque,
            int waitingForGlaze, int readyForPickup, int pastHold, int unclaimed, List<MaintenanceModel.Task> maintenanceDue,
            List<KilnView> outOfService, List<Booking> bookings, List<Shift> shifts, List<Shift> uncoveredShifts,
            List<SupplyModel.Item> lowStock, int openPurchaseRequests) {
    }

}
