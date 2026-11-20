package io.github.aaronfilson.studiomanager.glaze;

import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * Pure parser and validator for untrusted {@code glazecalc-export} v1 and {@code
 * glaze-vault-export} v1 JSON documents (docs/planning/13 and 14).
 */
final class GlazeImportParser {

  static final int MAX_IMPORT_BYTES = 2 * 1024 * 1024;
  static final int MAX_RECIPES = 500;
  static final int MAX_LINES = GlazeBook.MAX_LINES;

  static final String FORMAT_GLAZECALC = "glazecalc-export";
  static final String FORMAT_GLAZE_VAULT = "glaze-vault-export";

  private static final Pattern SCRIPT_OR_STYLE =
      Pattern.compile("(?is)<(script|style)[^>]*>.*?</\\1>");
  private static final Pattern HTML_TAG = Pattern.compile("(?s)<[^>]*>");
  private static final Pattern CONE_PREFIX = Pattern.compile("(?i)^(?:cone\\s*|c\\s*|\\^|Δ)\\s*");

  private GlazeImportParser() {}

  record ParsedExport(
      String format,
      GlazeRecipe.Source source,
      @Nullable String remoteAccountLabel,
      List<ParsedRecipe> recipes) {}

  record ParsedRecipe(
      int index,
      String displayName,
      @Nullable String sourceRef,
      @Nullable Integer sourceRev,
      @Nullable Instant sourceUpdatedAt,
      GlazeBook.@Nullable Draft draft,
      @Nullable String error,
      List<String> warnings) {}

  static ParsedExport parse(JsonNode root) {
    if (root == null || !root.isObject()) {
      throw new InvalidInputException("The import file must be a JSON object.");
    }
    String format = textOrNull(root.get("format"));
    JsonNode versionNode = root.get("version");
    Integer version =
        versionNode != null && versionNode.isIntegralNumber() ? versionNode.intValue() : null;

    if (FORMAT_GLAZECALC.equals(format)) {
      if (version == null || version != 1) {
        throw new InvalidInputException(
            "Unsupported glazecalc-export version. Only version 1 is supported.");
      }
      return parseGlazecalc(root);
    }
    if (FORMAT_GLAZE_VAULT.equals(format)) {
      if (version == null || version != 1) {
        throw new InvalidInputException(
            "Unsupported glaze-vault-export version. Only version 1 is supported.");
      }
      return parseGlazeVault(root);
    }
    throw new InvalidInputException(
        "Unsupported glaze export format. Upload a glazecalc-export v1 or glaze-vault-export v1 JSON file.");
  }

  private static ParsedExport parseGlazecalc(JsonNode root) {
    String accountLabel = extractGlazecalcAccountLabel(root.get("account"));
    JsonNode records = root.get("records");
    if (records == null || !records.isObject()) {
      throw new InvalidInputException(
          "This glazecalc-export file is missing its \"records\" object.");
    }
    JsonNode recipesNode = records.get("recipes");
    if (recipesNode == null || recipesNode.isNull()) {
      return new ParsedExport(FORMAT_GLAZECALC, GlazeRecipe.Source.GLAZECALC, accountLabel, List.of());
    }
    if (!recipesNode.isArray()) {
      throw new InvalidInputException("records.recipes must be an array.");
    }
    if (recipesNode.size() > MAX_RECIPES) {
      throw new InvalidInputException(
          "An import can contain at most %d recipes (found %d)."
              .formatted(MAX_RECIPES, recipesNode.size()));
    }

    List<ParsedRecipe> parsed = new ArrayList<>(recipesNode.size());
    for (int i = 0; i < recipesNode.size(); i++) {
      parsed.add(parseGlazecalcRecipe(i + 1, recipesNode.get(i)));
    }
    return new ParsedExport(FORMAT_GLAZECALC, GlazeRecipe.Source.GLAZECALC, accountLabel, parsed);
  }

  private static ParsedRecipe parseGlazecalcRecipe(int index, JsonNode node) {
    if (node == null || !node.isObject()) {
      return errorRow(index, "Recipe #" + index, null, "Recipe entry is not an object.", List.of());
    }
    List<String> warnings = new ArrayList<>();
    String rawTitle = stripHtml(textOrNull(node.get("title")));
    String displayName =
        rawTitle == null || rawTitle.isBlank() ? "Recipe #" + index : truncate(rawTitle, 120);
    String sourceRef = trimToMax(textOrNull(node.get("_id")), 200);
    if (sourceRef == null && rawTitle != null && !rawTitle.isBlank()) {
      sourceRef = trimToMax(rawTitle, 200);
      warnings.add("Recipe has no _id; matching by title on re-import.");
    }
    if (rawTitle == null || rawTitle.isBlank()) {
      return errorRow(index, displayName, sourceRef, "Recipe is missing a title.", warnings);
    }
    if (rawTitle.length() > 120) {
      warnings.add("Title exceeded 120 characters and was shortened.");
    }

    Integer sourceRev = intOrNull(node.get("rev"));
    Instant sourceUpdatedAt = instantOrNull(node.get("updatedAt"));

    List<String> noteParts = new ArrayList<>();
    JsonNode notesNode = node.get("notes");
    if (notesNode != null && notesNode.isArray()) {
      for (JsonNode entry : notesNode) {
        String clean = stripHtml(textOrNull(entry));
        if (clean != null && !clean.isBlank()) {
          noteParts.add(clean);
        }
      }
    } else {
      String clean = stripHtml(textOrNull(notesNode));
      if (clean != null && !clean.isBlank()) {
        noteParts.add(clean);
      }
    }

    // Firing cone & atmosphere from firedAt or coneResults[0]
    String rawCone = textOrNull(node.get("firedAt"));
    String rawAtmosphere = textOrNull(node.get("atmosphere"));
    JsonNode coneResults = node.get("coneResults");
    if (coneResults != null && coneResults.isArray() && !coneResults.isEmpty()) {
      JsonNode first = coneResults.get(0);
      if (first != null && first.isObject()) {
        if (rawCone == null) {
          rawCone = textOrNull(first.get("cone"));
        }
        if (rawAtmosphere == null) {
          rawAtmosphere = textOrNull(first.get("atmosphere"));
        }
      }
    }

    String cone = parseCone(rawCone, noteParts, warnings);
    String atmosphere = parseAtmosphere(rawAtmosphere, rawCone, noteParts, warnings);
    boolean foodSurface = node.path("foodSurface").asBoolean(false);
    BigDecimal targetSg = parseTargetSg(node.get("sgTarget"), warnings);

    List<GlazeBook.Line> lines = new ArrayList<>();
    String materialsError =
        parseGlazecalcLines(node.get("materials"), GlazeBook.LineKind.BASE, lines, warnings);
    if (materialsError != null) {
      return errorRow(index, displayName, sourceRef, materialsError, warnings);
    }
    String additivesError =
        parseGlazecalcLines(node.get("additives"), GlazeBook.LineKind.ADDITION, lines, warnings);
    if (additivesError != null) {
      return errorRow(index, displayName, sourceRef, additivesError, warnings);
    }
    if (lines.isEmpty()) {
      return errorRow(index, displayName, sourceRef, "Recipe has no ingredient lines.", warnings);
    }
    if (lines.size() > MAX_LINES) {
      return errorRow(
          index,
          displayName,
          sourceRef,
          "Recipe has %d lines; the maximum is %d.".formatted(lines.size(), MAX_LINES),
          warnings);
    }

    String joinedNotes = joinNotes(noteParts, warnings);
    GlazeBook.Draft draft =
        new GlazeBook.Draft(
            displayName, joinedNotes, cone, atmosphere, foodSurface, targetSg, false, List.copyOf(lines));
    return new ParsedRecipe(
        index, displayName, sourceRef, sourceRev, sourceUpdatedAt, draft, null, List.copyOf(warnings));
  }

  private static @Nullable String parseGlazecalcLines(
      @Nullable JsonNode arrayNode,
      GlazeBook.LineKind kind,
      List<GlazeBook.Line> out,
      List<String> warnings) {
    if (arrayNode == null || arrayNode.isNull() || arrayNode.isMissingNode()) {
      return null;
    }
    if (!arrayNode.isArray()) {
      return kind == GlazeBook.LineKind.BASE
          ? "materials must be an array."
          : "additives must be an array.";
    }
    for (int i = 0; i < arrayNode.size(); i++) {
      JsonNode lineNode = arrayNode.get(i);
      if (lineNode == null || !lineNode.isObject()) {
        return "Line %d is not an object.".formatted(i + 1);
      }
      String material = stripHtml(textOrNull(lineNode.get("name")));
      if (material == null || material.isBlank()) {
        return "Line %d is missing a material name.".formatted(i + 1);
      }
      if (material.length() > 80) {
        material = material.substring(0, 80);
        warnings.add("Material name \"%s…\" was shortened to 80 characters.".formatted(material.substring(0, 20)));
      }
      ParsedAmount parsedAmount = parseAmount(lineNode.get("amount"), material, warnings);
      if (parsedAmount.error() != null) {
        return parsedAmount.error();
      }
      GlazeBook.Unit unit =
          kind == GlazeBook.LineKind.BASE
              ? GlazeBook.Unit.PARTS
              : parseAdditiveUnit(textOrNull(lineNode.get("unit")), material, warnings);
      GlazeBook.Chemistry chemistry = parseGlazecalcChemistry(lineNode);
      if (chemistry == null || chemistry.analysis().isEmpty()) {
        warnings.add("No chemistry snapshot on \"%s\" (built-in library will be used).".formatted(material));
      }
      out.add(
          new GlazeBook.Line(
              kind, material, parsedAmount.value(), parsedAmount.rawText(), unit, chemistry));
    }
    return null;
  }

  private static GlazeBook.@Nullable Chemistry parseGlazecalcChemistry(JsonNode lineNode) {
    String materialId = trimToMax(textOrNull(lineNode.get("_id")), 120);
    Map<String, Double> analysis = extractAnalysis(lineNode.get("analysis"));
    Double loi = parseNonNegativeDouble(lineNode.get("loi"));
    if (materialId == null && analysis.isEmpty() && loi == null) {
      return null;
    }
    return new GlazeBook.Chemistry(materialId, analysis, loi);
  }

  private static ParsedExport parseGlazeVault(JsonNode root) {
    JsonNode recipesNode = root.get("recipes");
    if (recipesNode == null || recipesNode.isNull()) {
      return new ParsedExport(FORMAT_GLAZE_VAULT, GlazeRecipe.Source.GLAZE_VAULT, null, List.of());
    }
    if (!recipesNode.isArray()) {
      throw new InvalidInputException("recipes must be an array.");
    }
    if (recipesNode.size() > MAX_RECIPES) {
      throw new InvalidInputException(
          "An import can contain at most %d recipes (found %d)."
              .formatted(MAX_RECIPES, recipesNode.size()));
    }

    Map<String, GlazeBook.Chemistry> materialsByName = new HashMap<>();
    JsonNode materialsNode = root.get("materials");
    if (materialsNode != null && materialsNode.isArray()) {
      for (JsonNode mat : materialsNode) {
        if (mat == null || !mat.isObject()) {
          continue;
        }
        String matName = textOrNull(mat.get("name"));
        if (matName == null) {
          continue;
        }
        Map<String, Double> analysis = extractAnalysis(mat.get("oxides"));
        if (analysis.isEmpty()) {
          analysis = extractAnalysis(mat.get("analysis"));
        }
        Double loi = parseNonNegativeDouble(mat.get("loi"));
        if (!analysis.isEmpty() || loi != null) {
          materialsByName.put(
              matName.toLowerCase(Locale.ROOT), new GlazeBook.Chemistry(null, analysis, loi));
        }
      }
    }

    List<ParsedRecipe> parsed = new ArrayList<>(recipesNode.size());
    for (int i = 0; i < recipesNode.size(); i++) {
      parsed.add(parseGlazeVaultRecipe(i + 1, recipesNode.get(i), materialsByName));
    }
    return new ParsedExport(FORMAT_GLAZE_VAULT, GlazeRecipe.Source.GLAZE_VAULT, null, parsed);
  }

  private static ParsedRecipe parseGlazeVaultRecipe(
      int index, JsonNode node, Map<String, GlazeBook.Chemistry> materialsByName) {
    if (node == null || !node.isObject()) {
      return errorRow(index, "Recipe #" + index, null, "Recipe entry is not an object.", List.of());
    }
    List<String> warnings = new ArrayList<>();
    String rawName = stripHtml(textOrNull(node.get("name")));
    String displayName =
        rawName == null || rawName.isBlank() ? "Recipe #" + index : truncate(rawName, 120);
    String sourceRef = rawName == null || rawName.isBlank() ? null : trimToMax(rawName, 200);
    if (rawName == null || rawName.isBlank()) {
      return errorRow(index, displayName, null, "Recipe is missing a name.", warnings);
    }
    if (rawName.length() > 120) {
      warnings.add("Name exceeded 120 characters and was shortened.");
    }
    warnings.add("glaze-vault exports match recipes by name on re-import.");

    Integer sourceRev = intOrNull(node.get("revision"));
    Instant sourceUpdatedAt = instantOrNull(node.get("savedAt"));

    List<String> noteParts = new ArrayList<>();
    String cleanNotes = stripHtml(textOrNull(node.get("notes")));
    if (cleanNotes != null && !cleanNotes.isBlank()) {
      noteParts.add(cleanNotes);
    }

    String rawCone = textOrNull(node.get("cone"));
    String rawAtmosphere = textOrNull(node.get("atmosphere"));
    String cone = parseCone(rawCone, noteParts, warnings);
    String atmosphere = parseAtmosphere(rawAtmosphere, rawCone, noteParts, warnings);
    BigDecimal targetSg = parseTargetSg(node.get("gravity"), warnings);

    JsonNode linesNode = node.get("lines");
    if (linesNode == null || !linesNode.isArray() || linesNode.isEmpty()) {
      return errorRow(index, displayName, sourceRef, "Recipe has no ingredient lines.", warnings);
    }
    if (linesNode.size() > MAX_LINES) {
      return errorRow(
          index,
          displayName,
          sourceRef,
          "Recipe has %d lines; the maximum is %d.".formatted(linesNode.size(), MAX_LINES),
          warnings);
    }

    List<GlazeBook.Line> lines = new ArrayList<>(linesNode.size());
    for (int i = 0; i < linesNode.size(); i++) {
      JsonNode lineNode = linesNode.get(i);
      if (lineNode == null || !lineNode.isObject()) {
        return errorRow(
            index, displayName, sourceRef, "Line %d is not an object.".formatted(i + 1), warnings);
      }
      String material = stripHtml(textOrNull(lineNode.get("material")));
      if (material == null || material.isBlank()) {
        return errorRow(
            index,
            displayName,
            sourceRef,
            "Line %d is missing a material name.".formatted(i + 1),
            warnings);
      }
      if (material.length() > 80) {
        material = material.substring(0, 80);
      }
      ParsedAmount parsedAmount = parseAmount(lineNode.get("amount"), material, warnings);
      if (parsedAmount.error() != null) {
        return errorRow(index, displayName, sourceRef, parsedAmount.error(), warnings);
      }
      boolean isAddition = lineNode.path("addition").asBoolean(false);
      GlazeBook.LineKind kind = isAddition ? GlazeBook.LineKind.ADDITION : GlazeBook.LineKind.BASE;
      GlazeBook.Unit unit = isAddition ? GlazeBook.Unit.PERCENT : GlazeBook.Unit.PARTS;
      GlazeBook.Chemistry chemistry = materialsByName.get(material.toLowerCase(Locale.ROOT));
      if (chemistry == null) {
        warnings.add("No chemistry found for \"%s\" in export (built-in library will be used).".formatted(material));
      }
      lines.add(
          new GlazeBook.Line(
              kind, material, parsedAmount.value(), parsedAmount.rawText(), unit, chemistry));
    }

    String joinedNotes = joinNotes(noteParts, warnings);
    GlazeBook.Draft draft =
        new GlazeBook.Draft(
            displayName, joinedNotes, cone, atmosphere, false, targetSg, false, List.copyOf(lines));
    return new ParsedRecipe(
        index, displayName, sourceRef, sourceRev, sourceUpdatedAt, draft, null, List.copyOf(warnings));
  }

  /** Strips HTML tags, scripts, styles, and common HTML entities to plain text. */
  static @Nullable String stripHtml(@Nullable String raw) {
    if (raw == null) {
      return null;
    }
    String text = SCRIPT_OR_STYLE.matcher(raw).replaceAll("");
    text = text.replaceAll("(?i)<br\\s*/?>", "\n").replaceAll("(?i)</p>", "\n");
    text = HTML_TAG.matcher(text).replaceAll("");
    text =
        text.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("\r\n", "\n")
            .replace('\r', '\n');
    String stripped = text.strip();
    return stripped.isEmpty() ? null : stripped;
  }

  private record ParsedAmount(
      BigDecimal value, @Nullable String rawText, @Nullable String error) {}

  private static ParsedAmount parseAmount(
      @Nullable JsonNode node, String material, List<String> warnings) {
    if (node == null || node.isNull() || node.isMissingNode()) {
      return new ParsedAmount(
          BigDecimal.ZERO, null, "Amount for \"%s\" is missing.".formatted(material));
    }
    String rawText;
    if (node.isNumber()) {
      rawText = node.decimalValue().stripTrailingZeros().toPlainString();
    } else if (node.isString()) {
      rawText = node.asString().strip();
    } else {
      return new ParsedAmount(
          BigDecimal.ZERO, null, "Amount for \"%s\" could not be parsed.".formatted(material));
    }
    if (rawText.isEmpty()) {
      return new ParsedAmount(
          BigDecimal.ZERO, null, "Amount for \"%s\" is blank.".formatted(material));
    }
    BigDecimal parsed;
    try {
      parsed = new BigDecimal(rawText);
    } catch (NumberFormatException e) {
      return new ParsedAmount(
          BigDecimal.ZERO,
          trimToMax(rawText, 30),
          "Amount \"%s\" for \"%s\" is not a valid number.".formatted(rawText, material));
    }
    if (parsed.signum() <= 0 || parsed.compareTo(GlazeBook.MAX_AMOUNT) > 0) {
      return new ParsedAmount(
          BigDecimal.ZERO,
          trimToMax(rawText, 30),
          "Amount for \"%s\" must be greater than 0 and at most 100,000.".formatted(material));
    }
    if (parsed.scale() > 4) {
      parsed = parsed.setScale(4, RoundingMode.HALF_UP);
      warnings.add(
          "Amount \"%s\" for \"%s\" was rounded to 4 decimal places (original text preserved)."
              .formatted(rawText, material));
    }
    return new ParsedAmount(parsed.stripTrailingZeros(), trimToMax(rawText, 30), null);
  }

  private static GlazeBook.Unit parseAdditiveUnit(
      @Nullable String raw, String material, List<String> warnings) {
    if (raw == null || raw.isBlank()) {
      return GlazeBook.Unit.PERCENT;
    }
    return switch (raw.strip().toLowerCase(Locale.ROOT)) {
      case "percent", "%", "pct" -> GlazeBook.Unit.PERCENT;
      case "parts", "part" -> GlazeBook.Unit.PARTS;
      case "grams", "gram", "g" -> GlazeBook.Unit.GRAMS;
      default -> {
        warnings.add(
            "Unknown additive unit \"%s\" on \"%s\"; defaulted to percent.".formatted(raw, material));
        yield GlazeBook.Unit.PERCENT;
      }
    };
  }

  private static @Nullable String parseCone(
      @Nullable String rawCone, List<String> noteParts, List<String> warnings) {
    String clean = stripHtml(rawCone);
    if (clean == null) {
      return null;
    }
    String withoutPrefix = CONE_PREFIX.matcher(clean).replaceFirst("").strip();
    // Strip trailing atmosphere word when someone wrote "Cone 6 oxidation"
    String shortCone =
        withoutPrefix
            .replaceAll("(?i)\\s+(oxidation|reduction|neutral|ox|red)$", "")
            .strip();
    if (!shortCone.isEmpty() && shortCone.length() <= 10) {
      return shortCone;
    }
    warnings.add("Cone \"%s\" didn't fit in 10 characters and was moved to notes.".formatted(clean));
    noteParts.add("Firing: " + clean);
    return null;
  }

  private static @Nullable String parseAtmosphere(
      @Nullable String rawAtmosphere,
      @Nullable String rawCone,
      List<String> noteParts,
      List<String> warnings) {
    String clean = stripHtml(rawAtmosphere);
    if (clean != null) {
      String mapped = mapAtmosphere(clean);
      if (mapped != null) {
        return mapped;
      }
      warnings.add("Unknown atmosphere \"%s\" was moved to notes.".formatted(clean));
      noteParts.add("Atmosphere: " + clean);
      return null;
    }
    if (rawCone != null) {
      String lower = rawCone.toLowerCase(Locale.ROOT);
      if (lower.contains("oxidation") || lower.contains("electric")) {
        return "OXIDATION";
      }
      if (lower.contains("reduction") || lower.contains("gas") || lower.contains("wood")) {
        return "REDUCTION";
      }
      if (lower.contains("neutral")) {
        return "NEUTRAL";
      }
    }
    return null;
  }

  private static @Nullable String mapAtmosphere(String value) {
    String lower = value.strip().toLowerCase(Locale.ROOT);
    return switch (lower) {
      case "oxidation", "ox", "electric", "oxidizing" -> "OXIDATION";
      case "reduction", "red", "gas", "wood", "soda", "salt", "reducing" -> "REDUCTION";
      case "neutral" -> "NEUTRAL";
      default -> null;
    };
  }

  private static @Nullable BigDecimal parseTargetSg(@Nullable JsonNode node, List<String> warnings) {
    if (node == null || node.isNull() || node.isMissingNode()) {
      return null;
    }
    BigDecimal value;
    try {
      value =
          node.isNumber()
              ? node.decimalValue()
              : node.isString() && !node.asString().isBlank()
                  ? new BigDecimal(node.asString().strip())
                  : null;
    } catch (NumberFormatException e) {
      warnings.add("Ignored unparseable target specific gravity \"%s\".".formatted(node.asString("")));
      return null;
    }
    if (value == null) {
      return null;
    }
    if (value.compareTo(GlazeEdits.MIN_SPECIFIC_GRAVITY) < 0
        || value.compareTo(GlazeEdits.MAX_SPECIFIC_GRAVITY) > 0) {
      warnings.add(
          "Ignored out-of-range specific gravity %s (must be 1.0–2.5).".formatted(value.toPlainString()));
      return null;
    }
    if (value.scale() > 3) {
      value = value.setScale(3, RoundingMode.HALF_UP);
    }
    return value.stripTrailingZeros();
  }

  private static Map<String, Double> extractAnalysis(@Nullable JsonNode analysisNode) {
    if (analysisNode == null || !analysisNode.isObject()) {
      return Map.of();
    }
    Map<String, Double> out = new LinkedHashMap<>();
    for (var entry : analysisNode.properties()) {
      Double value = GlazeChemistryJson.parseDouble(entry.getValue());
      if (value != null && Double.isFinite(value) && value > 0) {
        out.put(entry.getKey(), value);
      }
    }
    return Map.copyOf(out);
  }

  private static @Nullable String extractGlazecalcAccountLabel(@Nullable JsonNode accountNode) {
    if (accountNode == null || !accountNode.isObject()) {
      return null;
    }
    String email = textOrNull(accountNode.get("email"));
    if (email != null) {
      return trimToMax(email, 200);
    }
    String name = textOrNull(accountNode.get("name"));
    if (name != null) {
      return trimToMax(name, 200);
    }
    return trimToMax(textOrNull(accountNode.get("username")), 200);
  }

  private static @Nullable String joinNotes(List<String> parts, List<String> warnings) {
    if (parts.isEmpty()) {
      return null;
    }
    String joined = String.join("\n\n", parts).strip();
    if (joined.length() > 4000) {
      warnings.add("Notes exceeded 4,000 characters and were truncated.");
      return joined.substring(0, 4000);
    }
    return joined;
  }

  private static ParsedRecipe errorRow(
      int index,
      String displayName,
      @Nullable String sourceRef,
      String error,
      List<String> warnings) {
    return new ParsedRecipe(
        index, displayName, sourceRef, null, null, null, error, List.copyOf(warnings));
  }

  private static @Nullable String textOrNull(@Nullable JsonNode node) {
    if (node == null || node.isNull() || node.isMissingNode()) {
      return null;
    }
    if (!node.isString()) {
      return null;
    }
    String stripped = node.asString().strip();
    return stripped.isEmpty() ? null : stripped;
  }

  private static @Nullable Integer intOrNull(@Nullable JsonNode node) {
    if (node == null || node.isNull() || node.isMissingNode()) {
      return null;
    }
    if (node.isIntegralNumber() && node.canConvertToInt()) {
      return node.intValue();
    }
    return null;
  }

  private static @Nullable Double parseNonNegativeDouble(@Nullable JsonNode node) {
    Double value = GlazeChemistryJson.parseDouble(node);
    return value != null && Double.isFinite(value) && value >= 0 ? value : null;
  }

  private static @Nullable Instant instantOrNull(@Nullable JsonNode node) {
    String text = textOrNull(node);
    if (text == null) {
      return null;
    }
    try {
      return Instant.parse(text);
    } catch (DateTimeParseException ignored) {
      return null;
    }
  }

  private static String truncate(String text, int max) {
    return text.length() <= max ? text : text.substring(0, max);
  }

  private static @Nullable String trimToMax(@Nullable String text, int max) {
    if (text == null || text.isBlank()) {
      return null;
    }
    String clean = text.strip();
    return clean.length() <= max ? clean : clean.substring(0, max);
  }
}
