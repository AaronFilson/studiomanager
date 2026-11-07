package io.github.aaronfilson.studiomanager.glaze;

import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reading the studio's glaze book. Row-level security keeps a member's unshared recipes from
 * everyone else, staff included, and lets only the owner change them. Studio glazes are staff-only
 * to change, which {@link GlazeEdits} checks.
 *
 * <p>Each list is a fixed number of queries however long it is: the recipes with their lines, then
 * one lookup of all their owners' names.
 */
@Service
public class GlazeBook {

  static final int MAX_LINES = 60;
  static final BigDecimal MAX_AMOUNT = new BigDecimal("100000");

  private final GlazeRecipeRepository recipes;
  private final AccountDirectory accounts;

  GlazeBook(GlazeRecipeRepository recipes, AccountDirectory accounts) {
    this.recipes = recipes;
    this.accounts = accounts;
  }

  public enum OwnerKind {
    STUDIO,
    MEMBER
  }

  public enum Filter {
    STUDIO,
    MINE,
    SHARED
  }

  public enum LineKind {
    BASE,
    ADDITION
  }

  public enum Unit {
    PARTS,
    PERCENT,
    GRAMS
  }

  public record Line(LineKind kind, String material, BigDecimal amount, Unit unit) {}

  /** What a recipe is made of, as typed or imported. */
  public record Draft(
      String name,
      @Nullable String notes,
      @Nullable String cone,
      @Nullable String atmosphere,
      boolean foodSurface,
      @Nullable BigDecimal targetSg,
      boolean sharedWithStudio,
      List<Line> lines) {}

  public record Recipe(
      UUID id,
      OwnerKind ownerKind,
      @Nullable UUID ownerAccountId,
      @Nullable String ownerName,
      String name,
      @Nullable String notes,
      @Nullable String cone,
      @Nullable String atmosphere,
      boolean foodSurface,
      @Nullable BigDecimal targetSg,
      boolean sharedWithStudio,
      String source,
      List<Line> lines,
      boolean canEdit,
      int version,
      Instant updatedAt) {}

  @Transactional(readOnly = true)
  public List<Recipe> list(Filter filter) {
    TenantContext caller = TenantContext.require();
    List<GlazeRecipe> found =
        switch (filter) {
          case STUDIO -> recipes.findByOwnerKindWithLines(OwnerKind.STUDIO);
          case MINE -> recipes.findOwnedByWithLines(caller.accountId());
          case SHARED -> recipes.findSharedByOthersWithLines(caller.accountId());
        };
    Map<UUID, String> ownerNames =
        accounts.displayNames(
            found.stream().map(GlazeRecipe::ownerAccountId).filter(Objects::nonNull).toList());
    return found.stream().map(recipe -> view(recipe, caller, ownerNames)).toList();
  }

  @Transactional(readOnly = true)
  public Recipe get(UUID id) {
    return view(find(id), TenantContext.require());
  }

  /** For {@link GlazeEdits}, which changes a recipe it has loaded. */
  GlazeRecipe find(UUID id) {
    return recipes.findWithLines(id).orElseThrow(() -> new NotFoundException("No such recipe."));
  }

  /** One recipe as the API returns it, to {@code caller}. */
  Recipe view(GlazeRecipe recipe, TenantContext caller) {
    UUID owner = recipe.ownerAccountId();
    Map<UUID, String> ownerNames = owner == null ? Map.of() : accounts.displayNames(List.of(owner));
    return view(recipe, caller, ownerNames);
  }

  /** Whether {@code caller} may change the recipe: staff for a studio glaze, else its owner. */
  static boolean canEdit(TenantContext caller, GlazeRecipe recipe) {
    return switch (recipe.ownerKind()) {
      case STUDIO -> isStaff(caller);
      case MEMBER -> caller.accountId().equals(recipe.ownerAccountId());
    };
  }

  static boolean isStaff(TenantContext caller) {
    StudioRole role = caller.role();
    return role != null && role.includes(StudioRole.STAFF);
  }

  private static Recipe view(
      GlazeRecipe recipe, TenantContext caller, Map<UUID, String> ownerNames) {
    UUID owner = recipe.ownerAccountId();
    return recipe.toRecipe(owner == null ? null : ownerNames.get(owner), canEdit(caller, recipe));
  }
}
