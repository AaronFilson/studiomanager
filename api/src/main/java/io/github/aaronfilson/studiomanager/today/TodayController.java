package io.github.aaronfilson.studiomanager.today;

import java.time.Instant;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.contract.TodayApi;
import io.github.aaronfilson.studiomanager.contract.TodayDto;
import io.github.aaronfilson.studiomanager.kiln.KilnDtos;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceDtos;
import io.github.aaronfilson.studiomanager.schedule.ScheduleDtos;
import io.github.aaronfilson.studiomanager.supplies.SupplyDtos;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class TodayController implements TodayApi {

    private final TodayService today;

    TodayController(TodayService today) {
        this.today = today;
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<TodayDto> getToday(UUID studioId) {
        TodayService.Today t = today.today(Instant.now());
        return ResponseEntity.ok(new TodayDto()
                .date(t.date())
                .kilns(t.kilns().stream().map(KilnDtos::toDto).toList())
                .openFirings(t.openFirings().stream().map(KilnDtos::toDto).toList())
                .waitingForBisque(t.waitingForBisque())
                .waitingForGlaze(t.waitingForGlaze())
                .readyForPickup(t.readyForPickup())
                .pastHold(t.pastHold())
                .unclaimed(t.unclaimed())
                .maintenanceDue(t.maintenanceDue().stream().map(MaintenanceDtos::toDto).toList())
                .outOfService(t.outOfService().stream().map(KilnDtos::toDto).toList())
                .bookings(t.bookings().stream().map(ScheduleDtos::toDto).toList())
                .shifts(t.shifts().stream().map(ScheduleDtos::toDto).toList())
                .uncoveredShifts(t.uncoveredShifts().stream().map(ScheduleDtos::toDto).toList())
                .lowStock(t.lowStock().stream().map(SupplyDtos::toDto).toList())
                .openPurchaseRequests(t.openPurchaseRequests()));
    }

}
