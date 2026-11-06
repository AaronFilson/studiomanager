package io.github.aaronfilson.studiomanager.glaze;

import io.github.aaronfilson.studiomanager.contract.AtmosphereDto;
import io.github.aaronfilson.studiomanager.contract.GlazeAmountUnitDto;
import io.github.aaronfilson.studiomanager.contract.GlazeBatchDto;
import io.github.aaronfilson.studiomanager.contract.GlazeBatchLineDto;
import io.github.aaronfilson.studiomanager.contract.GlazeLineDto;
import io.github.aaronfilson.studiomanager.contract.GlazeLineKindDto;
import io.github.aaronfilson.studiomanager.contract.GlazeListFilterDto;
import io.github.aaronfilson.studiomanager.contract.GlazeOwnerKindDto;
import io.github.aaronfilson.studiomanager.contract.GlazeRecipeDto;
import io.github.aaronfilson.studiomanager.contract.GlazeSourceDto;
import io.github.aaronfilson.studiomanager.contract.GlazesApi;
import io.github.aaronfilson.studiomanager.contract.SaveGlazeRequestDto;
import io.github.aaronfilson.studiomanager.contract.WeightUnitDto;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class GlazeController implements GlazesApi {

  private final GlazeBook book;

  GlazeController(GlazeBook book) {
    this.book = book;
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'MEMBER')")
  public ResponseEntity<List<GlazeRecipeDto>> listGlazes(GlazeListFilterDto owner, UUID studioId) {
    return ResponseEntity.ok(
        book.list(GlazeBook.Filter.valueOf(owner.getValue())).stream()
            .map(GlazeController::dto)
            .toList());
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'MEMBER')")
  public ResponseEntity<GlazeRecipeDto> createGlaze(UUID studioId, SaveGlazeRequestDto request) {
    GlazeBook.OwnerKind kind =
        request.getOwnerKind() == null
            ? GlazeBook.OwnerKind.MEMBER
            : GlazeBook.OwnerKind.valueOf(request.getOwnerKind().getValue());
    return ResponseEntity.status(HttpStatus.CREATED).body(dto(book.create(kind, draft(request))));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'MEMBER')")
  public ResponseEntity<GlazeRecipeDto> getGlaze(UUID studioId, UUID glazeId) {
    return ResponseEntity.ok(dto(book.get(glazeId)));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'MEMBER')")
  public ResponseEntity<GlazeRecipeDto> updateGlaze(
      UUID studioId, UUID glazeId, SaveGlazeRequestDto request) {
    if (request.getVersion() == null) {
      throw new InvalidInputException("Send the version the change was made from.");
    }
    return ResponseEntity.ok(dto(book.update(glazeId, request.getVersion(), draft(request))));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'MEMBER')")
  public ResponseEntity<Void> deleteGlaze(UUID studioId, UUID glazeId) {
    book.delete(glazeId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'MEMBER')")
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
                    GlazeBook.Unit.valueOf(line.getUnit().getValue())))
            .toList());
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
        .lines(
            recipe.lines().stream()
                .map(line ->
                    new GlazeLineDto()
                        .kind(GlazeLineKindDto.fromValue(line.kind().name()))
                        .material(line.material())
                        .amount(line.amount().toPlainString())
                        .unit(GlazeAmountUnitDto.fromValue(line.unit().name())))
                .toList())
        .canEdit(recipe.canEdit())
        .version(recipe.version())
        .updatedAt(recipe.updatedAt().atOffset(ZoneOffset.UTC));
  }

  private static @Nullable String plain(@Nullable BigDecimal value) {
    return value == null ? null : value.toPlainString();
  }
}
