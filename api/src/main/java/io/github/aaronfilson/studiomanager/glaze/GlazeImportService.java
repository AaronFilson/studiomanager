package io.github.aaronfilson.studiomanager.glaze;

import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Dry-run preview and atomic import of {@code glazecalc-export} v1 and {@code glaze-vault-export}
 * v1 JSON documents, plus exporting any recipe back into a valid {@code glazecalc-export} v1 file.
 */
@Service
public class GlazeImportService {

  private final GlazeRecipeRepository recipes;
  private final GlazeBook book;
  private final EntityManager entityManager;
  private final JsonMapper json;
  private final Clock clock;

  GlazeImportService(
      GlazeRecipeRepository recipes,
      GlazeBook book,
      EntityManager entityManager,
      JsonMapper json,
      Clock clock) {
    this.recipes = recipes;
    this.book = book;
    this.entityManager = entityManager;
    this.json = json;
    this.clock = clock;
  }

  public enum Outcome {
    NEW,
    UPDATED,
    UNCHANGED,
    ERROR
  }

  public record Row(
      int index,
      String name,
      @Nullable String sourceRef,
      Outcome outcome,
      @Nullable UUID recipeId,
      String message,
      List<String> warnings) {}

  public record Result(
      String format,
      boolean committed,
      int createdCount,
      int updatedCount,
      int unchangedCount,
      int errorCount,
      List<Row> rows,
      @Nullable String remoteAccountLabel) {}

  @Transactional
  public Result importMap(Map<String, Object> body, boolean dryRun) {
    byte[] encoded = json.writeValueAsBytes(body);
    if (encoded.length > GlazeImportParser.MAX_IMPORT_BYTES) {
      throw new InvalidInputException("The import file exceeds the 2 MB limit.");
    }
    return importNode(json.readTree(encoded), dryRun);
  }

  @Transactional
  public Result importJson(String rawJson, boolean dryRun) {
    if (rawJson.getBytes(StandardCharsets.UTF_8).length > GlazeImportParser.MAX_IMPORT_BYTES) {
      throw new InvalidInputException("The import file exceeds the 2 MB limit.");
    }
    JsonNode root;
    try {
      root = json.readTree(rawJson);
    } catch (JacksonException e) {
      throw new InvalidInputException("The import file is not valid JSON.");
    }
    return importNode(root, dryRun);
  }

  @Transactional
  public Result importNode(JsonNode root, boolean dryRun) {
    TenantContext caller = TenantContext.require();
    GlazeImportParser.ParsedExport parsed = GlazeImportParser.parse(root);

    Map<String, GlazeRecipe> existingByRef = new HashMap<>();
    for (GlazeRecipe existing :
        recipes.findImportedByOwnerAndSourceWithLines(caller.accountId(), parsed.source())) {
      if (existing.sourceRef() != null) {
        existingByRef.put(existing.sourceRef(), existing);
      }
    }

    Set<String> seenRefsInFile = new HashSet<>();
    List<Planned> plan = new ArrayList<>(parsed.recipes().size());
    int errors = 0;

    for (GlazeImportParser.ParsedRecipe item : parsed.recipes()) {
      List<String> warnings = new ArrayList<>(item.warnings());
      if (item.error() != null || item.draft() == null) {
        errors++;
        plan.add(
            new Planned(
                item,
                null,
                null,
                Outcome.ERROR,
                item.error() == null ? "Invalid recipe." : item.error(),
                warnings));
        continue;
      }
      if (item.sourceRef() != null && !seenRefsInFile.add(item.sourceRef())) {
        errors++;
        plan.add(
            new Planned(
                item,
                null,
                null,
                Outcome.ERROR,
                "Duplicate recipe reference \"%s\" in the same import file."
                    .formatted(item.sourceRef()),
                warnings));
        continue;
      }

      GlazeBook.Draft clean;
      try {
        clean = GlazeEdits.check(GlazeBook.OwnerKind.MEMBER, item.draft());
      } catch (InvalidInputException e) {
        errors++;
        plan.add(
            new Planned(
                item,
                null,
                null,
                Outcome.ERROR,
                e.getMessage() == null ? "Invalid recipe." : e.getMessage(),
                warnings));
        continue;
      }

      GlazeRecipe existing =
          item.sourceRef() == null ? null : existingByRef.get(item.sourceRef());
      if (existing == null) {
        plan.add(
            new Planned(item, clean, null, Outcome.NEW, "Will create a new recipe.", warnings));
      } else if (isOlderSource(existing, item.sourceRev(), item.sourceUpdatedAt())) {
        warnings.add("Studio copy is from a newer source revision; left unchanged.");
        plan.add(
            new Planned(
                item,
                clean,
                existing,
                Outcome.UNCHANGED,
                "Already up to date (newer copy in studio).",
                warnings));
      } else if (existing.matchesImportContent(clean)
          && !isNewerSource(existing, item.sourceRev(), item.sourceUpdatedAt())) {
        plan.add(
            new Planned(item, clean, existing, Outcome.UNCHANGED, "No changes.", warnings));
      } else {
        plan.add(
            new Planned(
                item,
                clean,
                existing,
                Outcome.UPDATED,
                "Will update existing recipe.",
                warnings));
      }
    }

    boolean willCommit = !dryRun && errors == 0;
    Instant now = clock.instant();
    List<Row> rows = new ArrayList<>(plan.size());
    int createdCount = 0;
    int updatedCount = 0;
    int unchangedCount = 0;

    for (Planned step : plan) {
      UUID recipeId = step.existing() == null ? null : step.existing().id();
      String message = step.message();
      if (willCommit && step.draft() != null) {
        if (step.outcome() == Outcome.NEW) {
          GlazeRecipe created =
              new GlazeRecipe(
                  GlazeBook.OwnerKind.MEMBER,
                  caller.accountId(),
                  step.draft(),
                  parsed.source(),
                  step.parsed().sourceRef(),
                  step.parsed().sourceRev(),
                  step.parsed().sourceUpdatedAt(),
                  null,
                  caller.accountId(),
                  now);
          entityManager.persist(created);
          recipeId = created.id();
          message = "Created.";
        } else if (step.outcome() == Outcome.UPDATED && step.existing() != null) {
          step.existing()
              .applyImport(
                  step.draft(),
                  step.parsed().sourceRev(),
                  step.parsed().sourceUpdatedAt(),
                  caller.accountId(),
                  now);
          recipeId = step.existing().id();
          message = "Updated.";
        }
      }
      switch (step.outcome()) {
        case NEW -> createdCount++;
        case UPDATED -> updatedCount++;
        case UNCHANGED -> unchangedCount++;
        case ERROR -> {
          // already counted in errors
        }
      }
      rows.add(
          new Row(
              step.parsed().index(),
              step.parsed().displayName(),
              step.parsed().sourceRef(),
              step.outcome(),
              recipeId,
              message,
              List.copyOf(step.warnings())));
    }

    if (willCommit) {
      entityManager.flush();
    }

    return new Result(
        parsed.format(),
        willCommit,
        createdCount,
        updatedCount,
        unchangedCount,
        errors,
        List.copyOf(rows),
        parsed.remoteAccountLabel());
  }

  /** Exports a recipe visible to the caller as a valid {@code glazecalc-export} v1 JSON object. */
  @Transactional(readOnly = true)
  public Map<String, Object> exportAsGlazecalc(UUID glazeId) {
    GlazeRecipe recipe = book.find(glazeId);

    List<Map<String, Object>> materials = new ArrayList<>();
    List<Map<String, Object>> additives = new ArrayList<>();
    for (GlazeBook.Line line : recipe.lineViews()) {
      Map<String, Object> entry = new LinkedHashMap<>();
      entry.put("name", line.material());
      entry.put(
          "amount",
          line.amountText() != null ? line.amountText() : line.amount().toPlainString());
      if (line.kind() == GlazeBook.LineKind.ADDITION) {
        entry.put("unit", line.unit().name().toLowerCase(Locale.ROOT));
      }
      GlazeBook.Chemistry chem = line.chemistry();
      if (chem != null) {
        if (chem.materialId() != null) {
          entry.put("_id", chem.materialId());
        }
        if (!chem.analysis().isEmpty()) {
          entry.put("analysis", chem.analysis());
        }
        if (chem.loi() != null) {
          entry.put("loi", chem.loi());
        }
      }
      if (line.kind() == GlazeBook.LineKind.BASE) {
        materials.add(entry);
      } else {
        additives.add(entry);
      }
    }

    Map<String, Object> recipeDoc = new LinkedHashMap<>();
    recipeDoc.put(
        "_id", recipe.sourceRef() != null ? recipe.sourceRef() : recipe.id().toString());
    recipeDoc.put("title", recipe.name());
    if (recipe.notes() != null && !recipe.notes().isBlank()) {
      recipeDoc.put("notes", List.of(recipe.notes().split("\\n\\n+", -1)));
    } else {
      recipeDoc.put("notes", List.of());
    }
    if (recipe.cone() != null) {
      recipeDoc.put("firedAt", recipe.cone());
    }
    if (recipe.cone() != null || recipe.atmosphere() != null) {
      Map<String, Object> coneResult = new LinkedHashMap<>();
      if (recipe.cone() != null) {
        coneResult.put("cone", recipe.cone());
      }
      if (recipe.atmosphere() != null) {
        coneResult.put("atmosphere", recipe.atmosphere().toLowerCase(Locale.ROOT));
      }
      recipeDoc.put("coneResults", List.of(coneResult));
    }
    recipeDoc.put("foodSurface", recipe.foodSurface());
    if (recipe.targetSg() != null) {
      recipeDoc.put("sgTarget", recipe.targetSg().stripTrailingZeros().doubleValue());
    }
    recipeDoc.put("rev", recipe.sourceRev() != null ? recipe.sourceRev() : recipe.version());
    recipeDoc.put("updatedAt", recipe.updatedAt().toString());
    recipeDoc.put("materials", materials);
    recipeDoc.put("additives", additives);

    Map<String, Object> root = new LinkedHashMap<>();
    root.put("format", GlazeImportParser.FORMAT_GLAZECALC);
    root.put("version", 1);
    root.put("exportedAt", clock.instant().toString());
    root.put("records", Map.of("recipes", List.of(recipeDoc)));
    return root;
  }

  private record Planned(
      GlazeImportParser.ParsedRecipe parsed,
      GlazeBook.@Nullable Draft draft,
      @Nullable GlazeRecipe existing,
      Outcome outcome,
      String message,
      List<String> warnings) {}

  private static boolean isOlderSource(
      GlazeRecipe existing, @Nullable Integer incomingRev, @Nullable Instant incomingUpdatedAt) {
    if (existing.sourceRev() != null && incomingRev != null) {
      return incomingRev < existing.sourceRev();
    }
    if (existing.sourceUpdatedAt() != null && incomingUpdatedAt != null) {
      return incomingUpdatedAt.isBefore(existing.sourceUpdatedAt());
    }
    return false;
  }

  private static boolean isNewerSource(
      GlazeRecipe existing, @Nullable Integer incomingRev, @Nullable Instant incomingUpdatedAt) {
    if (existing.sourceRev() != null && incomingRev != null && incomingRev > existing.sourceRev()) {
      return true;
    }
    return existing.sourceUpdatedAt() != null
        && incomingUpdatedAt != null
        && incomingUpdatedAt.isAfter(existing.sourceUpdatedAt());
  }
}
