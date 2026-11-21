package io.github.aaronfilson.studiomanager.glaze;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.TestUser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * End-to-end API integration tests for Plan 14 steps 3–5: studio glaze mix log & SG warnings,
 * Glazecalc/glaze-vault import + dedup + 413 size limit + export, direct-to-blob test-tile photos +
 * abandoned sweep, and promoting shared member recipes.
 */
@IntegrationTest
class GlazeMixAndPhotoApiTests {

  private static final byte[] JPEG = {
    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F', 0
  };

  private final HttpClient http = HttpClient.newHttpClient();

  @Autowired MockMvcTester mvc;

  @Autowired GlazePhotoUploadSweep sweep;

  private final TestUser owner = TestUser.named("Olive");
  private final TestUser mo = TestUser.named("Mo");
  private final TestUser pat = TestUser.named("Pat");
  private UUID studioId;

  @BeforeEach
  void setUp() {
    studioId =
        UUID.fromString(
            read(
                post(
                    owner,
                    "/api/v1/studios",
                    """
                    {"name": "Glaze Lab", "unitSystem": "METRIC", "timeZone": "America/Chicago", "country": "US"}
                    """),
                "$.id"));
    join(mo);
    join(pat);
  }

  @Test
  void studioGlazeMixLogWarnsWhenSpecificGravityIsOffTarget() {
    String studioGlazeId =
        read(
            post(
                owner,
                path("/glazes"),
                """
                {"ownerKind": "STUDIO", "name": "Studio Clear", "cone": "6", "targetSpecificGravity": "1.45",
                 "lines": [{"kind": "BASE", "material": "Ferro Frit 3124", "amount": "50", "unit": "PARTS"},
                           {"kind": "BASE", "material": "EPK", "amount": "25", "unit": "PARTS"},
                           {"kind": "BASE", "material": "Silica", "amount": "25", "unit": "PARTS"}]}
                """),
            "$.id");

    // Members cannot log mixes
    assertThat(
            post(
                mo,
                path("/glazes/" + studioGlazeId + "/mixes"),
                "{\"dryWeightG\": \"5000\", \"waterG\": \"3800\"}"))
        .hasStatus(HttpStatus.FORBIDDEN);

    // Staff logs a mix with an on-target initial reading (trailing zeros past DB scale allowed)
    MvcTestResult mixed =
        post(
            owner,
            path("/glazes/" + studioGlazeId + "/mixes"),
            """
            {"dryWeightG": "10000.000", "waterG": "7500.000", "specificGravity": "1.4600", "notes": "Bucket #1"}
            """);
    assertThat(mixed).hasStatus(HttpStatus.CREATED);
    assertThat(mixed).bodyJson().extractingPath("$.readings[0].offTarget").isEqualTo(false);
    String mixId = read(mixed, "$.id");

    // Adding an off-target reading (> ±0.03 from 1.45) produces an actionable warning
    MvcTestResult offTarget =
        post(
            owner,
            path("/glazes/" + studioGlazeId + "/mixes/" + mixId + "/readings"),
            """
            {"specificGravity": "1.52", "note": "Evaporated over the weekend"}
            """);
    assertThat(offTarget).hasStatus(HttpStatus.CREATED);
    assertThat(offTarget).bodyJson().extractingPath("$.offTarget").isEqualTo(true);
    assertThat(offTarget)
        .bodyJson()
        .extractingPath("$.warning")
        .asString()
        .contains("1.52")
        .contains("1.45");

    // Members can read the mix log
    MvcTestResult listed = get(mo, path("/glazes/" + studioGlazeId + "/mixes"));
    assertThat(listed).hasStatusOk();
    assertThat(listed).bodyJson().extractingPath("$[0].readings.length()").isEqualTo(2);

    // Member recipes cannot have a mix log
    String memberRecipeId =
        read(
            post(
                mo,
                path("/glazes"),
                """
                {"ownerKind": "MEMBER", "name": "My Test", "sharedWithStudio": true,
                 "lines": [{"kind": "BASE", "material": "Silica", "amount": "100", "unit": "PARTS"}]}
                """),
            "$.id");
    assertThat(
            post(owner, path("/glazes/" + memberRecipeId + "/mixes"), "{\"dryWeightG\": \"1000\"}"))
        .hasStatus(HttpStatus.BAD_REQUEST);
  }

  @Test
  void importsDeduplicatesExportsAndRejectsOversizedFiles() {
    String glazecalcExport =
        """
        {
          "format": "glazecalc-export",
          "version": 1,
          "records": {
            "recipes": [
              {
                "_id": "gc-1",
                "title": "Floating Blue",
                "rev": 1,
                "firedAt": "Cone 6 oxidation",
                "foodSurface": true,
                "sgTarget": 1.47,
                "notes": ["<p>Test <b>note</b></p><script>bad()</script>"],
                "materials": [
                  {"_id": "m1", "name": "Nepheline Syenite", "amount": "42.50", "loi": 0.7,
                   "analysis": {"SiO2": 60.7, "Al2O3": 23.3, "Na2O": 9.8, "K2O": 4.6}},
                  {"name": "Gerstley Borate", "amount": "27.5"},
                  {"name": "Silica", "amount": "20"},
                  {"name": "EPK", "amount": "10"}
                ],
                "additives": [
                  {"name": "Cobalt Oxide", "amount": "1", "unit": "percent"}
                ]
              }
            ]
          }
        }
        """;

    // Dry run writes nothing
    MvcTestResult dryRun = post(mo, path("/glazes/import?dryRun=true"), glazecalcExport);
    assertThat(dryRun).hasStatusOk();
    assertThat(dryRun).bodyJson().extractingPath("$.committed").isEqualTo(false);
    assertThat(dryRun).bodyJson().extractingPath("$.createdCount").isEqualTo(1);
    assertThat(dryRun).bodyJson().extractingPath("$.rows[0].outcome").isEqualTo("NEW");
    assertThat(get(mo, path("/glazes?owner=MINE")))
        .bodyJson()
        .extractingPath("$.length()")
        .isEqualTo(0);

    // Commit creates the recipe with stripped notes and preserved chemistry
    MvcTestResult committed = post(mo, path("/glazes/import?dryRun=false"), glazecalcExport);
    assertThat(committed).hasStatusOk();
    assertThat(committed).bodyJson().extractingPath("$.committed").isEqualTo(true);
    String importedId = read(committed, "$.rows[0].recipeId");

    MvcTestResult loaded = get(mo, path("/glazes/" + importedId));
    assertThat(loaded).hasStatusOk();
    assertThat(loaded).bodyJson().extractingPath("$.source").isEqualTo("GLAZECALC");
    assertThat(loaded).bodyJson().extractingPath("$.notes").isEqualTo("Test note");
    assertThat(loaded)
        .bodyJson()
        .extractingPath("$.lines[0].chemistry.analysis.SiO2")
        .isEqualTo(60.7);
    assertThat(loaded).bodyJson().extractingPath("$.lines[0].amountText").isEqualTo("42.50");

    // Re-importing the exact same export is UNCHANGED and creates no duplicate
    MvcTestResult unchanged = post(mo, path("/glazes/import?dryRun=false"), glazecalcExport);
    assertThat(unchanged).hasStatusOk();
    assertThat(unchanged).bodyJson().extractingPath("$.unchangedCount").isEqualTo(1);
    assertThat(get(mo, path("/glazes?owner=MINE")))
        .bodyJson()
        .extractingPath("$.length()")
        .isEqualTo(1);

    // Re-importing a newer revision updates the existing recipe in place
    String updatedExport =
        glazecalcExport
            .replace("\"rev\": 1", "\"rev\": 2")
            .replace("Floating Blue", "Floating Blue v2");
    MvcTestResult updated = post(mo, path("/glazes/import?dryRun=false"), updatedExport);
    assertThat(updated).hasStatusOk();
    assertThat(updated).bodyJson().extractingPath("$.updatedCount").isEqualTo(1);
    assertThat(updated).bodyJson().extractingPath("$.rows[0].recipeId").isEqualTo(importedId);
    assertThat(get(mo, path("/glazes/" + importedId)))
        .bodyJson()
        .extractingPath("$.name")
        .isEqualTo("Floating Blue v2");

    // Export as glazecalc-export v1 round-trips cleanly
    MvcTestResult exported = get(mo, path("/glazes/" + importedId + "/export"));
    assertThat(exported).hasStatusOk();
    assertThat(exported).bodyJson().extractingPath("$.format").isEqualTo("glazecalc-export");
    assertThat(exported).bodyJson().extractingPath("$.version").isEqualTo(1);
    assertThat(exported)
        .bodyJson()
        .extractingPath("$.records.recipes[0].title")
        .isEqualTo("Floating Blue v2");

    // Oversized (> 2 MB) body is rejected with 413 ProblemDetail before parsing
    byte[] oversized =
        ("{\"format\":\"glazecalc-export\",\"version\":1,\"pad\":\""
                + "x".repeat(GlazeImportParser.MAX_IMPORT_BYTES + 1024)
                + "\"}")
            .getBytes(StandardCharsets.UTF_8);
    assertThat(
            mvc.post()
                .uri(path("/glazes/import?dryRun=true"))
                .with(mo.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(oversized))
        .hasStatus(HttpStatus.CONTENT_TOO_LARGE)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
  }

  @Test
  void testTilePhotosUploadSweepAndPromoteWorkEndToEnd() throws Exception {
    // Member creates and shares a recipe
    String memberGlazeId =
        read(
            post(
                mo,
                path("/glazes"),
                """
                {"ownerKind": "MEMBER", "name": "Mo Celadon", "cone": "10", "atmosphere": "REDUCTION",
                 "sharedWithStudio": true, "targetSpecificGravity": "1.45",
                 "lines": [{"kind": "BASE", "material": "Custer Feldspar", "amount": "40", "unit": "PARTS"},
                           {"kind": "BASE", "material": "Silica", "amount": "30", "unit": "PARTS"},
                           {"kind": "BASE", "material": "Whiting", "amount": "20", "unit": "PARTS"},
                           {"kind": "BASE", "material": "EPK", "amount": "10", "unit": "PARTS"}]}
                """),
            "$.id");

    // Member uploads a test-tile photo straight to Azurite
    MvcTestResult started = post(mo, path("/glazes/" + memberGlazeId + "/photos"), "");
    assertThat(started).hasStatus(HttpStatus.CREATED);
    String photoId = read(started, "$.photoId");
    assertThat(put(read(started, "$.photoUploadUrl"), JPEG)).isEqualTo(201);
    assertThat(put(read(started, "$.thumbnailUploadUrl"), JPEG)).isEqualTo(201);

    MvcTestResult completed =
        post(mo, path("/glazes/" + memberGlazeId + "/photos/" + photoId + "/complete"), "");
    assertThat(completed).hasStatusOk();
    assertThat(completed).bodyJson().extractingPath("$.photos[0].id").isEqualTo(photoId);
    String url = read(completed, "$.photos[0].url");
    HttpResponse<byte[]> image =
        http.send(
            HttpRequest.newBuilder(URI.create(url)).build(),
            HttpResponse.BodyHandlers.ofByteArray());
    assertThat(image.statusCode()).isEqualTo(200);
    assertThat(image.body()).isEqualTo(JPEG);

    // Another member or staff cannot upload or delete Mo's photo
    assertThat(post(pat, path("/glazes/" + memberGlazeId + "/photos"), ""))
        .hasStatus(HttpStatus.FORBIDDEN);
    assertThat(
            mvc.delete()
                .uri(path("/glazes/" + memberGlazeId + "/photos/" + photoId))
                .with(owner.token()))
        .hasStatus(HttpStatus.FORBIDDEN);

    // Abandoned upload is cleaned by GlazePhotoUploadSweep (acting as PHOTO_CLEANUP)
    String abandonedId =
        read(post(mo, path("/glazes/" + memberGlazeId + "/photos"), ""), "$.photoId");
    assertThat(
            sweep.runAt(Instant.now().plus(GlazePhotoUploadSweep.ABANDONED_AFTER).plusSeconds(60)))
        .isGreaterThanOrEqualTo(1);
    assertThat(
            post(mo, path("/glazes/" + memberGlazeId + "/photos/" + abandonedId + "/complete"), ""))
        .hasStatus(HttpStatus.NOT_FOUND);

    // Owner deletes their own photo
    assertThat(
            mvc.delete()
                .uri(path("/glazes/" + memberGlazeId + "/photos/" + photoId))
                .with(mo.token()))
        .hasStatus(HttpStatus.NO_CONTENT);

    // Staff promotes Mo's shared recipe to a studio glaze; Mo cannot promote
    assertThat(post(mo, path("/glazes/" + memberGlazeId + "/promote"), ""))
        .hasStatus(HttpStatus.FORBIDDEN);
    MvcTestResult promoted = post(owner, path("/glazes/" + memberGlazeId + "/promote"), "");
    assertThat(promoted).hasStatus(HttpStatus.CREATED);
    assertThat(promoted).bodyJson().extractingPath("$.ownerKind").isEqualTo("STUDIO");
    assertThat(promoted).bodyJson().extractingPath("$.source").isEqualTo("MANUAL");
    assertThat(promoted).bodyJson().extractingPath("$.promotedFrom").isEqualTo(memberGlazeId);
    assertThat(promoted).bodyJson().extractingPath("$.name").isEqualTo("Mo Celadon");
    // Original member copy stays untouched
    assertThat(get(mo, path("/glazes/" + memberGlazeId)))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.ownerKind")
        .isEqualTo("MEMBER");

    // Deleting a recipe with an uploaded photo also removes its blobs after commit
    MvcTestResult secondUpload = post(mo, path("/glazes/" + memberGlazeId + "/photos"), "");
    String secondPhotoId = read(secondUpload, "$.photoId");
    assertThat(put(read(secondUpload, "$.photoUploadUrl"), JPEG)).isEqualTo(201);
    assertThat(put(read(secondUpload, "$.thumbnailUploadUrl"), JPEG)).isEqualTo(201);
    MvcTestResult secondCompleted =
        post(mo, path("/glazes/" + memberGlazeId + "/photos/" + secondPhotoId + "/complete"), "");
    assertThat(secondCompleted).hasStatusOk();
    String secondPhotoUrl = read(secondCompleted, "$.photos[0].url");
    assertThat(
            http.send(
                    HttpRequest.newBuilder(URI.create(secondPhotoUrl)).build(),
                    HttpResponse.BodyHandlers.discarding())
                .statusCode())
        .isEqualTo(200);

    assertThat(mvc.delete().uri(path("/glazes/" + memberGlazeId)).with(mo.token()))
        .hasStatus(HttpStatus.NO_CONTENT);
    assertThat(
            http.send(
                    HttpRequest.newBuilder(URI.create(secondPhotoUrl)).build(),
                    HttpResponse.BodyHandlers.discarding())
                .statusCode())
        .isEqualTo(404);
  }

  private int put(String url, byte[] body) throws Exception {
    return http.send(
            HttpRequest.newBuilder(URI.create(url))
                .header("x-ms-blob-type", "BlockBlob")
                .header("Content-Type", "image/jpeg")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(body))
                .build(),
            HttpResponse.BodyHandlers.discarding())
        .statusCode();
  }

  private void join(TestUser who) {
    String code = read(post(owner, path("/invites"), "{\"role\": \"MEMBER\"}"), "$.code");
    assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(who.token()))
        .hasStatusOk();
  }

  private MvcTestResult get(TestUser user, String uri) {
    return mvc.get().uri(uri).with(user.token()).exchange();
  }

  private MvcTestResult post(TestUser user, String uri, String body) {
    return body.isEmpty()
        ? mvc.post().uri(uri).with(user.token()).exchange()
        : mvc.post()
            .uri(uri)
            .with(user.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .exchange();
  }

  private String path(String suffix) {
    return "/api/v1/studios/" + studioId + suffix;
  }

  private static String read(MvcTestResult result, String jsonPath) {
    try {
      return JsonPath.read(result.getResponse().getContentAsString(), jsonPath).toString();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
