package io.github.aaronfilson.studiomanager.glaze;

import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The studio's glaze book. Row-level security keeps a member's unshared recipes from everyone else,
 * staff included, and lets only the owner change them. Studio glazes are staff-only to change,
 * which is checked here.
 */
@Service
public class GlazeBook {

  static final int MAX_LINES = 60;
  static final BigDecimal MAX_AMOUNT = new BigDecimal("100000");

  private final JdbcClient jdbc;

  GlazeBook(JdbcClient jdbc) {
    this.jdbc = jdbc;
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
    String where =
        switch (filter) {
          case STUDIO -> "r.owner_kind = 'STUDIO'";
          case MINE -> "r.owner_account_id = :me";
          case SHARED ->
              "r.owner_kind = 'MEMBER' and r.shared_with_studio and r.owner_account_id <> :me";
        };
    List<Recipe> recipes =
        jdbc.sql(SELECT_RECIPE + " where " + where + " order by lower(r.name), r.id")
            .param("me", caller.accountId())
            .query((rs, row) -> recipe(rs, caller))
            .list();
    Map<UUID, List<Line>> lines = lines(recipes.stream().map(Recipe::id).toList());
    return recipes.stream().map(r -> withLines(r, lines.getOrDefault(r.id(), List.of()))).toList();
  }

  @Transactional(readOnly = true)
  public Recipe get(UUID id) {
    TenantContext caller = TenantContext.require();
    Recipe recipe =
        jdbc.sql(SELECT_RECIPE + " where r.id = :id")
            .param("id", id)
            .query((rs, row) -> recipe(rs, caller))
            .optional()
            .orElseThrow(() -> new NotFoundException("No such recipe."));
    return withLines(recipe, lines(List.of(id)).getOrDefault(id, List.of()));
  }

  @Transactional
  public Recipe create(OwnerKind ownerKind, Draft draft) {
    TenantContext caller = TenantContext.require();
    if (ownerKind == OwnerKind.STUDIO) {
      requireStaff(caller);
    }
    UUID id = insert(caller, ownerKind, check(ownerKind, draft), "MANUAL", null, null, null);
    return get(id);
  }

  @Transactional
  public Recipe update(UUID id, int version, Draft draft) {
    TenantContext caller = TenantContext.require();
    Recipe current = get(id);
    requireCanEdit(current);
    Draft clean = check(current.ownerKind(), draft);
    int changed =
        jdbc.sql(
                """
                update glaze_recipe set name = :name, notes = :notes, cone = :cone, atmosphere = :atmosphere,
                       food_surface = :food, target_sg = :sg, shared_with_studio = :shared,
                       version = version + 1, updated_by = :by, updated_at = :now
                where id = :id and version = :version
                """)
            .param("name", clean.name())
            .param("notes", clean.notes())
            .param("cone", clean.cone())
            .param("atmosphere", clean.atmosphere())
            .param("food", clean.foodSurface())
            .param("sg", clean.targetSg())
            .param("shared", clean.sharedWithStudio())
            .param("by", caller.accountId())
            .param("now", now().atOffset(ZoneOffset.UTC))
            .param("id", id)
            .param("version", version)
            .update();
    if (changed == 0) {
      throw new ConflictException("Someone else changed this recipe. Reload it and try again.");
    }
    jdbc.sql("delete from glaze_line where recipe_id = :id").param("id", id).update();
    insertLines(caller.studioId(), id, clean.lines());
    return get(id);
  }

  @Transactional
  public void delete(UUID id) {
    requireCanEdit(get(id));
    jdbc.sql("delete from glaze_recipe where id = :id").param("id", id).update();
  }

  /** Adds a recipe as given; the caller has already checked it. Returns its id. */
  UUID insert(
      TenantContext caller,
      OwnerKind ownerKind,
      Draft draft,
      String source,
      @Nullable String sourceRef,
      @Nullable Integer sourceRev,
      @Nullable Instant sourceUpdatedAt) {
    UUID id = Uuids.v7();
    OffsetDateTime now = now().atOffset(ZoneOffset.UTC);
    jdbc.sql(
            """
            insert into glaze_recipe (id, studio_id, owner_kind, owner_account_id, name, notes, cone, atmosphere,
                   food_surface, target_sg, shared_with_studio, source, source_ref, source_rev, source_updated_at,
                   created_by, created_at, updated_by, updated_at)
            values (:id, :studioId, :kind, :owner, :name, :notes, :cone, :atmosphere, :food, :sg, :shared,
                    :source, :ref, :rev, :sourceAt, :by, :now, :by, :now)
            """)
        .param("id", id)
        .param("studioId", caller.studioId())
        .param("kind", ownerKind.name())
        .param("owner", ownerKind == OwnerKind.MEMBER ? caller.accountId() : null)
        .param("name", draft.name())
        .param("notes", draft.notes())
        .param("cone", draft.cone())
        .param("atmosphere", draft.atmosphere())
        .param("food", draft.foodSurface())
        .param("sg", draft.targetSg())
        .param("shared", draft.sharedWithStudio())
        .param("source", source)
        .param("ref", sourceRef)
        .param("rev", sourceRev)
        .param(
            "sourceAt", sourceUpdatedAt == null ? null : sourceUpdatedAt.atOffset(ZoneOffset.UTC))
        .param("by", caller.accountId())
        .param("now", now)
        .update();
    insertLines(caller.studioId(), id, draft.lines());
    return id;
  }

  private void insertLines(UUID studioId, UUID recipeId, List<Line> lines) {
    for (int i = 0; i < lines.size(); i++) {
      Line line = lines.get(i);
      jdbc.sql(
              """
              insert into glaze_line (id, studio_id, recipe_id, position, kind, material, amount, unit)
              values (:id, :studioId, :recipeId, :position, :kind, :material, :amount, :unit)
              """)
          .param("id", Uuids.v7())
          .param("studioId", studioId)
          .param("recipeId", recipeId)
          .param("position", i)
          .param("kind", line.kind().name())
          .param("material", line.material())
          .param("amount", line.amount())
          .param("unit", line.unit().name())
          .update();
    }
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
    BigDecimal sg = draft.targetSg();
    if (sg != null
        && (sg.compareTo(BigDecimal.ONE) < 0 || sg.compareTo(new BigDecimal("2.5")) > 0)) {
      throw new InvalidInputException("A specific gravity is between 1 and 2.5.");
    }
    if (draft.sharedWithStudio() && ownerKind == OwnerKind.STUDIO) {
      throw new InvalidInputException("A studio glaze is already shared with everyone.");
    }
    if (draft.lines().isEmpty() || draft.lines().size() > MAX_LINES) {
      throw new InvalidInputException("A recipe has between 1 and %d lines.".formatted(MAX_LINES));
    }
    List<Line> lines = new ArrayList<>();
    for (Line line : draft.lines()) {
      String material = strip(line.material());
      if (material == null || material.length() > 80) {
        throw new InvalidInputException("Every line needs a material, of up to 80 characters.");
      }
      if (line.amount().signum() <= 0 || line.amount().compareTo(MAX_AMOUNT) > 0) {
        throw new InvalidInputException(
            "The amount of %s must be more than 0.".formatted(material));
      }
      if (line.amount().scale() > 4) {
        throw new InvalidInputException("Amounts can have up to 4 decimal places.");
      }
      if (line.kind() == LineKind.BASE && line.unit() != Unit.PARTS) {
        throw new InvalidInputException(
            "Base lines are in parts. Only additions can be a percent or grams.");
      }
      lines.add(new Line(line.kind(), material, line.amount(), line.unit()));
    }
    return new Draft(
        name,
        notes,
        cone,
        draft.atmosphere(),
        draft.foodSurface(),
        sg == null ? null : sg.stripTrailingZeros(),
        draft.sharedWithStudio(),
        List.copyOf(lines));
  }

  private static void requireCanEdit(Recipe recipe) {
    if (!recipe.canEdit()) {
      throw new AccessDeniedException(
          recipe.ownerKind() == OwnerKind.STUDIO
              ? "Only staff can change a studio glaze."
              : "Only its owner can change a member's recipe.");
    }
  }

  private static void requireStaff(TenantContext caller) {
    if (caller.role() == null || !caller.role().includes(StudioRole.STAFF)) {
      throw new AccessDeniedException("Only staff can add a studio glaze.");
    }
  }

  private static boolean canEdit(TenantContext caller, OwnerKind kind, @Nullable UUID owner) {
    return kind == OwnerKind.STUDIO
        ? caller.role() != null && caller.role().includes(StudioRole.STAFF)
        : caller.accountId().equals(owner);
  }

  private Map<UUID, List<Line>> lines(List<UUID> recipeIds) {
    Map<UUID, List<Line>> byRecipe = new LinkedHashMap<>();
    if (recipeIds.isEmpty()) {
      return byRecipe;
    }
    jdbc.sql(
            """
            select recipe_id, kind, material, amount, unit from glaze_line
            where recipe_id = any(:ids) order by recipe_id, position
            """)
        .param("ids", recipeIds.toArray(UUID[]::new))
        .query(rs -> {
          byRecipe
              .computeIfAbsent(rs.getObject("recipe_id", UUID.class), k -> new ArrayList<>())
              .add(
                  new Line(
                      LineKind.valueOf(rs.getString("kind")),
                      rs.getString("material"),
                      rs.getBigDecimal("amount").stripTrailingZeros(),
                      Unit.valueOf(rs.getString("unit"))));
        });
    return byRecipe;
  }

  private static Recipe withLines(Recipe r, List<Line> lines) {
    return new Recipe(
        r.id(),
        r.ownerKind(),
        r.ownerAccountId(),
        r.ownerName(),
        r.name(),
        r.notes(),
        r.cone(),
        r.atmosphere(),
        r.foodSurface(),
        r.targetSg(),
        r.sharedWithStudio(),
        r.source(),
        List.copyOf(lines),
        r.canEdit(),
        r.version(),
        r.updatedAt());
  }

  private static final String SELECT_RECIPE =
      """
      select r.id, r.owner_kind, r.owner_account_id, a.display_name as owner_name, r.name, r.notes, r.cone,
             r.atmosphere, r.food_surface, r.target_sg, r.shared_with_studio, r.source, r.version, r.updated_at
      from glaze_recipe r left join account a on a.id = r.owner_account_id
      """;

  /** A recipe row, without its lines; {@link #withLines} adds them. */
  private static Recipe recipe(ResultSet rs, TenantContext caller) throws SQLException {
    OwnerKind kind = OwnerKind.valueOf(rs.getString("owner_kind"));
    UUID owner = rs.getObject("owner_account_id", UUID.class);
    BigDecimal sg = rs.getBigDecimal("target_sg");
    return new Recipe(
        rs.getObject("id", UUID.class),
        kind,
        owner,
        rs.getString("owner_name"),
        rs.getString("name"),
        rs.getString("notes"),
        rs.getString("cone"),
        rs.getString("atmosphere"),
        rs.getBoolean("food_surface"),
        sg == null ? null : sg.stripTrailingZeros(),
        rs.getBoolean("shared_with_studio"),
        rs.getString("source"),
        List.of(),
        canEdit(caller, kind, owner),
        rs.getInt("version"),
        rs.getObject("updated_at", OffsetDateTime.class).toInstant());
  }

  private static @Nullable String strip(@Nullable String text) {
    return text == null || text.isBlank() ? null : text.strip();
  }

  private static Instant now() {
    return Instant.now().truncatedTo(ChronoUnit.MICROS);
  }
}
