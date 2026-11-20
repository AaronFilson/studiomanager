package io.github.aaronfilson.studiomanager.glaze;

import io.github.aaronfilson.studiomanager.contract.AddGlazeMixReadingRequestDto;
import io.github.aaronfilson.studiomanager.contract.AtmosphereDto;
import io.github.aaronfilson.studiomanager.contract.GlazeAmountUnitDto;
import io.github.aaronfilson.studiomanager.contract.GlazeBatchDto;
import io.github.aaronfilson.studiomanager.contract.GlazeBatchLineDto;
import io.github.aaronfilson.studiomanager.contract.GlazeImportOutcomeDto;
import io.github.aaronfilson.studiomanager.contract.GlazeImportResultDto;
import io.github.aaronfilson.studiomanager.contract.GlazeImportRowDto;
import io.github.aaronfilson.studiomanager.contract.GlazeLineChemistryDto;
import io.github.aaronfilson.studiomanager.contract.GlazeLineDto;
import io.github.aaronfilson.studiomanager.contract.GlazeLineKindDto;
import io.github.aaronfilson.studiomanager.contract.GlazeListFilterDto;
import io.github.aaronfilson.studiomanager.contract.GlazeMixDto;
import io.github.aaronfilson.studiomanager.contract.GlazeMixReadingDto;
import io.github.aaronfilson.studiomanager.contract.GlazeOwnerKindDto;
import io.github.aaronfilson.studiomanager.contract.GlazePhotoDto;
import io.github.aaronfilson.studiomanager.contract.GlazeRecipeDto;
import io.github.aaronfilson.studiomanager.contract.GlazeSourceDto;
import io.github.aaronfilson.studiomanager.contract.GlazecalcCallbackRequestDto;
import io.github.aaronfilson.studiomanager.contract.GlazecalcConnectRequestDto;
import io.github.aaronfilson.studiomanager.contract.GlazecalcConnectResponseDto;
import io.github.aaronfilson.studiomanager.contract.GlazecalcStatusDto;
import io.github.aaronfilson.studiomanager.contract.GlazesApi;
import io.github.aaronfilson.studiomanager.contract.ImportGlazeShareRequestDto;
import io.github.aaronfilson.studiomanager.contract.LogGlazeMixRequestDto;
import io.github.aaronfilson.studiomanager.contract.PhotoUploadDto;
import io.github.aaronfilson.studiomanager.contract.SaveGlazeRequestDto;
import io.github.aaronfilson.studiomanager.contract.WeightUnitDto;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class GlazeController implements GlazesApi {

  private final GlazeBook book;
  private final GlazeEdits edits;
  private final GlazeMixService mixes;
  private final GlazePhotoService photos;
  private final GlazeImportService imports;
  private final GlazecalcService glazecalc;

  GlazeController(
      GlazeBook book,
      GlazeEdits edits,
      GlazeMixService mixes,
      GlazePhotoService photos,
      GlazeImportService imports,
      GlazecalcService glazecalc) {
    this.book = book;
    this.edits = edits;
    this.mixes = mixes;
    this.photos = photos;
    this.imports = imports;
    this.glazecalc = glazecalc;
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'GLAZES_READ')")
  public ResponseEntity<List<GlazeRecipeDto>> listGlazes(GlazeListFilterDto owner, UUID studioId) {
    return ResponseEntity.ok(
        book.list(GlazeBook.Filter.valueOf(owner.getValue())).stream()
            .map(GlazeController::dto)
            .toList());
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'GLAZES_OWN_EDIT')")
  public ResponseEntity<GlazeRecipeDto> createGlaze(UUID studioId, SaveGlazeRequestDto request) {
    GlazeBook.OwnerKind kind =
        request.getOwnerKind() == null
            ? GlazeBook.OwnerKind.MEMBER
            : GlazeBook.OwnerKind.valueOf(request.getOwnerKind().getValue());
    return ResponseEntity.status(HttpStatus.CREATED).body(dto(edits.create(kind, draft(request))));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'GLAZES_READ')")
  public ResponseEntity<GlazeRecipeDto> getGlaze(UUID studioId, UUID glazeId) {
    return ResponseEntity.ok(dto(book.get(glazeId)));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'GLAZES_OWN_EDIT')")
  public ResponseEntity<GlazeRecipeDto> updateGlaze(
      UUID studioId, UUID glazeId, SaveGlazeRequestDto request) {
    Integer version = request.getVersion();
    if (version == null) {
      throw new InvalidInputException("Send the version the change was made from.");
    }
    return ResponseEntity.ok(dto(edits.update(glazeId, version, draft(request))));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'GLAZES_OWN_EDIT')")
  public ResponseEntity<Void> deleteGlaze(UUID studioId, UUID glazeId) {
    edits.delete(glazeId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'GLAZES_READ')")
  public ResponseEntity<GlazeBatchDto> getGlazeBatch(
      String size, WeightUnitDto unit, UUID studioId, UUID glazeId) {
    var batch =
        BatchCalculator.scale(
            book.get(glazeId).lines(),
            new BigDecimal(size),
            BatchCalculator.WeightUnit.valueOf(unit.getValue()));
    return ResponseEntity.ok(
        new GlazeBatchDto()
            .size(batch.size().toPlainString())
            .unit(unit)
            .lines(
                batch.lines().stream()
                    .map(w ->
                        new GlazeBatchLineDto()
                            .kind(GlazeLineKindDto.fromValue(w.line().kind().name()))
                            .material(w.line().material())
                            .weight(w.weight().toPlainString()))
                    .toList())
            .total(batch.total().toPlainString()));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'GLAZES_OWN_EDIT')")
  public ResponseEntity<GlazeImportResultDto> importGlazes(
      UUID studioId, Map<String, Object> requestBody, @Nullable Boolean dryRun) {
    boolean previewOnly = dryRun == null || dryRun;
    return ResponseEntity.ok(importDto(imports.importMap(requestBody, previewOnly)));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'GLAZES_OWN_EDIT')")
  public ResponseEntity<GlazeImportResultDto> importGlazeShare(
      UUID studioId, ImportGlazeShareRequestDto request) {
    boolean previewOnly = request.getDryRun() == null || request.getDryRun();
    return ResponseEntity.ok(
        importDto(glazecalc.importShare(request.getShareUrlOrSecret(), previewOnly)));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'GLAZES_READ')")
  public ResponseEntity<GlazecalcStatusDto> getGlazecalcStatus(UUID studioId) {
    return ResponseEntity.ok(statusDto(glazecalc.status()));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'GLAZES_OWN_EDIT')")
  public ResponseEntity<GlazecalcConnectResponseDto> connectGlazecalc(
      UUID studioId, @Nullable GlazecalcConnectRequestDto request) {
    GlazecalcService.ConnectStart started =
        glazecalc.startConnect(request == null ? null : request.getRedirectUri());
    return ResponseEntity.ok(
        new GlazecalcConnectResponseDto()
            .authorizationUrl(started.authorizationUrl())
            .state(started.state()));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'GLAZES_OWN_EDIT')")
  public ResponseEntity<GlazecalcStatusDto> completeGlazecalcCallback(
      UUID studioId, GlazecalcCallbackRequestDto request) {
    return ResponseEntity.ok(
        statusDto(
            glazecalc.completeCallback(
                request.getCode(), request.getState(), request.getRedirectUri())));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'GLAZES_OWN_EDIT')")
  public ResponseEntity<GlazeImportResultDto> syncGlazecalc(
      UUID studioId, @Nullable Boolean dryRun) {
    boolean previewOnly = Boolean.TRUE.equals(dryRun);
    return ResponseEntity.ok(importDto(glazecalc.sync(previewOnly)));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'GLAZES_OWN_EDIT')")
  public ResponseEntity<Void> disconnectGlazecalc(UUID studioId) {
    glazecalc.disconnect();
    return ResponseEntity.noContent().build();
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'GLAZES_READ')")
  public ResponseEntity<Map<String, Object>> exportGlaze(UUID studioId, UUID glazeId) {
    return ResponseEntity.ok(imports.exportAsGlazecalc(glazeId));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'GLAZES_STUDIO_EDIT')")
  public ResponseEntity<GlazeRecipeDto> promoteGlaze(UUID studioId, UUID glazeId) {
    return ResponseEntity.status(HttpStatus.CREATED).body(dto(edits.promote(glazeId)));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'GLAZES_READ')")
  public ResponseEntity<List<GlazeMixDto>> listGlazeMixes(UUID studioId, UUID glazeId) {
    return ResponseEntity.ok(mixes.list(glazeId).stream().map(GlazeController::mixDto).toList());
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'GLAZES_STUDIO_EDIT')")
  public ResponseEntity<GlazeMixDto> logGlazeMix(
      UUID studioId, UUID glazeId, LogGlazeMixRequestDto request) {
    GlazeMixService.MixView logged =
        mixes.logMix(
            glazeId,
            new BigDecimal(request.getDryWeightG()),
            request.getWaterG() == null ? null : new BigDecimal(request.getWaterG()),
            request.getSpecificGravity() == null
                ? null
                : new BigDecimal(request.getSpecificGravity()),
            request.getNotes());
    return ResponseEntity.status(HttpStatus.CREATED).body(mixDto(logged));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'GLAZES_STUDIO_EDIT')")
  public ResponseEntity<GlazeMixReadingDto> addGlazeMixReading(
      UUID studioId, UUID glazeId, UUID mixId, AddGlazeMixReadingRequestDto request) {
    GlazeMixService.ReadingView reading =
        mixes.addReading(
            glazeId, mixId, new BigDecimal(request.getSpecificGravity()), request.getNote());
    return ResponseEntity.status(HttpStatus.CREATED).body(readingDto(reading));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'GLAZES_OWN_EDIT')")
  public ResponseEntity<PhotoUploadDto> startGlazePhotoUpload(UUID studioId, UUID glazeId) {
    GlazePhotoService.PhotoUpload upload = photos.startUpload(glazeId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new PhotoUploadDto()
                .photoId(upload.photoId())
                .photoUploadUrl(upload.photoUploadUrl())
                .thumbnailUploadUrl(upload.thumbnailUploadUrl())
                .expiresAt(utc(upload.expiresAt()))
                .maxPhotoBytes(upload.maxPhotoBytes())
                .maxThumbnailBytes(upload.maxThumbnailBytes()));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'GLAZES_OWN_EDIT')")
  public ResponseEntity<GlazeRecipeDto> completeGlazePhotoUpload(
      UUID studioId, UUID glazeId, UUID photoId) {
    return ResponseEntity.ok(dto(photos.complete(glazeId, photoId)));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'GLAZES_OWN_EDIT')")
  public ResponseEntity<Void> deleteGlazePhoto(UUID studioId, UUID glazeId, UUID photoId) {
    photos.delete(glazeId, photoId);
    return ResponseEntity.noContent().build();
  }

  private static GlazeBook.Draft draft(SaveGlazeRequestDto request) {
    return new GlazeBook.Draft(
        request.getName(),
        request.getNotes(),
        request.getCone(),
        request.getAtmosphere() == null ? null : request.getAtmosphere().getValue(),
        Boolean.TRUE.equals(request.getFoodSurface()),
        request.getTargetSpecificGravity() == null
            ? null
            : new BigDecimal(request.getTargetSpecificGravity()),
        Boolean.TRUE.equals(request.getSharedWithStudio()),
        request.getLines().stream()
            .map(line ->
                new GlazeBook.Line(
                    GlazeBook.LineKind.valueOf(line.getKind().getValue()),
                    line.getMaterial(),
                    new BigDecimal(line.getAmount()),
                    line.getAmountText(),
                    GlazeBook.Unit.valueOf(line.getUnit().getValue()),
                    fromChemistryDto(line.getChemistry())))
            .toList());
  }

  private static GlazeBook.@Nullable Chemistry fromChemistryDto(
      @Nullable GlazeLineChemistryDto dto) {
    if (dto == null) {
      return null;
    }
    Map<String, Double> analysis = new LinkedHashMap<>();
    if (dto.getAnalysis() != null) {
      for (var entry : dto.getAnalysis().entrySet()) {
        Double num = entry.getValue();
        if (num != null && Double.isFinite(num) && num >= 0) {
          analysis.put(entry.getKey(), num);
        }
      }
    }
    Double loi = dto.getLoi();
    if (loi != null && (!Double.isFinite(loi) || loi < 0)) {
      loi = null;
    }
    if (dto.getMaterialId() == null && analysis.isEmpty() && loi == null) {
      return null;
    }
    return new GlazeBook.Chemistry(dto.getMaterialId(), analysis, loi);
  }

  private static @Nullable GlazeLineChemistryDto toChemistryDto(
      GlazeBook.@Nullable Chemistry chemistry) {
    if (chemistry == null) {
      return null;
    }
    GlazeLineChemistryDto dto =
        new GlazeLineChemistryDto().materialId(chemistry.materialId()).loi(chemistry.loi());
    if (!chemistry.analysis().isEmpty()) {
      dto.analysis(new LinkedHashMap<>(chemistry.analysis()));
    }
    return dto;
  }

  static GlazeRecipeDto dto(GlazeBook.Recipe recipe) {
    return new GlazeRecipeDto()
        .id(recipe.id())
        .ownerKind(GlazeOwnerKindDto.fromValue(recipe.ownerKind().name()))
        .ownerAccountId(recipe.ownerAccountId())
        .ownerName(recipe.ownerName())
        .name(recipe.name())
        .notes(recipe.notes())
        .cone(recipe.cone())
        .atmosphere(
            recipe.atmosphere() == null ? null : AtmosphereDto.fromValue(recipe.atmosphere()))
        .foodSurface(recipe.foodSurface())
        .targetSpecificGravity(plain(recipe.targetSg()))
        .sharedWithStudio(recipe.sharedWithStudio())
        .source(GlazeSourceDto.fromValue(recipe.source()))
        .promotedFrom(recipe.promotedFrom())
        .lines(
            recipe.lines().stream()
                .map(line ->
                    new GlazeLineDto()
                        .kind(GlazeLineKindDto.fromValue(line.kind().name()))
                        .material(line.material())
                        .amount(line.amount().toPlainString())
                        .amountText(line.amountText())
                        .unit(GlazeAmountUnitDto.fromValue(line.unit().name()))
                        .chemistry(toChemistryDto(line.chemistry())))
                .toList())
        .photos(
            recipe.photos().stream()
                .map(photo ->
                    new GlazePhotoDto()
                        .id(photo.id())
                        .url(photo.url())
                        .thumbnailUrl(photo.thumbnailUrl())
                        .uploadedByAccountId(photo.uploadedByAccountId())
                        .uploadedAt(utc(photo.uploadedAt())))
                .toList())
        .canEdit(recipe.canEdit())
        .version(recipe.version())
        .updatedAt(utc(recipe.updatedAt()));
  }

  private static GlazeMixDto mixDto(GlazeMixService.MixView mix) {
    return new GlazeMixDto()
        .id(mix.id())
        .recipeId(mix.recipeId())
        .mixedByAccountId(mix.mixedByAccountId())
        .mixedByName(mix.mixedByName())
        .mixedAt(utc(mix.mixedAt()))
        .dryWeightG(mix.dryWeightG().toPlainString())
        .waterG(plain(mix.waterG()))
        .notes(mix.notes())
        .readings(mix.readings().stream().map(GlazeController::readingDto).toList());
  }

  private static GlazeMixReadingDto readingDto(GlazeMixService.ReadingView reading) {
    return new GlazeMixReadingDto()
        .id(reading.id())
        .readByAccountId(reading.readByAccountId())
        .readByName(reading.readByName())
        .readAt(utc(reading.readAt()))
        .specificGravity(reading.specificGravity().toPlainString())
        .note(reading.note())
        .offTarget(reading.offTarget())
        .warning(reading.warning());
  }

  private static GlazeImportResultDto importDto(GlazeImportService.Result result) {
    return new GlazeImportResultDto()
        .format(result.format())
        .committed(result.committed())
        .createdCount(result.createdCount())
        .updatedCount(result.updatedCount())
        .unchangedCount(result.unchangedCount())
        .errorCount(result.errorCount())
        .rows(
            result.rows().stream()
                .map(row ->
                    new GlazeImportRowDto()
                        .index(row.index())
                        .name(row.name())
                        .sourceRef(row.sourceRef())
                        .outcome(GlazeImportOutcomeDto.fromValue(row.outcome().name()))
                        .recipeId(row.recipeId())
                        .message(row.message())
                        .warnings(row.warnings()))
                .toList());
  }

  private static GlazecalcStatusDto statusDto(GlazecalcService.Status status) {
    return new GlazecalcStatusDto()
        .configured(status.configured())
        .connected(status.connected())
        .baseUrl(status.baseUrl())
        .connectedAt(utcOrNull(status.connectedAt()))
        .lastSyncedAt(utcOrNull(status.lastSyncedAt()))
        .expiresAt(utcOrNull(status.expiresAt()))
        .remoteAccountLabel(status.remoteAccountLabel());
  }

  private static @Nullable String plain(@Nullable BigDecimal value) {
    return value == null ? null : value.toPlainString();
  }

  private static OffsetDateTime utc(Instant instant) {
    return instant.atOffset(ZoneOffset.UTC);
  }

  private static @Nullable OffsetDateTime utcOrNull(@Nullable Instant instant) {
    return instant == null ? null : utc(instant);
  }
}
