package io.github.aaronfilson.studiomanager.kiln;

import io.github.aaronfilson.studiomanager.contract.FiringDto;
import io.github.aaronfilson.studiomanager.contract.FiringKindDto;
import io.github.aaronfilson.studiomanager.contract.FiringStatusDto;
import io.github.aaronfilson.studiomanager.contract.KilnDto;
import io.github.aaronfilson.studiomanager.contract.KilnLogImportResultDto;
import io.github.aaronfilson.studiomanager.contract.KilnLogSourceDto;
import io.github.aaronfilson.studiomanager.contract.KilnTypeDto;
import io.github.aaronfilson.studiomanager.contract.SuggestedFiringMatchDto;
import io.github.aaronfilson.studiomanager.contract.TemperaturePointDto;
import io.github.aaronfilson.studiomanager.contract.TemperatureUnitDto;
import io.github.aaronfilson.studiomanager.piece.PieceDtos;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

/** Converts kilns and firings to the HTTP contract. */
public final class KilnDtos {

  private KilnDtos() {}

  public static KilnDto toDto(KilnView kiln) {
    return new KilnDto()
        .id(kiln.id())
        .name(kiln.name())
        .site(kiln.site())
        .kind(KilnTypeDto.valueOf(kiln.kind().name()))
        .maxPiecesPerMember(kiln.maxPiecesPerMember())
        .totalFirings(kiln.totalFirings())
        .available(kiln.available())
        .downReason(kiln.downReason())
        .version(kiln.version());
  }

  /**
   * With the wall clock, for modules that don't take the clock yet (the today module). Prefer
   * {@link #toDto(FiringView, Instant)}.
   */
  public static FiringDto toDto(FiringView firing) {
    return toDto(firing, Instant.now());
  }

  /**
   * @param now the time pieces' holds are judged against, from the application's clock
   */
  public static FiringDto toDto(FiringView firing, Instant now) {
    FiringDto dto =
        new FiringDto()
            .id(firing.id())
            .kilnId(firing.kilnId())
            .kilnName(firing.kilnName())
            .kind(FiringKindDto.valueOf(firing.kind().name()))
            .cone(firing.cone())
            .status(FiringStatusDto.valueOf(firing.status().name()))
            .notes(firing.notes())
            .createdAt(firing.createdAt().atOffset(ZoneOffset.UTC))
            .startedAt(
                firing.startedAt() == null ? null : firing.startedAt().atOffset(ZoneOffset.UTC))
            .finishedAt(
                firing.finishedAt() == null ? null : firing.finishedAt().atOffset(ZoneOffset.UTC))
            .pieceCount(firing.pieceCount())
            .version(firing.version())
            .pieces(firing.pieces().stream().map(piece -> PieceDtos.toDto(piece, now)).toList())
            .peakTemp(firing.peakTemp() == null ? null : firing.peakTemp().doubleValue())
            .tempUnit(
                firing.tempUnit() == null
                    ? null
                    : TemperatureUnitDto.valueOf(firing.tempUnit().name()))
            .durationMinutes(firing.durationMinutes())
            .errorCode(firing.errorCode())
            .logSource(
                firing.logSource() == null
                    ? null
                    : KilnLogSourceDto.valueOf(firing.logSource().name()));
    List<TemperaturePoint> curve = firing.temperatureCurve();
    if (curve != null) {
      dto.temperatureCurve(curve.stream().map(KilnDtos::toPointDto).toList());
    }
    return dto;
  }

  static KilnLogImportResultDto toDto(KilnLogImportService.ImportResult result, Instant now) {
    return new KilnLogImportResultDto()
        .committed(result.committed())
        .source(KilnLogSourceDto.valueOf(result.source().name()))
        .presetUnverified(result.presetUnverified())
        .tempUnit(TemperatureUnitDto.valueOf(result.tempUnit().name()))
        .tempUnitInferred(result.tempUnitInferred())
        .startedAt(result.startedAt() == null ? null : result.startedAt().atOffset(ZoneOffset.UTC))
        .finishedAt(
            result.finishedAt() == null ? null : result.finishedAt().atOffset(ZoneOffset.UTC))
        .durationMinutes(result.durationMinutes())
        .peakTemp(result.peakTemp().doubleValue())
        .detectedCone(result.detectedCone())
        .errorCode(result.errorCode())
        .rowCount(result.rowCount())
        .zoneCount(result.zoneCount())
        .detectedColumns(result.detectedColumns())
        .warnings(result.warnings())
        .temperatureCurve(result.temperatureCurve().stream().map(KilnDtos::toPointDto).toList())
        .suggestedMatch(
            result.suggestedMatch() == null ? null : toSuggestedDto(result.suggestedMatch()))
        .firing(result.firing() == null ? null : toDto(result.firing(), now));
  }

  private static TemperaturePointDto toPointDto(TemperaturePoint point) {
    TemperaturePointDto dto =
        new TemperaturePointDto()
            .elapsedMinutes(point.elapsedMinutes())
            .temp(point.temp())
            .setpoint(point.setpoint());
    List<Double> zones = point.zoneTemps();
    if (zones != null) {
      dto.zoneTemps(zones);
    }
    return dto;
  }

  private static SuggestedFiringMatchDto toSuggestedDto(KilnLogImportService.SuggestedMatch match) {
    return new SuggestedFiringMatchDto()
        .firingId(match.firingId())
        .kind(FiringKindDto.valueOf(match.kind().name()))
        .cone(match.cone())
        .status(FiringStatusDto.valueOf(match.status().name()))
        .createdAt(match.createdAt().atOffset(ZoneOffset.UTC))
        .startedAt(match.startedAt() == null ? null : match.startedAt().atOffset(ZoneOffset.UTC))
        .finishedAt(match.finishedAt() == null ? null : match.finishedAt().atOffset(ZoneOffset.UTC))
        .overlapDescription(match.overlapDescription());
  }
}
