package io.github.aaronfilson.studiomanager.schedule;

import io.github.aaronfilson.studiomanager.access.Duty;
import io.github.aaronfilson.studiomanager.contract.AdjustHoursRequestDto;
import io.github.aaronfilson.studiomanager.contract.AssignShiftRequestDto;
import io.github.aaronfilson.studiomanager.contract.BookingDto;
import io.github.aaronfilson.studiomanager.contract.BookingListDto;
import io.github.aaronfilson.studiomanager.contract.CreateBookingRequestDto;
import io.github.aaronfilson.studiomanager.contract.CreateResourceRequestDto;
import io.github.aaronfilson.studiomanager.contract.CreateShiftsRequestDto;
import io.github.aaronfilson.studiomanager.contract.HoursEntryDto;
import io.github.aaronfilson.studiomanager.contract.HoursLedgerDto;
import io.github.aaronfilson.studiomanager.contract.OfferShiftRequestDto;
import io.github.aaronfilson.studiomanager.contract.ResourceDto;
import io.github.aaronfilson.studiomanager.contract.ResourceKindDto;
import io.github.aaronfilson.studiomanager.contract.ScheduleApi;
import io.github.aaronfilson.studiomanager.contract.ShiftDto;
import io.github.aaronfilson.studiomanager.contract.ShiftListDto;
import io.github.aaronfilson.studiomanager.contract.VersionRequestDto;
import io.github.aaronfilson.studiomanager.platform.Capped;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.BookingKind;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.HoursLedger;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.Resource;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.ResourceKind;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ScheduleController implements ScheduleApi {

  private final ScheduleService schedule;
  private final ResourceChanges resourceChanges;
  private final BookingChanges bookingChanges;
  private final ShiftChanges shiftChanges;
  private final CreditedHours creditedHours;

  ScheduleController(
      ScheduleService schedule,
      ResourceChanges resourceChanges,
      BookingChanges bookingChanges,
      ShiftChanges shiftChanges,
      CreditedHours creditedHours) {
    this.schedule = schedule;
    this.resourceChanges = resourceChanges;
    this.bookingChanges = bookingChanges;
    this.shiftChanges = shiftChanges;
    this.creditedHours = creditedHours;
  }

  @Override
  @PreAuthorize("@studioAuthz.isMember(#studioId)")
  public ResponseEntity<List<ResourceDto>> listResources(UUID studioId) {
    return ResponseEntity.ok(schedule.resources().stream().map(ScheduleController::toDto).toList());
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<ResourceDto> createResource(
      UUID studioId, CreateResourceRequestDto request) {
    Resource resource =
        resourceChanges.createResource(
            request.getName(),
            ResourceKind.valueOf(request.getKind().name()),
            request.getCapacity(),
            request.getRequiredCertification(),
            request.getAssetId());
    return ResponseEntity.created(
            URI.create("/api/v1/studios/%s/resources/%s".formatted(studioId, resource.id())))
        .body(toDto(resource));
  }

  @Override
  @PreAuthorize("@studioAuthz.isMember(#studioId)")
  public ResponseEntity<BookingListDto> listBookings(
      OffsetDateTime from, OffsetDateTime to, UUID studioId) {
    Capped<BookingDto> calendar =
        schedule.bookings(from.toInstant(), to.toInstant()).map(ScheduleDtos::toDto);
    return ResponseEntity.ok(
        new BookingListDto(calendar.items(), calendar.truncated(), calendar.limit()));
  }

  @Override
  @PreAuthorize("@studioAuthz.isMember(#studioId)")
  public ResponseEntity<BookingDto> createBooking(UUID studioId, CreateBookingRequestDto request) {
    BookingDto booking =
        ScheduleDtos.toDto(
            bookingChanges.book(
                request.getResourceId(),
                // getValue, not name: the generator renames the CLASS constant, a reserved word in
                // Java
                BookingKind.valueOf(request.getKind().getValue()),
                request.getTitle(),
                request.getStartsAt().toInstant(),
                request.getEndsAt().toInstant(),
                request.getAccountId()));
    return ResponseEntity.created(
            URI.create("/api/v1/studios/%s/bookings/%s".formatted(studioId, booking.getId())))
        .body(booking);
  }

  @Override
  @PreAuthorize("@studioAuthz.isMember(#studioId)")
  public ResponseEntity<Void> cancelBooking(UUID studioId, UUID bookingId) {
    bookingChanges.cancelBooking(bookingId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PreAuthorize("@studioAuthz.isMember(#studioId)")
  public ResponseEntity<ShiftListDto> listShifts(
      OffsetDateTime from, OffsetDateTime to, UUID studioId) {
    Capped<ShiftDto> roster =
        schedule.shifts(from.toInstant(), to.toInstant()).map(ScheduleDtos::toDto);
    return ResponseEntity.ok(new ShiftListDto(roster.items(), roster.truncated(), roster.limit()));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<List<ShiftDto>> createShifts(
      UUID studioId, CreateShiftsRequestDto request) {
    Duty duty = request.getDuty() == null ? null : Duty.valueOf(request.getDuty().name());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            shiftChanges
                .createWeeklyShifts(
                    request.getTitle(),
                    duty,
                    request.getFirstStartsAt().toInstant(),
                    request.getMinutes(),
                    request.getWeeks())
                .stream()
                .map(ScheduleDtos::toDto)
                .toList());
  }

  @Override
  @PreAuthorize("@studioAuthz.isMember(#studioId)")
  public ResponseEntity<ShiftDto> assignShift(
      UUID studioId, UUID shiftId, AssignShiftRequestDto request) {
    return ResponseEntity.ok(
        ScheduleDtos.toDto(
            shiftChanges.assign(shiftId, request.getAccountId(), request.getExpectedVersion())));
  }

  /** Members may call this; the service lets through only whoever has the shift, and staff. */
  @Override
  @PreAuthorize("@studioAuthz.isMember(#studioId)")
  public ResponseEntity<ShiftDto> offerShift(
      UUID studioId, UUID shiftId, OfferShiftRequestDto request) {
    return ResponseEntity.ok(
        ScheduleDtos.toDto(
            shiftChanges.offer(shiftId, request.getOffered(), request.getExpectedVersion())));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<ShiftDto> markShiftWorked(
      UUID studioId, UUID shiftId, VersionRequestDto request) {
    return ResponseEntity.ok(
        ScheduleDtos.toDto(shiftChanges.markWorked(shiftId, request.getExpectedVersion())));
  }

  @Override
  @PreAuthorize("@studioAuthz.isMember(#studioId)")
  public ResponseEntity<HoursLedgerDto> getHours(UUID studioId, UUID accountId) {
    return ResponseEntity.ok(toDto(schedule.hours(accountId)));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<HoursLedgerDto> adjustHours(
      UUID studioId, UUID accountId, AdjustHoursRequestDto request) {
    return ResponseEntity.ok(
        toDto(creditedHours.adjustHours(accountId, request.getMinutes(), request.getNote())));
  }

  private static ResourceDto toDto(Resource resource) {
    return new ResourceDto()
        .id(resource.id())
        .name(resource.name())
        .kind(ResourceKindDto.valueOf(resource.kind().name()))
        .capacity(resource.capacity())
        .requiredCertification(resource.requiredCertification())
        .requiredCertificationName(resource.requiredCertificationName())
        .assetId(resource.assetId())
        .downReason(resource.downReason())
        .version(resource.version());
  }

  private static HoursLedgerDto toDto(HoursLedger ledger) {
    return new HoursLedgerDto()
        .accountId(ledger.accountId())
        .accountName(ledger.accountName())
        .totalMinutes(ledger.totalMinutes())
        .entries(
            ledger.entries().items().stream()
                .map(e ->
                    new HoursEntryDto()
                        .id(e.id())
                        .minutes(e.minutes())
                        .note(e.note())
                        .shiftId(e.shiftId())
                        .creditedAt(e.creditedAt().atOffset(ZoneOffset.UTC))
                        .creditedByName(e.creditedByName()))
                .toList())
        .truncated(ledger.entries().truncated())
        .limit(ledger.entries().limit());
  }
}
