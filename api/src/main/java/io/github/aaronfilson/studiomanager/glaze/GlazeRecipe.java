package io.github.aaronfilson.studiomanager.glaze;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A recipe with its lines, which are part of it: they're saved, replaced and deleted with it.
 *
 * <p>Two things differ from the usual entity (ADR 21):
 *
 * <ul>
 *   <li>The version is an {@code integer} that starts at 1, as the column's default does and as
 *       clients already expect, so a new recipe is persisted with {@code EntityManager.persist}
 *       rather than {@code save} (Spring Data would take a set version to mean a saved row).
 *   <li>{@code updated_by} and {@code updated_at} are set by {@link #revise}, not by auditing, so
 *       that a change to the lines alone also counts as a change to the recipe and bumps its
 *       version.
 * </ul>
 */
@SuppressWarnings("UnusedVariable")
@Entity
@Table(name = "glaze_recipe")
@EntityListeners(AuditingEntityListener.class)
class GlazeRecipe {

  private static final int FIRST_VERSION = 1;

  /** Where a recipe came from. */
  enum Source {
    MANUAL,
    GLAZECALC,
    GLAZE_VAULT
  }

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  @Enumerated(EnumType.STRING)
  private GlazeBook.OwnerKind ownerKind;

  private @Nullable UUID ownerAccountId;

  private String name;

  private @Nullable String notes;

  private @Nullable String cone;

  private @Nullable String atmosphere;

  private boolean foodSurface;

  private @Nullable BigDecimal targetSg;

  private boolean sharedWithStudio;

  @Enumerated(EnumType.STRING)
  private Source source;

  private @Nullable String sourceRef;

  private @Nullable Integer sourceRev;

  private @Nullable Instant sourceUpdatedAt;

  private @Nullable UUID promotedFrom;

  @CreatedBy private @Nullable UUID createdBy;

  @CreatedDate private @Nullable Instant createdAt;

  private UUID updatedBy;

  private Instant updatedAt;

  @Version private int version;

  @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("position")
  private List<GlazeLine> lines = new ArrayList<>();

  @SuppressWarnings("NullAway.Init")
  protected GlazeRecipe() {}

  /** A new, typed-in recipe from a checked draft. */
  GlazeRecipe(
      GlazeBook.OwnerKind ownerKind,
      @Nullable UUID ownerAccountId,
      GlazeBook.Draft draft,
      UUID createdByAccountId,
      Instant now) {
    this(
        ownerKind,
        ownerAccountId,
        draft,
        Source.MANUAL,
        null,
        null,
        null,
        null,
        createdByAccountId,
        now);
  }

  /** A new imported or promoted recipe from a checked draft. */
  GlazeRecipe(
      GlazeBook.OwnerKind ownerKind,
      @Nullable UUID ownerAccountId,
      GlazeBook.Draft draft,
      Source source,
      @Nullable String sourceRef,
      @Nullable Integer sourceRev,
      @Nullable Instant sourceUpdatedAt,
      @Nullable UUID promotedFrom,
      UUID createdByAccountId,
      Instant now) {
    this.id = Uuids.v7();
    this.ownerKind = ownerKind;
    this.ownerAccountId = ownerAccountId;
    this.source = source;
    this.sourceRef = sourceRef;
    this.sourceRev = sourceRev;
    this.sourceUpdatedAt = sourceUpdatedAt;
    this.promotedFrom = promotedFrom;
    this.version = FIRST_VERSION;
    apply(draft);
    this.updatedBy = createdByAccountId;
    this.updatedAt = now;
  }

  UUID id() {
    return id;
  }

  GlazeBook.OwnerKind ownerKind() {
    return ownerKind;
  }

  @Nullable UUID ownerAccountId() {
    return ownerAccountId;
  }

  String name() {
    return name;
  }

  @Nullable String notes() {
    return notes;
  }

  @Nullable String cone() {
    return cone;
  }

  @Nullable String atmosphere() {
    return atmosphere;
  }

  boolean foodSurface() {
    return foodSurface;
  }

  @Nullable BigDecimal targetSg() {
    return targetSg;
  }

  boolean sharedWithStudio() {
    return sharedWithStudio;
  }

  Source source() {
    return source;
  }

  @Nullable String sourceRef() {
    return sourceRef;
  }

  @Nullable Integer sourceRev() {
    return sourceRev;
  }

  @Nullable Instant sourceUpdatedAt() {
    return sourceUpdatedAt;
  }

  @Nullable UUID promotedFrom() {
    return promotedFrom;
  }

  Instant updatedAt() {
    return updatedAt;
  }

  int version() {
    return version;
  }

  List<GlazeBook.Line> lineViews() {
    return lines.stream().map(GlazeLine::toLine).toList();
  }

  /**
   * Applies a checked draft. If anything differs, including only the lines, records who changed it
   * and when, which also makes Hibernate write the recipe and bump its version. A draft that
   * changes nothing writes nothing.
   */
  void revise(GlazeBook.Draft draft, UUID changedByAccountId, Instant now) {
    if (matches(draft)) {
      return;
    }
    apply(draft);
    this.updatedBy = changedByAccountId;
    this.updatedAt = now;
  }

  /** Updates an imported recipe from a newer export, preserving its existing sharing preference. */
  void applyImport(
      GlazeBook.Draft draft,
      @Nullable Integer newSourceRev,
      @Nullable Instant newSourceUpdatedAt,
      UUID changedByAccountId,
      Instant now) {
    GlazeBook.Draft withShared =
        new GlazeBook.Draft(
            draft.name(),
            draft.notes(),
            draft.cone(),
            draft.atmosphere(),
            draft.foodSurface(),
            draft.targetSg(),
            this.sharedWithStudio,
            draft.lines());
    apply(withShared);
    this.sourceRev = newSourceRev;
    this.sourceUpdatedAt = newSourceUpdatedAt;
    this.updatedBy = changedByAccountId;
    this.updatedAt = now;
  }

  boolean matchesImportContent(GlazeBook.Draft draft) {
    GlazeBook.Draft withShared =
        new GlazeBook.Draft(
            draft.name(),
            draft.notes(),
            draft.cone(),
            draft.atmosphere(),
            draft.foodSurface(),
            draft.targetSg(),
            this.sharedWithStudio,
            draft.lines());
    return matches(withShared);
  }

  GlazeBook.Recipe toRecipe(
      @Nullable String ownerName, boolean canEdit, List<GlazeBook.Photo> photos) {
    return new GlazeBook.Recipe(
        id,
        ownerKind,
        ownerAccountId,
        ownerName,
        name,
        notes,
        cone,
        atmosphere,
        foodSurface,
        targetSg == null ? null : targetSg.stripTrailingZeros(),
        sharedWithStudio,
        source.name(),
        promotedFrom,
        lineViews(),
        List.copyOf(photos),
        canEdit,
        version,
        updatedAt);
  }

  private boolean matches(GlazeBook.Draft draft) {
    return name.equals(draft.name())
        && Objects.equals(notes, draft.notes())
        && Objects.equals(cone, draft.cone())
        && Objects.equals(atmosphere, draft.atmosphere())
        && foodSurface == draft.foodSurface()
        && sameValue(targetSg, draft.targetSg())
        && sharedWithStudio == draft.sharedWithStudio()
        && linesMatch(draft.lines());
  }

  private boolean linesMatch(List<GlazeBook.Line> draftLines) {
    if (lines.size() != draftLines.size()) {
      return false;
    }
    for (int position = 0; position < draftLines.size(); position++) {
      if (!lines.get(position).holds(draftLines.get(position))) {
        return false;
      }
    }
    return true;
  }

  private void apply(GlazeBook.Draft draft) {
    this.name = draft.name();
    this.notes = draft.notes();
    this.cone = draft.cone();
    this.atmosphere = draft.atmosphere();
    this.foodSurface = draft.foodSurface();
    this.targetSg = draft.targetSg();
    this.sharedWithStudio = draft.sharedWithStudio();
    replaceLines(draft.lines());
  }

  /**
   * Rewrites the lines position by position: the same positions are updated in place, new ones
   * added and extra ones removed, so the unique {@code (recipe_id, position)} is never broken
   * mid-flush.
   */
  private void replaceLines(List<GlazeBook.Line> draftLines) {
    for (int position = 0; position < draftLines.size(); position++) {
      GlazeBook.Line line = draftLines.get(position);
      if (position < lines.size()) {
        lines.get(position).change(line);
      } else {
        lines.add(new GlazeLine(this, position, line));
      }
    }
    while (lines.size() > draftLines.size()) {
      lines.removeLast();
    }
  }

  private static boolean sameValue(@Nullable BigDecimal stored, @Nullable BigDecimal drafted) {
    if (stored == null || drafted == null) {
      return stored == null && drafted == null;
    }
    return stored.compareTo(drafted) == 0;
  }
}
