package io.github.aaronfilson.studiomanager.kiln;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.contract.FiringDto;
import io.github.aaronfilson.studiomanager.contract.FiringKindDto;
import io.github.aaronfilson.studiomanager.contract.FiringTransitionRequestDto;
import io.github.aaronfilson.studiomanager.contract.KilnAvailabilityRequestDto;
import io.github.aaronfilson.studiomanager.contract.KilnDto;
import io.github.aaronfilson.studiomanager.contract.KilnsApi;
import io.github.aaronfilson.studiomanager.contract.LoadKilnRequestDto;
import io.github.aaronfilson.studiomanager.contract.QueueEntryDto;
import io.github.aaronfilson.studiomanager.contract.SaveKilnRequestDto;
import io.github.aaronfilson.studiomanager.contract.UpdateKilnRequestDto;
import io.github.aaronfilson.studiomanager.piece.PieceDtos;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class KilnController implements KilnsApi {

    private final KilnService kilns;
    private final FiringService firings;

    KilnController(KilnService kilns, FiringService firings) {
        this.kilns = kilns;
        this.firings = firings;
    }

    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<List<KilnDto>> listKilns(UUID studioId) {
        return ResponseEntity.ok(kilns.list().stream().map(KilnController::toDto).toList());
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<KilnDto> createKiln(UUID studioId, SaveKilnRequestDto request) {
        KilnView kiln = kilns.create(settings(request));
        return ResponseEntity.created(URI.create("/api/v1/studios/%s/kilns/%s".formatted(studioId, kiln.id())))
                .body(toDto(kiln));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<KilnDto> updateKiln(UUID studioId, UUID kilnId, UpdateKilnRequestDto request) {
        return ResponseEntity.ok(toDto(kilns.update(kilnId, settings(request.getKiln()), request.getExpectedVersion())));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<KilnDto> setKilnAvailability(UUID studioId, UUID kilnId, KilnAvailabilityRequestDto request) {
        return ResponseEntity.ok(toDto(kilns.setDown(kilnId, request.getDownReason(), request.getExpectedVersion())));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<List<QueueEntryDto>> getKilnQueue(FiringKindDto kind, UUID studioId, UUID kilnId) {
        Instant now = Instant.now();
        return ResponseEntity.ok(firings.queue(kilnId, FiringKind.valueOf(kind.name())).stream()
                .map(entry -> new QueueEntryDto()
                        .piece(PieceDtos.toDto(entry.piece().piece(), now))
                        .waitingSince(entry.piece().since().atOffset(ZoneOffset.UTC))
                        .fitsNextLoad(entry.fitsNextLoad()))
                .toList());
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<List<FiringDto>> listKilnFirings(UUID studioId, UUID kilnId) {
        return ResponseEntity.ok(firings.listForKiln(kilnId).stream().map(KilnController::toDto).toList());
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<FiringDto> loadKiln(UUID studioId, UUID kilnId, LoadKilnRequestDto request) {
        FiringView firing = firings.load(kilnId, FiringKind.valueOf(request.getKind().name()), request.getCone(),
                request.getPieceIds());
        return ResponseEntity.created(URI.create("/api/v1/studios/%s/firings/%s".formatted(studioId, firing.id())))
                .body(toDto(firing));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<FiringDto> getFiring(UUID studioId, UUID firingId) {
        return ResponseEntity.ok(toDto(firings.get(firingId)));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<FiringDto> transitionFiring(UUID studioId, UUID firingId, FiringTransitionRequestDto request) {
        long version = request.getExpectedVersion();
        FiringView firing = switch (request.getTo()) {
            case FIRING -> firings.start(firingId, version);
            case COMPLETE -> firings.complete(firingId, request.getNotes(), version);
            case CANCELLED -> firings.cancel(firingId, request.getNotes(), version);
        };
        return ResponseEntity.ok(toDto(firing));
    }

    private static KilnService.KilnSettings settings(SaveKilnRequestDto request) {
        return new KilnService.KilnSettings(request.getName(), request.getSite(),
                KilnView.KilnType.valueOf(request.getKind().name()), request.getMaxPiecesPerMember());
    }

    private static KilnDto toDto(KilnView kiln) {
        return KilnDtos.toDto(kiln);
    }

    private static FiringDto toDto(FiringView firing) {
        return KilnDtos.toDto(firing);
    }

}
