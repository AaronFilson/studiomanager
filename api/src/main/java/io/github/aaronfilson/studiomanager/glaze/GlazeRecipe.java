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
 *
 * The import-only columns ({@code source_ref}, {@code source_rev}, {@code source_updated_at},
 * {@code promoted_from}) aren't mapped: nothing writes them yet.
 */
@Entity
@Table(name = "glaze_recipe")
@EntityListeners(AuditingEntityListener.class)
class GlazeRecipe {

  private static final int FIRST_VERSION = 1;

  /** Where a recipe came from. Only typed-in recipes exist until the importers are built. */
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

  /**
   * A new, typed-in recipe from a checked draft.
   *
   * @param ownerAccountId the member who owns it, or null for a studio glaze
   */
  GlazeRecipe(
      GlazeBook.OwnerKind ownerKind,
      @Nullable UUID ownerAccountId,
      GlazeBook.Draft draft,
      UUID createdByAccountId,
      Instant now) {
    this.id = Uuids.v7();
    this.ownerKind = ownerKind;
    this.ownerAccountId = ownerAccountId;
    this.source = Source.MANUAL;
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

  int version() {
    return version;
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

  GlazeBook.Recipe toRecipe(@Nullable String ownerName, boolean canEdit) {
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
        lines.stream().map(GlazeLine::toLine).toList(),
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
      return stored == drafted;
    }
    return stored.compareTo(drafted) == 0;
  }
}
