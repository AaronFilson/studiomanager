package io.github.aaronfilson.studiomanager.piece;

import io.github.aaronfilson.studiomanager.contract.ActorKindDto;
import io.github.aaronfilson.studiomanager.contract.MoneyDto;
import io.github.aaronfilson.studiomanager.contract.PieceDto;
import io.github.aaronfilson.studiomanager.contract.PieceEventDto;
import io.github.aaronfilson.studiomanager.contract.PieceFeeDto;
import io.github.aaronfilson.studiomanager.contract.PiecePhotoDto;
import io.github.aaronfilson.studiomanager.contract.PieceStageDto;
import io.github.aaronfilson.studiomanager.firing.PieceSizes;
import io.github.aaronfilson.studiomanager.platform.tenancy.SystemActor;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Converts pieces to the HTTP contract, for this module's controller and any other that shows
 * pieces.
 */
public final class PieceDtos {

  private PieceDtos() {}

  public static PieceDto toDto(PieceView piece, Instant now) {
    return new PieceDto()
        .id(piece.id())
        .number(piece.number())
        .makerAccountId(piece.makerAccountId())
        .makerName(piece.makerName())
        .description(piece.description())
        .clayBody(piece.clayBody())
        .makerMark(piece.makerMark())
        .size(PieceSizes.toDto(piece.size()))
        .stage(PieceStageDto.valueOf(piece.stage().name()))
        .fee(
            new PieceFeeDto()
                .amount(
                    new MoneyDto()
                        .amount(piece.fee().amount().toPlainString())
                        .currency(piece.fee().currency().getCurrencyCode()))
                .scheduleVersion(piece.feeScheduleVersion())
                .taxCategory(piece.feeTaxCategory())
                .breakdown(piece.feeBreakdown()))
        .receivedAt(piece.receivedAt().atOffset(ZoneOffset.UTC))
        .readyAt(piece.readyAt() == null ? null : piece.readyAt().atOffset(ZoneOffset.UTC))
        .holdUntil(piece.holdUntil() == null ? null : piece.holdUntil().atOffset(ZoneOffset.UTC))
        .overdue(piece.overdue(now))
        .nextStages(
            piece.stage().next().stream()
                .map(stage -> PieceStageDto.valueOf(stage.name()))
                .toList())
        .version(piece.version())
        .photos(
            piece.photos().stream()
                .map(photo ->
                    new PiecePhotoDto()
                        .id(photo.id())
                        .url(photo.url())
                        .thumbnailUrl(photo.thumbnailUrl())
                        .uploadedByAccountId(photo.uploadedBy())
                        .uploadedAt(photo.uploadedAt().atOffset(ZoneOffset.UTC)))
                .toList())
        .timeline(
            piece.timeline().stream()
                .map(event ->
                    new PieceEventDto()
                        .from(
                            event.from() == null
                                ? null
                                : PieceStageDto.valueOf(event.from().name()))
                        .to(PieceStageDto.valueOf(event.to().name()))
                        .note(event.note())
                        .actorAccountId(event.actorAccountId())
                        .actorName(event.actorName())
                        .actorKind(actorKind(event.actorAccountId()))
                        .occurredAt(event.occurredAt().atOffset(ZoneOffset.UTC)))
                .toList());
  }

  /** A job's own account, or no account at all (a job's change from before jobs had accounts). */
  static ActorKindDto actorKind(@Nullable UUID actorAccountId) {
    return actorAccountId == null || SystemActor.byAccountId(actorAccountId).isPresent()
        ? ActorKindDto.SYSTEM
        : ActorKindDto.PERSON;
  }
}
