package io.github.aaronfilson.studiomanager.piece;

import io.github.aaronfilson.studiomanager.contract.IntakePieceRequestDto;
import io.github.aaronfilson.studiomanager.contract.MovePieceRequestDto;
import io.github.aaronfilson.studiomanager.contract.PhotoUploadDto;
import io.github.aaronfilson.studiomanager.contract.PieceDto;
import io.github.aaronfilson.studiomanager.contract.PieceListDto;
import io.github.aaronfilson.studiomanager.contract.PiecePolicyDto;
import io.github.aaronfilson.studiomanager.contract.PieceStageDto;
import io.github.aaronfilson.studiomanager.contract.PiecesApi;
import io.github.aaronfilson.studiomanager.firing.PieceSizes;
import io.github.aaronfilson.studiomanager.platform.Capped;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PieceController implements PiecesApi {

  private final PieceService pieces;
  private final PiecePhotoService photos;
  private final Clock clock;

  PieceController(PieceService pieces, PiecePhotoService photos, Clock clock) {
    this.pieces = pieces;
    this.photos = photos;
    this.clock = clock;
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'PIECES_READ')")
  public ResponseEntity<PieceListDto> listPieces(
      UUID studioId, @Nullable PieceStageDto stage, @Nullable UUID maker) {
    Instant now = clock.instant();
    Capped<PieceDto> listed =
        pieces
            .list(stage == null ? null : PieceStage.valueOf(stage.name()), maker)
            .map(piece -> PieceDtos.toDto(piece, now));
    return ResponseEntity.ok(new PieceListDto(listed.items(), listed.truncated(), listed.limit()));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'PIECES_INTAKE_OWN')")
  public ResponseEntity<PieceDto> intakePiece(UUID studioId, IntakePieceRequestDto request) {
    PieceView piece =
        pieces.intake(
            new PieceIntake(
                request.getMakerAccountId(),
                request.getDescription(),
                request.getClayBody(),
                request.getMakerMark(),
                PieceSizes.fromDto(request.getSize())));
    return ResponseEntity.created(
            URI.create("/api/v1/studios/%s/pieces/%s".formatted(studioId, piece.id())))
        .body(PieceDtos.toDto(piece, clock.instant()));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'PIECES_READ')")
  public ResponseEntity<PieceDto> getPiece(UUID studioId, UUID pieceId) {
    return ResponseEntity.ok(PieceDtos.toDto(pieces.get(pieceId), clock.instant()));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'PIECES_MOVE')")
  public ResponseEntity<PieceDto> movePiece(
      UUID studioId, UUID pieceId, MovePieceRequestDto request) {
    PieceView piece =
        pieces.move(
            pieceId,
            PieceStage.valueOf(request.getTo().name()),
            request.getNote(),
            request.getExpectedVersion());
    return ResponseEntity.ok(PieceDtos.toDto(piece, clock.instant()));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'PIECES_INTAKE_OWN')")
  public ResponseEntity<PhotoUploadDto> startPhotoUpload(UUID studioId, UUID pieceId) {
    PiecePhotoService.PhotoUpload upload = photos.startUpload(pieceId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new PhotoUploadDto()
                .photoId(upload.photoId())
                .photoUploadUrl(upload.photoUploadUrl())
                .thumbnailUploadUrl(upload.thumbnailUploadUrl())
                .expiresAt(upload.expiresAt().atOffset(ZoneOffset.UTC))
                .maxPhotoBytes(upload.maxPhotoBytes())
                .maxThumbnailBytes(upload.maxThumbnailBytes()));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'PIECES_INTAKE_OWN')")
  public ResponseEntity<PieceDto> completePhotoUpload(UUID studioId, UUID pieceId, UUID photoId) {
    return ResponseEntity.ok(PieceDtos.toDto(photos.complete(pieceId, photoId), clock.instant()));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'PIECES_INTAKE_OWN')")
  public ResponseEntity<Void> deletePhoto(UUID studioId, UUID pieceId, UUID photoId) {
    photos.delete(pieceId, photoId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'PIECES_READ')")
  public ResponseEntity<PiecePolicyDto> getPiecePolicy(UUID studioId) {
    return ResponseEntity.ok(new PiecePolicyDto().holdDays(pieces.holdDays()));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'PIECES_POLICY_EDIT')")
  public ResponseEntity<PiecePolicyDto> savePiecePolicy(UUID studioId, PiecePolicyDto request) {
    return ResponseEntity.ok(
        new PiecePolicyDto().holdDays(pieces.setHoldDays(request.getHoldDays())));
  }
}
