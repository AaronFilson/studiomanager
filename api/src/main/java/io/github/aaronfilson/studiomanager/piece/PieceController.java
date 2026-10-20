package io.github.aaronfilson.studiomanager.piece;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.contract.IntakePieceRequestDto;
import io.github.aaronfilson.studiomanager.contract.MovePieceRequestDto;
import io.github.aaronfilson.studiomanager.contract.PieceDto;
import io.github.aaronfilson.studiomanager.contract.PiecePolicyDto;
import io.github.aaronfilson.studiomanager.contract.PieceStageDto;
import io.github.aaronfilson.studiomanager.contract.PiecesApi;
import io.github.aaronfilson.studiomanager.firing.PieceSizes;

import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PieceController implements PiecesApi {

    private final PieceService pieces;

    PieceController(PieceService pieces) {
        this.pieces = pieces;
    }

    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<List<PieceDto>> listPieces(UUID studioId, @Nullable PieceStageDto stage, @Nullable UUID maker) {
        Instant now = Instant.now();
        return ResponseEntity.ok(pieces.list(stage == null ? null : PieceStage.valueOf(stage.name()), maker).stream()
                .map(piece -> PieceDtos.toDto(piece, now))
                .toList());
    }

    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<PieceDto> intakePiece(UUID studioId, IntakePieceRequestDto request) {
        PieceView piece = pieces.intake(new PieceIntake(request.getMakerAccountId(), request.getDescription(),
                request.getClayBody(), request.getMakerMark(), PieceSizes.fromDto(request.getSize())));
        return ResponseEntity.created(URI.create("/api/v1/studios/%s/pieces/%s".formatted(studioId, piece.id())))
                .body(PieceDtos.toDto(piece, Instant.now()));
    }

    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<PieceDto> getPiece(UUID studioId, UUID pieceId) {
        return ResponseEntity.ok(PieceDtos.toDto(pieces.get(pieceId), Instant.now()));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<PieceDto> movePiece(UUID studioId, UUID pieceId, MovePieceRequestDto request) {
        PieceView piece = pieces.move(pieceId, PieceStage.valueOf(request.getTo().name()), request.getNote(),
                request.getExpectedVersion());
        return ResponseEntity.ok(PieceDtos.toDto(piece, Instant.now()));
    }

    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<PiecePolicyDto> getPiecePolicy(UUID studioId) {
        return ResponseEntity.ok(new PiecePolicyDto().holdDays(pieces.holdDays()));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<PiecePolicyDto> savePiecePolicy(UUID studioId, PiecePolicyDto request) {
        return ResponseEntity.ok(new PiecePolicyDto().holdDays(pieces.setHoldDays(request.getHoldDays())));
    }

}
