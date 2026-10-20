package io.github.aaronfilson.studiomanager.schedule;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.access.Duty;
import io.github.aaronfilson.studiomanager.contract.AdjustHoursRequestDto;
import io.github.aaronfilson.studiomanager.contract.AssignShiftRequestDto;
import io.github.aaronfilson.studiomanager.contract.BookingDto;
import io.github.aaronfilson.studiomanager.contract.CreateBookingRequestDto;
import io.github.aaronfilson.studiomanager.contract.CreateResourceRequestDto;
import io.github.aaronfilson.studiomanager.contract.CreateShiftsRequestDto;
import io.github.aaronfilson.studiomanager.contract.HoursEntryDto;
import io.github.aaronfilson.studiomanager.contract.HoursLedgerDto;
import io.github.aaronfilson.studiomanager.contract.ResourceDto;
import io.github.aaronfilson.studiomanager.contract.ResourceKindDto;
import io.github.aaronfilson.studiomanager.contract.ScheduleApi;
import io.github.aaronfilson.studiomanager.contract.ShiftDto;
import io.github.aaronfilson.studiomanager.contract.VersionRequestDto;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.BookingKind;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.HoursLedger;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.Resource;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.ResourceKind;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ScheduleController implements ScheduleApi {

    private final ScheduleService schedule;

    ScheduleController(ScheduleService schedule) {
        this.schedule = schedule;
    }

    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<List<ResourceDto>> listResources(UUID studioId) {
        return ResponseEntity.ok(schedule.resources().stream().map(ScheduleController::toDto).toList());
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<ResourceDto> createResource(UUID studioId, CreateResourceRequestDto request) {
        Resource resource = schedule.createResource(request.getName(), ResourceKind.valueOf(request.getKind().name()),
                request.getCapacity(), request.getRequiredCertification());
        return ResponseEntity.created(URI.create("/api/v1/studios/%s/resources/%s".formatted(studioId, resource.id())))
                .body(toDto(resource));
    }

    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<List<BookingDto>> listBookings(OffsetDateTime from, OffsetDateTime to, UUID studioId) {
        return ResponseEntity.ok(schedule.bookings(from.toInstant(), to.toInstant()).stream()
                .map(ScheduleDtos::toDto).toList());
    }

    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<BookingDto> createBooking(UUID studioId, CreateBookingRequestDto request) {
        BookingDto booking = ScheduleDtos.toDto(schedule.book(request.getResourceId(),
                // getValue, not name: the generator renames the CLASS constant, a reserved word in Java
                BookingKind.valueOf(request.getKind().getValue()), request.getTitle(), request.getStartsAt().toInstant(),
                request.getEndsAt().toInstant(), request.getAccountId()));
        return ResponseEntity.created(URI.create("/api/v1/studios/%s/bookings/%s".formatted(studioId, booking.getId())))
                .body(booking);
    }

    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<Void> cancelBooking(UUID studioId, UUID bookingId) {
        schedule.cancelBooking(bookingId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<List<ShiftDto>> listShifts(OffsetDateTime from, OffsetDateTime to, UUID studioId) {
        return ResponseEntity.ok(schedule.shifts(from.toInstant(), to.toInstant()).stream()
                .map(ScheduleDtos::toDto).toList());
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<List<ShiftDto>> createShifts(UUID studioId, CreateShiftsRequestDto request) {
        Duty duty = request.getDuty() == null ? null : Duty.valueOf(request.getDuty().name());
        return ResponseEntity.status(HttpStatus.CREATED).body(schedule.createWeeklyShifts(request.getTitle(), duty,
                        request.getFirstStartsAt().toInstant(), request.getMinutes(), request.getWeeks())
                .stream().map(ScheduleDtos::toDto).toList());
    }

    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<ShiftDto> assignShift(UUID studioId, UUID shiftId, AssignShiftRequestDto request) {
        return ResponseEntity.ok(ScheduleDtos.toDto(schedule.assign(shiftId, request.getAccountId(),
                request.getExpectedVersion())));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<ShiftDto> markShiftWorked(UUID studioId, UUID shiftId, VersionRequestDto request) {
        return ResponseEntity.ok(ScheduleDtos.toDto(schedule.markWorked(shiftId, request.getExpectedVersion())));
    }

    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<HoursLedgerDto> getHours(UUID studioId, UUID accountId) {
        return ResponseEntity.ok(toDto(schedule.hours(accountId)));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<HoursLedgerDto> adjustHours(UUID studioId, UUID accountId, AdjustHoursRequestDto request) {
        return ResponseEntity.ok(toDto(schedule.adjustHours(accountId, request.getMinutes(), request.getNote())));
    }

    private static ResourceDto toDto(Resource resource) {
        return new ResourceDto()
                .id(resource.id())
                .name(resource.name())
                .kind(ResourceKindDto.valueOf(resource.kind().name()))
                .capacity(resource.capacity())
                .requiredCertification(resource.requiredCertification())
                .requiredCertificationName(resource.requiredCertificationName())
                .version(resource.version());
    }

    private static HoursLedgerDto toDto(HoursLedger ledger) {
        return new HoursLedgerDto()
                .accountId(ledger.accountId())
                .accountName(ledger.accountName())
                .totalMinutes(ledger.totalMinutes())
                .entries(ledger.entries().stream()
                        .map(e -> new HoursEntryDto()
                                .id(e.id())
                                .minutes(e.minutes())
                                .note(e.note())
                                .shiftId(e.shiftId())
                                .creditedAt(e.creditedAt().atOffset(ZoneOffset.UTC))
                                .creditedByName(e.creditedByName()))
                        .toList());
    }

}
