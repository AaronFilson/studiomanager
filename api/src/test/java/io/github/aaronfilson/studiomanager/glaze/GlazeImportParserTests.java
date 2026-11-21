package io.github.aaronfilson.studiomanager.glaze;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Pure unit tests for untrusted glazecalc-export v1 and glaze-vault-export v1 parsing. */
class GlazeImportParserTests {

  private final JsonMapper json = JsonMapper.builder().build();

  @Test
  void parsesGlazecalcExportV1WithChemistryHtmlStrippingAndAdditiveUnits() {
    String payload =
        """
        {
          "format": "glazecalc-export",
          "version": 1,
          "account": {"email": "potter@example.com"},
          "records": {
            "recipes": [
              {
                "_id": "gc-recipe-42",
                "title": "Leach <b>4321</b>",
                "rev": 3,
                "updatedAt": "2026-11-10T12:00:00Z",
                "firedAt": "Cone 10 reduction",
                "foodSurface": true,
                "sgTarget": 1.46,
                "notes": ["<p>Classic clear</p><script>alert(1)</script>", "Dip for <b>3 seconds</b>"],
                "materials": [
                  {
                    "_id": "std-custer",
                    "name": "Custer Feldspar",
                    "amount": "40.00",
                    "loi": 0.3,
                    "analysis": {"SiO2": 68.5, "Al2O3": 17.0, "K2O": 10.0, "Na2O": 3.0}
                  },
                  {"name": "Silica", "amount": "30"},
                  {"name": "Whiting", "amount": "20"},
                  {"name": "EPK", "amount": "10"}
                ],
                "additives": [
                  {"name": "Red Iron Oxide", "amount": "2.5", "unit": "percent"},
                  {"name": "Epsom Salts", "amount": "5", "unit": "grams"}
                ]
              }
            ]
          }
        }
        """;

    var parsed = GlazeImportParser.parse(json.readTree(payload));
    assertThat(parsed.format()).isEqualTo("glazecalc-export");
    assertThat(parsed.source()).isEqualTo(GlazeRecipe.Source.GLAZECALC);
    assertThat(parsed.remoteAccountLabel()).isEqualTo("potter@example.com");
    assertThat(parsed.recipes()).hasSize(1);

    var recipe = parsed.recipes().getFirst();
    assertThat(recipe.error()).isNull();
    assertThat(recipe.sourceRef()).isEqualTo("gc-recipe-42");
    assertThat(recipe.sourceRev()).isEqualTo(3);
    var draft = recipe.draft();
    assertThat(draft).isNotNull();
    assertThat(draft.name()).isEqualTo("Leach 4321");
    assertThat(draft.cone()).isEqualTo("10");
    assertThat(draft.atmosphere()).isEqualTo("REDUCTION");
    assertThat(draft.foodSurface()).isTrue();
    assertThat(draft.targetSg()).isEqualByComparingTo("1.46");
    assertThat(draft.notes()).isEqualTo("Classic clear\n\nDip for 3 seconds");
    assertThat(draft.notes()).doesNotContain("<script>").doesNotContain("<b>");
    assertThat(draft.lines()).hasSize(6);
    assertThat(draft.lines().get(0).amountText()).isEqualTo("40.00");
    assertThat(draft.lines().get(0).chemistry()).isNotNull();
    assertThat(draft.lines().get(0).chemistry().materialId()).isEqualTo("std-custer");
    assertThat(draft.lines().get(0).chemistry().analysis()).containsEntry("SiO2", 68.5);
    assertThat(draft.lines().get(0).chemistry().loi()).isEqualTo(0.3);
    assertThat(draft.lines().get(4).kind()).isEqualTo(GlazeBook.LineKind.ADDITION);
    assertThat(draft.lines().get(4).unit()).isEqualTo(GlazeBook.Unit.PERCENT);
    assertThat(draft.lines().get(5).unit()).isEqualTo(GlazeBook.Unit.GRAMS);
  }

  @Test
  void parsesGlazeVaultExportV1AndAttachesMaterialChemistryByName() {
    String payload =
        """
        {
          "format": "glaze-vault-export",
          "version": 1,
          "materials": [
            {"name": "Custer Feldspar", "oxides": {"SiO2": 68.5, "Al2O3": 17.0, "K2O": 10.0}, "loi": 0.3}
          ],
          "recipes": [
            {
              "name": "Studio Clear",
              "cone": "6",
              "atmosphere": "Oxidation",
              "gravity": 1.44,
              "revision": 2,
              "savedAt": "2026-11-01T10:00:00Z",
              "notes": "<i>Great liner</i>",
              "lines": [
                {"material": "Custer Feldspar", "amount": 40, "addition": false},
                {"material": "Silica", "amount": 30, "addition": false},
                {"material": "Zircopax", "amount": 8, "addition": true}
              ]
            }
          ]
        }
        """;

    var parsed = GlazeImportParser.parse(json.readTree(payload));
    assertThat(parsed.format()).isEqualTo("glaze-vault-export");
    assertThat(parsed.source()).isEqualTo(GlazeRecipe.Source.GLAZE_VAULT);
    var recipe = parsed.recipes().getFirst();
    assertThat(recipe.error()).isNull();
    assertThat(recipe.sourceRef()).isEqualTo("Studio Clear");
    assertThat(recipe.warnings()).anyMatch(w -> w.contains("by name"));
    var draft = recipe.draft();
    assertThat(draft).isNotNull();
    assertThat(draft.notes()).isEqualTo("Great liner");
    assertThat(draft.cone()).isEqualTo("6");
    assertThat(draft.atmosphere()).isEqualTo("OXIDATION");
    assertThat(draft.targetSg()).isEqualByComparingTo("1.44");
    assertThat(draft.lines().get(0).chemistry()).isNotNull();
    assertThat(draft.lines().get(0).chemistry().analysis()).containsEntry("SiO2", 68.5);
    assertThat(draft.lines().get(2).kind()).isEqualTo(GlazeBook.LineKind.ADDITION);
    assertThat(draft.lines().get(2).unit()).isEqualTo(GlazeBook.Unit.PERCENT);
    assertThat(draft.lines().get(2).amount()).isEqualByComparingTo(BigDecimal.valueOf(8));
  }

  @Test
  void rejectsUnsupportedFormatVersionAndOverLimitRecipeOrLineCounts() {
    assertThatThrownBy(() ->
            GlazeImportParser.parse(json.readTree("{\"format\": \"glazy-xml\", \"version\": 1}")))
        .isInstanceOf(InvalidInputException.class);
    assertThatThrownBy(() ->
            GlazeImportParser.parse(
                json.readTree("{\"format\": \"glazecalc-export\", \"version\": 2}")))
        .isInstanceOf(InvalidInputException.class);

    List<Map<String, Object>> tooManyRecipes =
        IntStream.range(0, 501)
            .mapToObj(i -> Map.<String, Object>of("_id", "r" + i, "title", "R" + i))
            .toList();
    assertThatThrownBy(() ->
            GlazeImportParser.parse(
                json.valueToTree(
                    Map.of(
                        "format",
                        "glazecalc-export",
                        "version",
                        1,
                        "records",
                        Map.of("recipes", tooManyRecipes)))))
        .isInstanceOf(InvalidInputException.class)
        .hasMessageContaining("500");

    List<Map<String, Object>> tooManyLines =
        IntStream.range(0, 61)
            .mapToObj(i -> Map.<String, Object>of("name", "Mat " + i, "amount", "1"))
            .toList();
    var parsed =
        GlazeImportParser.parse(
            json.valueToTree(
                Map.of(
                    "format",
                    "glazecalc-export",
                    "version",
                    1,
                    "records",
                    Map.of(
                        "recipes",
                        List.of(
                            Map.of("_id", "huge", "title", "Huge", "materials", tooManyLines))))));
    assertThat(parsed.recipes().getFirst().error()).contains("60");
  }

  @Test
  void rejectsExtremeScientificNotationOverflowNumbersAndHugeVersionCleanly() {
    String scientificStrings =
        """
        {
          "format": "glazecalc-export",
          "version": 1,
          "records": {
            "recipes": [
              {
                "_id": "sci-1",
                "title": "Tiny Exponent",
                "sgTarget": "1e-10000000",
                "materials": [{"name": "Silica", "amount": "1e-10000000"}]
              },
              {
                "_id": "sci-2",
                "title": "Huge Exponent",
                "sgTarget": "1e100000000",
                "materials": [{"name": "Silica", "amount": "1e100000000"}]
              },
              {
                "_id": "sci-3",
                "title": "Overflow JSON Number",
                "sgTarget": 1e500,
                "materials": [{"name": "Silica", "amount": 1e500}]
              },
              {
                "_id": "sci-4",
                "title": "Overflow SG Only",
                "sgTarget": 1e500,
                "materials": [{"name": "Silica", "amount": "100"}]
              }
            ]
          }
        }
        """;

    var parsed = GlazeImportParser.parse(json.readTree(scientificStrings));
    assertThat(parsed.recipes()).hasSize(4);
    assertThat(parsed.recipes().get(0).error()).contains("not a valid number");
    assertThat(parsed.recipes().get(0).warnings()).anyMatch(w -> w.contains("specific gravity"));
    assertThat(parsed.recipes().get(1).error()).contains("not a valid number");
    assertThat(parsed.recipes().get(1).warnings()).anyMatch(w -> w.contains("specific gravity"));
    assertThat(parsed.recipes().get(2).error()).contains("not a valid number");
    assertThat(parsed.recipes().get(2).warnings()).anyMatch(w -> w.contains("specific gravity"));
    assertThat(parsed.recipes().get(3).error()).isNull();
    assertThat(parsed.recipes().get(3).draft().targetSg()).isNull();
    assertThat(parsed.recipes().get(3).warnings()).anyMatch(w -> w.contains("specific gravity"));

    // Integral version outside int range must not overflow or crash
    assertThatThrownBy(() ->
            GlazeImportParser.parse(
                json.readTree(
                    "{\"format\": \"glazecalc-export\", \"version\": 99999999999999999}")))
        .isInstanceOf(InvalidInputException.class);
  }

  @Test
  void stripHtmlHandlesPathologicalRepeatedAngleBracketsInLinearTime() {
    String pathological = "<".repeat(100_000);
    long started = System.nanoTime();
    String stripped = GlazeImportParser.stripHtml(pathological);
    Duration elapsed = Duration.ofNanos(System.nanoTime() - started);
    assertThat(elapsed).isLessThan(Duration.ofSeconds(2));
    assertThat(stripped).isNotNull();

    assertThat(
            GlazeImportParser.stripHtml("<STYLE>body{color:red}</STYLE><p>Line 1</p><br/>Line 2"))
        .isEqualTo("Line 1\n\nLine 2");
    assertThat(GlazeImportParser.stripHtml("<script type=\"text/javascript\">evil()")).isNull();
  }

  @Test
  void capsChemistryAnalysisEntriesAndOxideKeyLengths() {
    Map<String, Object> hugeAnalysis = new LinkedHashMap<>();
    hugeAnalysis.put("x".repeat(40), 10.0);
    for (int i = 0; i < 80; i++) {
      hugeAnalysis.put("Ox" + i, 1.0);
    }
    var parsed =
        GlazeImportParser.parse(
            json.valueToTree(
                Map.of(
                    "format",
                    "glazecalc-export",
                    "version",
                    1,
                    "records",
                    Map.of(
                        "recipes",
                        List.of(
                            Map.of(
                                "_id",
                                "chem-cap",
                                "title",
                                "Chem Cap",
                                "materials",
                                List.of(
                                    Map.of(
                                        "name",
                                        "Silica",
                                        "amount",
                                        "100",
                                        "analysis",
                                        hugeAnalysis))))))));
    var chem = parsed.recipes().getFirst().draft().lines().getFirst().chemistry();
    assertThat(chem).isNotNull();
    assertThat(chem.analysis()).hasSize(GlazeChemistryJson.MAX_ANALYSIS_ENTRIES);
    assertThat(chem.analysis()).doesNotContainKey("x".repeat(40));
  }
}
