package io.github.aaronfilson.studiomanager.glaze;

import io.github.aaronfilson.studiomanager.glaze.GlazeBook.Draft;
import io.github.aaronfilson.studiomanager.glaze.GlazeBook.Line;
import io.github.aaronfilson.studiomanager.glaze.GlazeBook.OwnerKind;
import io.github.aaronfilson.studiomanager.glaze.GlazeBook.Recipe;
import io.github.aaronfilson.studiomanager.glaze.GlazeBook.Unit;
import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Adding, changing and deleting recipes. Reading them is {@link GlazeBook}. */
@Service
class GlazeEdits {

  private static final BigDecimal MIN_SPECIFIC_GRAVITY = BigDecimal.ONE;
  private static final BigDecimal MAX_SPECIFIC_GRAVITY = new BigDecimal("2.5");

  private final GlazeRecipeRepository recipes;
  private final GlazeBook book;
  private final EntityManager entityManager;
  private final Clock clock;

  GlazeEdits(
      GlazeRecipeRepository recipes, GlazeBook book, EntityManager entityManager, Clock clock) {
    this.recipes = recipes;
    this.book = book;
    this.entityManager = entityManager;
    this.clock = clock;
  }

  @Transactional
  public Recipe create(OwnerKind ownerKind, Draft draft) {
    TenantContext caller = TenantContext.require();
    UUID ownerAccountId =
        switch (ownerKind) {
          case STUDIO -> {
            if (!GlazeBook.isStaff(caller)) {
              throw new AccessDeniedException("Only staff can add a studio glaze.");
            }
            yield null;
          }
          case MEMBER -> caller.accountId();
        };
    GlazeRecipe recipe =
        new GlazeRecipe(
            ownerKind,
            ownerAccountId,
            check(ownerKind, draft),
            caller.accountId(),
            clock.instant());
    // Persisted, not saved: a recipe starts at version 1 (see GlazeRecipe)
    entityManager.persist(recipe);
    entityManager.flush();
    return book.view(recipe, caller);
  }

  @Transactional
  public Recipe update(UUID id, int expectedVersion, Draft draft) {
    TenantContext caller = TenantContext.require();
    GlazeRecipe recipe = book.find(id);
    requireCanEdit(caller, recipe);
    Draft clean = check(recipe.ownerKind(), draft);
    if (recipe.version() != expectedVersion) {
      throw ConflictException.staleVersion(
          "Someone else changed this recipe. Reload it and try again.");
    }
    recipe.revise(clean, caller.accountId(), clock.instant());
    recipes.flush();
    return book.view(recipe, caller);
  }

  @Transactional
  public void delete(UUID id) {
    GlazeRecipe recipe = book.find(id);
    requireCanEdit(TenantContext.require(), recipe);
    recipes.delete(recipe);
  }

  /** Trims and validates a draft, with messages a potter can act on. */
  static Draft check(OwnerKind ownerKind, Draft draft) {
    String name = strip(draft.name());
    if (name == null || name.length() > 120) {
      throw new InvalidInputException("Give the recipe a name of up to 120 characters.");
    }
    String notes = strip(draft.notes());
    if (notes != null && notes.length() > 4000) {
      throw new InvalidInputException("Keep the notes to 4,000 characters.");
    }
    String cone = strip(draft.cone());
    if (cone != null && cone.length() > 10) {
      throw new InvalidInputException("Write the cone short, such as \"6\" or \"04\".");
    }
    BigDecimal specificGravity = draft.targetSg();
    if (specificGravity != null
        && (specificGravity.compareTo(MIN_SPECIFIC_GRAVITY) < 0
            || specificGravity.compareTo(MAX_SPECIFIC_GRAVITY) > 0)) {
      throw new InvalidInputException("A specific gravity is between 1 and 2.5.");
    }
    boolean shared =
        switch (ownerKind) {
          case STUDIO -> {
            if (draft.sharedWithStudio()) {
              throw new InvalidInputException("A studio glaze is already shared with everyone.");
            }
            yield false;
          }
          case MEMBER -> draft.sharedWithStudio();
        };
    if (draft.lines().isEmpty() || draft.lines().size() > GlazeBook.MAX_LINES) {
      throw new InvalidInputException(
          "A recipe has between 1 and %d lines.".formatted(GlazeBook.MAX_LINES));
    }
    List<Line> lines = new ArrayList<>();
    for (Line line : draft.lines()) {
      lines.add(checkLine(line));
    }
    return new Draft(
        name,
        notes,
        cone,
        draft.atmosphere(),
        draft.foodSurface(),
        specificGravity == null ? null : specificGravity.stripTrailingZeros(),
        shared,
        List.copyOf(lines));
  }

  private static Line checkLine(Line line) {
    String material = strip(line.material());
    if (material == null || material.length() > 80) {
      throw new InvalidInputException("Every line needs a material, of up to 80 characters.");
    }
    if (line.amount().signum() <= 0 || line.amount().compareTo(GlazeBook.MAX_AMOUNT) > 0) {
      throw new InvalidInputException("The amount of %s must be more than 0.".formatted(material));
    }
    if (line.amount().scale() > 4) {
      throw new InvalidInputException("Amounts can have up to 4 decimal places.");
    }
    boolean unitAllowed =
        switch (line.kind()) {
          case BASE -> line.unit() == Unit.PARTS;
          case ADDITION -> true;
        };
    if (!unitAllowed) {
      throw new InvalidInputException(
          "Base lines are in parts. Only additions can be a percent or grams.");
    }
    return new Line(line.kind(), material, line.amount(), line.unit());
  }

  private static void requireCanEdit(TenantContext caller, GlazeRecipe recipe) {
    if (GlazeBook.canEdit(caller, recipe)) {
      return;
    }
    throw new AccessDeniedException(
        switch (recipe.ownerKind()) {
          case STUDIO -> "Only staff can change a studio glaze.";
          case MEMBER -> "Only its owner can change a member's recipe.";
        });
  }

  private static @Nullable String strip(@Nullable String text) {
    return text == null || text.isBlank() ? null : text.strip();
  }
}
