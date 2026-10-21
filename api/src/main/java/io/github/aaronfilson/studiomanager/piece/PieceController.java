package io.github.aaronfilson.studiomanager.piece;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.contract.IntakePieceRequestDto;
import io.github.aaronfilson.studiomanager.contract.MovePieceRequestDto;
import io.github.aaronfilson.studiomanager.contract.PhotoUploadDto;
import io.github.aaronfilson.studiomanager.contract.PieceDto;
import io.github.aaronfilson.studiomanager.contract.PiecePolicyDto;
import io.github.aaronfilson.studiomanager.contract.PieceStageDto;
import io.github.aaronfilson.studiomanager.contract.PiecesApi;
import io.github.aaronfilson.studiomanager.firing.PieceSizes;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PieceController implements PiecesApi {

    private final PieceService pieces;
    private final PiecePhotoService photos;

    PieceController(PieceService pieces, PiecePhotoService photos) {
        this.pieces = pieces;
        this.photos = photos;
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
    public ResponseEntity<PhotoUploadDto> startPhotoUpload(UUID studioId, UUID pieceId) {
        PiecePhotoService.PhotoUpload upload = photos.startUpload(pieceId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new PhotoUploadDto()
                .photoId(upload.photoId())
                .photoUploadUrl(upload.photoUploadUrl())
                .thumbnailUploadUrl(upload.thumbnailUploadUrl())
                .expiresAt(upload.expiresAt().atOffset(ZoneOffset.UTC))
                .maxPhotoBytes(upload.maxPhotoBytes())
                .maxThumbnailBytes(upload.maxThumbnailBytes()));
    }

    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<PieceDto> completePhotoUpload(UUID studioId, UUID pieceId, UUID photoId) {
        return ResponseEntity.ok(PieceDtos.toDto(photos.complete(pieceId, photoId), Instant.now()));
    }

    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<Void> deletePhoto(UUID studioId, UUID pieceId, UUID photoId) {
        photos.delete(pieceId, photoId);
        return ResponseEntity.noContent().build();
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
