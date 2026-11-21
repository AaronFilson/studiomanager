package io.github.aaronfilson.studiomanager.kiln;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.TestUser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.json.JsonMapper;

/** Plan 09 through the API: dry-run preview, firing match suggestion, commit, and RLS isolation. */
@IntegrationTest
class KilnLogImportApiTests {

  @Autowired MockMvcTester mvc;
  @Autowired JsonMapper json;

  private final TestUser owner = TestUser.named("Olive");
  private final TestUser member = TestUser.named("Mo");
  private final TestUser outsider = TestUser.named("Sora");

  private UUID studioId;
  private UUID otherStudioId;
  private String kilnId;

  @BeforeEach
  void setUp() throws Exception {
    studioId = createStudio(owner, "Kiln Log Studio", "US_CUSTOMARY", "America/Chicago", "US");
    otherStudioId = createStudio(outsider, "Other Clay Studio", "METRIC", "Europe/London", "GB");

    String code = read(post(owner, path("/invites"), "{\"role\": \"MEMBER\"}"), "$.code");
    assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(member.token()))
        .hasStatusOk();

    kilnId =
        read(
            post(owner, path("/kilns"), "{\"name\": \"Skutt 1227\", \"kind\": \"ELECTRIC\"}"),
            "$.id");
  }

  @Test
  void dryRunPreviewWritesNothingAndCommitCreatesCompleteFiringAndIncrementsTotalFirings()
      throws Exception {
    String genesisCsv = fixture("kilns/genesis-sample.csv");

    MvcTestResult preview =
        importLog(
            owner,
            studioId,
            kilnId,
            mapOf("csvText", genesisCsv, "source", "GENESIS", "dryRun", true));

    assertThat(preview)
        .hasStatusOk()
        .bodyJson()
        .hasPathSatisfying("$.committed", c -> assertThat(c).isEqualTo(false))
        .hasPathSatisfying("$.presetUnverified", u -> assertThat(u).isEqualTo(true))
        .hasPathSatisfying("$.tempUnit", u -> assertThat(u).isEqualTo("F"))
        .hasPathSatisfying("$.peakTemp", p -> assertThat(p).isEqualTo(2232.0))
        .hasPathSatisfying("$.durationMinutes", d -> assertThat(d).isEqualTo(570))
        .hasPathSatisfying("$.detectedCone", c -> assertThat(c).isEqualTo("6"))
        .hasPathSatisfying("$.zoneCount", z -> assertThat(z).isEqualTo(3))
        .hasPathSatisfying("$.rowCount", r -> assertThat(r).isEqualTo(18));

    // Preview wrote nothing
    assertThat(mvc.get().uri(path("/kilns/" + kilnId + "/firings")).with(owner.token()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.items.length()")
        .isEqualTo(0);
    assertThat(totalFirings(studioId, owner)).isZero();

    // Commit creates a new COMPLETE firing and counts it against the kiln
    MvcTestResult committed =
        importLog(
            owner,
            studioId,
            kilnId,
            mapOf(
                "csvText", genesisCsv,
                "source", "GENESIS",
                "kind", "GLAZE",
                "cone", "6",
                "dryRun", false));

    assertThat(committed)
        .hasStatusOk()
        .bodyJson()
        .hasPathSatisfying("$.committed", c -> assertThat(c).isEqualTo(true))
        .hasPathSatisfying("$.firing.status", s -> assertThat(s).isEqualTo("COMPLETE"))
        .hasPathSatisfying("$.firing.kind", k -> assertThat(k).isEqualTo("GLAZE"))
        .hasPathSatisfying("$.firing.cone", c -> assertThat(c).isEqualTo("6"))
        .hasPathSatisfying("$.firing.peakTemp", p -> assertThat(p).isEqualTo(2232.0))
        .hasPathSatisfying("$.firing.tempUnit", u -> assertThat(u).isEqualTo("F"))
        .hasPathSatisfying("$.firing.durationMinutes", d -> assertThat(d).isEqualTo(570))
        .hasPathSatisfying("$.firing.logSource", s -> assertThat(s).isEqualTo("GENESIS"))
        .hasPathSatisfying("$.firing.temperatureCurve.length()", n -> assertThat(n).isEqualTo(18));

    assertThat(totalFirings(studioId, owner)).isEqualTo(1);

    String firingId = read(committed, "$.firing.id");
    assertThat(mvc.get().uri(path("/kilns/" + kilnId + "/firings")).with(owner.token()))
        .hasStatusOk()
        .bodyJson()
        .hasPathSatisfying("$.items[0].id", id -> assertThat(id).isEqualTo(firingId))
        .hasPathSatisfying("$.items[0].peakTemp", p -> assertThat(p).isEqualTo(2232.0))
        .hasPathSatisfying("$.items[0].logSource", s -> assertThat(s).isEqualTo("GENESIS"));
  }

  @Test
  void previewSuggestsOverlappingFiringAndCommitAttachesWithoutDoubleCounting() throws Exception {
    String pieceId = read(post(member, path("/pieces"), "{\"size\": {}}"), "$.id");
    String firingId =
        read(
            post(
                owner,
                path("/kilns/" + kilnId + "/firings"),
                "{\"kind\": \"BISQUE\", \"cone\": \"04\", \"pieceIds\": [\"" + pieceId + "\"]}"),
            "$.id");
    assertThat(
            post(
                owner,
                path("/firings/" + firingId + "/transitions"),
                "{\"to\": \"FIRING\", \"expectedVersion\": 0}"))
        .hasStatusOk();
    assertThat(
            post(
                owner,
                path("/firings/" + firingId + "/transitions"),
                "{\"to\": \"COMPLETE\", \"expectedVersion\": 1}"))
        .hasStatusOk();
    assertThat(totalFirings(studioId, owner)).isEqualTo(1);

    // Build a TAP CSV whose timestamps overlap right around now (within +-2 hours)
    Instant now = Instant.now();
    DateTimeFormatter iso = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);
    String overlappingTap =
        "Timestamp,Temp (C),Target (C),Output %,Event\n"
            + iso.format(now.minusSeconds(1800))
            + ",25,25,0,Firing Start\n"
            + iso.format(now.minusSeconds(900))
            + ",650,655,95,\n"
            + iso.format(now)
            + ",1060,1060,90,FTH\n";

    MvcTestResult preview =
        importLog(
            owner,
            studioId,
            kilnId,
            mapOf("csvText", overlappingTap, "source", "TAP", "dryRun", true));

    assertThat(preview)
        .hasStatusOk()
        .bodyJson()
        .hasPathSatisfying("$.suggestedMatch.firingId", id -> assertThat(id).isEqualTo(firingId))
        .hasPathSatisfying("$.errorCode", err -> assertThat(err).isEqualTo("FTH"))
        .hasPathSatisfying("$.tempUnit", u -> assertThat(u).isEqualTo("C"));

    MvcTestResult linked =
        importLog(
            owner,
            studioId,
            kilnId,
            mapOf(
                "csvText", overlappingTap, "source", "TAP", "firingId", firingId, "dryRun", false));

    assertThat(linked)
        .hasStatusOk()
        .bodyJson()
        .hasPathSatisfying("$.committed", c -> assertThat(c).isEqualTo(true))
        .hasPathSatisfying("$.firing.id", id -> assertThat(id).isEqualTo(firingId))
        .hasPathSatisfying("$.firing.peakTemp", p -> assertThat(p).isEqualTo(1060.0))
        .hasPathSatisfying("$.firing.tempUnit", u -> assertThat(u).isEqualTo("C"))
        .hasPathSatisfying("$.firing.errorCode", e -> assertThat(e).isEqualTo("FTH"))
        .hasPathSatisfying("$.firing.logSource", s -> assertThat(s).isEqualTo("TAP"));

    // Attaching to an existing firing does NOT increment totalFirings again or create a duplicate
    assertThat(totalFirings(studioId, owner)).isEqualTo(1);
    assertThat(mvc.get().uri(path("/kilns/" + kilnId + "/firings")).with(owner.token()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.items.length()")
        .isEqualTo(1);
  }

  @Test
  void enforcesPermissionsTenantIsolationAndValidationErrors() throws Exception {
    String genesisCsv = fixture("kilns/genesis-error-sample.csv");

    // Members cannot import kiln logs
    assertThat(
            importLog(
                member,
                studioId,
                kilnId,
                mapOf("csvText", genesisCsv, "source", "GENESIS", "dryRun", true)))
        .hasStatus(HttpStatus.FORBIDDEN);

    // Another studio's owner cannot touch this studio's kiln
    assertThat(
            importLog(
                outsider,
                studioId,
                kilnId,
                mapOf("csvText", genesisCsv, "source", "GENESIS", "dryRun", true)))
        .hasStatus(HttpStatus.FORBIDDEN);

    // Another studio's owner trying to attach a firing from studio A inside studio B gets 404 (RLS)
    String committedFiringId =
        read(
            importLog(
                owner,
                studioId,
                kilnId,
                mapOf("csvText", genesisCsv, "source", "GENESIS", "dryRun", false)),
            "$.firing.id");

    String otherKilnId =
        read(
            post(
                outsider,
                "/api/v1/studios/" + otherStudioId + "/kilns",
                "{\"name\": \"Other Kiln\", \"kind\": \"ELECTRIC\"}"),
            "$.id");

    assertThat(
            importLog(
                outsider,
                otherStudioId,
                otherKilnId,
                mapOf(
                    "csvText",
                    genesisCsv,
                    "source",
                    "GENESIS",
                    "firingId",
                    committedFiringId,
                    "dryRun",
                    false)))
        .hasStatus(HttpStatus.NOT_FOUND);

    // Firing belonging to a second kiln in the same studio is refused with 400
    String secondKilnId =
        read(
            post(owner, path("/kilns"), "{\"name\": \"L&L eQuad\", \"kind\": \"ELECTRIC\"}"),
            "$.id");
    assertThat(
            importLog(
                owner,
                studioId,
                secondKilnId,
                mapOf(
                    "csvText",
                    genesisCsv,
                    "source",
                    "GENESIS",
                    "firingId",
                    committedFiringId,
                    "dryRun",
                    false)))
        .hasStatus(HttpStatus.BAD_REQUEST);

    // Unrecognized CSV returns 400 Problem Details
    assertThat(
            importLog(
                owner,
                studioId,
                kilnId,
                mapOf(
                    "csvText",
                    "Name,Email\nMo,mo@example.test\n",
                    "source",
                    "OTHER",
                    "dryRun",
                    true)))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
  }

  @Test
  void rejectsOversizedRequestPayloadWith413ProblemDetails() {
    String hugeBody =
        "{\"source\":\"OTHER\",\"dryRun\":true,\"csvText\":\""
            + "a".repeat((int) KilnLogImportSizeFilter.MAX_REQUEST_BYTES + 1024)
            + "\"}";
    assertThat(post(owner, path("/kilns/" + kilnId + "/log-imports"), hugeBody))
        .hasStatus(HttpStatus.CONTENT_TOO_LARGE)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
  }

  @Test
  void refusesAttachingToLoadingOrCancelledFiringAndExcludesThemFromOverlapMatch()
      throws Exception {
    Instant now = Instant.now();
    DateTimeFormatter iso = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);
    String overlappingCsv =
        "Timestamp,Temp (C)\n"
            + iso.format(now.minusSeconds(1800))
            + ",25\n"
            + iso.format(now)
            + ",1060\n";

    String pieceId = read(post(member, path("/pieces"), "{\"size\": {}}"), "$.id");
    String loadingId =
        read(
            post(
                owner,
                path("/kilns/" + kilnId + "/firings"),
                "{\"kind\": \"BISQUE\", \"cone\": \"04\", \"pieceIds\": [\"" + pieceId + "\"]}"),
            "$.id");

    // LOADING firing is excluded from overlap suggestion and refused on explicit attach
    assertThat(
            importLog(
                owner,
                studioId,
                kilnId,
                mapOf("csvText", overlappingCsv, "source", "TAP", "dryRun", true)))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.suggestedMatch")
        .isNull();
    assertThat(
            importLog(
                owner,
                studioId,
                kilnId,
                mapOf(
                    "csvText",
                    overlappingCsv,
                    "source",
                    "TAP",
                    "firingId",
                    loadingId,
                    "dryRun",
                    false)))
        .hasStatus(HttpStatus.BAD_REQUEST);

    // Transition to CANCELLED: still excluded from overlap suggestion and refused on attach
    assertThat(
            post(
                owner,
                path("/firings/" + loadingId + "/transitions"),
                "{\"to\": \"CANCELLED\", \"expectedVersion\": 0}"))
        .hasStatusOk();
    assertThat(
            importLog(
                owner,
                studioId,
                kilnId,
                mapOf("csvText", overlappingCsv, "source", "TAP", "dryRun", true)))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.suggestedMatch")
        .isNull();
    assertThat(
            importLog(
                owner,
                studioId,
                kilnId,
                mapOf(
                    "csvText",
                    overlappingCsv,
                    "source",
                    "TAP",
                    "firingId",
                    loadingId,
                    "dryRun",
                    false)))
        .hasStatus(HttpStatus.BAD_REQUEST);

    // Start a fresh FIRING load: attaching telemetry while FIRING succeeds
    String activeId =
        read(
            post(
                owner,
                path("/kilns/" + kilnId + "/firings"),
                "{\"kind\": \"BISQUE\", \"cone\": \"04\", \"pieceIds\": [\"" + pieceId + "\"]}"),
            "$.id");
    assertThat(
            post(
                owner,
                path("/firings/" + activeId + "/transitions"),
                "{\"to\": \"FIRING\", \"expectedVersion\": 0}"))
        .hasStatusOk();
    assertThat(
            importLog(
                owner,
                studioId,
                kilnId,
                mapOf(
                    "csvText",
                    overlappingCsv,
                    "source",
                    "TAP",
                    "firingId",
                    activeId,
                    "dryRun",
                    false)))
        .hasStatusOk()
        .bodyJson()
        .hasPathSatisfying("$.firing.id", id -> assertThat(id).isEqualTo(activeId))
        .hasPathSatisfying("$.firing.peakTemp", p -> assertThat(p).isEqualTo(1060.0));
  }

  @Test
  void refusesDuplicateTimestampedImportWithoutDoubleCountingTotalFirings() throws Exception {
    String genesisCsv = fixture("kilns/genesis-sample.csv");

    assertThat(
            importLog(
                owner,
                studioId,
                kilnId,
                mapOf("csvText", genesisCsv, "source", "GENESIS", "dryRun", false)))
        .hasStatusOk();
    assertThat(totalFirings(studioId, owner)).isEqualTo(1);

    // Importing the exact same timestamped log again with firingId == null returns 409 Conflict
    assertThat(
            importLog(
                owner,
                studioId,
                kilnId,
                mapOf("csvText", genesisCsv, "source", "GENESIS", "dryRun", false)))
        .hasStatus(HttpStatus.CONFLICT)
        .bodyJson()
        .extractingPath("$.detail")
        .asString()
        .contains("already been imported");
    assertThat(totalFirings(studioId, owner)).isEqualTo(1);
  }

  @Test
  void downsamplesListCurvesTo32PointsWhileDetailReturnsFullCurveAndOrdersChronologically()
      throws Exception {
    // Newer firing (2026-11-10) with 120 data points imported first
    StringBuilder newerCsv = new StringBuilder("Timestamp,Temp (F)\n");
    Instant newerStart = Instant.parse("2026-11-10T10:00:00Z");
    DateTimeFormatter iso = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);
    for (int i = 0; i < 120; i++) {
      double temp = i == 80 ? 2240.0 : (70.0 + i * 15.0);
      newerCsv
          .append(iso.format(newerStart.plusSeconds(i * 60L)))
          .append(',')
          .append(temp)
          .append('\n');
    }
    MvcTestResult newerCommitted =
        importLog(
            owner,
            studioId,
            kilnId,
            mapOf("csvText", newerCsv.toString(), "source", "OTHER", "dryRun", false));
    assertThat(newerCommitted).hasStatusOk();
    String newerId = read(newerCommitted, "$.firing.id");
    assertThat(newerCommitted)
        .bodyJson()
        .extractingPath("$.firing.temperatureCurve.length()")
        .isEqualTo(120);

    // Older historical firing (2026-09-01) imported AFTER the newer one
    String olderCsv =
        "Timestamp,Temp (F)\n"
            + "2026-09-01T10:00:00Z,70\n"
            + "2026-09-01T14:00:00Z,1900\n";
    MvcTestResult olderCommitted =
        importLog(
            owner,
            studioId,
            kilnId,
            mapOf("csvText", olderCsv, "source", "OTHER", "dryRun", false));
    assertThat(olderCommitted).hasStatusOk();
    String olderId = read(olderCommitted, "$.firing.id");

    // GET /firings/{id} returns the full 120-point curve
    assertThat(mvc.get().uri(path("/firings/" + newerId)).with(owner.token()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.temperatureCurve.length()")
        .isEqualTo(120);

    // GET /kilns/{kilnId}/firings orders chronologically (newer before older) and downsamples
    // curves to <= 32 sparkline points while preserving start, peak, and end.
    MvcTestResult listed =
        mvc.get().uri(path("/kilns/" + kilnId + "/firings")).with(owner.token()).exchange();
    assertThat(listed)
        .hasStatusOk()
        .bodyJson()
        .hasPathSatisfying("$.items[0].id", id -> assertThat(id).isEqualTo(newerId))
        .hasPathSatisfying("$.items[1].id", id -> assertThat(id).isEqualTo(olderId));
    String listedBody = listed.getResponse().getContentAsString();
    List<Number> sparkElapsed =
        JsonPath.read(listedBody, "$.items[0].temperatureCurve[*].elapsedMinutes");
    assertThat(sparkElapsed).hasSizeLessThanOrEqualTo(32);
    assertThat(sparkElapsed.getFirst().doubleValue()).isEqualTo(0.0);
    assertThat(sparkElapsed.getLast().doubleValue()).isEqualTo(119.0);
    List<Number> sparkTemps = JsonPath.read(listedBody, "$.items[0].temperatureCurve[*].temp");
    assertThat(sparkTemps).anySatisfy(t -> assertThat(t.doubleValue()).isEqualTo(2240.0));
  }

  private MvcTestResult importLog(
      TestUser user, UUID targetStudioId, String targetKilnId, Map<String, Object> payload) {
    return post(
        user,
        "/api/v1/studios/" + targetStudioId + "/kilns/" + targetKilnId + "/log-imports",
        json.writeValueAsString(payload));
  }

  private int totalFirings(UUID targetStudioId, TestUser user) throws Exception {
    MvcTestResult result =
        mvc.get().uri("/api/v1/studios/" + targetStudioId + "/kilns").with(user.token()).exchange();
    assertThat(result).hasStatusOk();
    return JsonPath.read(result.getResponse().getContentAsString(), "$[0].totalFirings");
  }

  private UUID createStudio(
      TestUser user, String name, String unitSystem, String timeZone, String country)
      throws Exception {
    MvcTestResult result =
        post(
            user,
            "/api/v1/studios",
            """
            {"name": "%s", "unitSystem": "%s", "timeZone": "%s", "country": "%s"}
            """
                .formatted(name, unitSystem, timeZone, country));
    assertThat(result).hasStatus(HttpStatus.CREATED);
    return UUID.fromString(read(result, "$.id"));
  }

  private MvcTestResult post(TestUser user, String uri, String body) {
    return mvc.post()
        .uri(uri)
        .with(user.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .exchange();
  }

  private String path(String suffix) {
    return "/api/v1/studios/" + studioId + suffix;
  }

  private static Map<String, Object> mapOf(Object... pairs) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      map.put((String) pairs[i], pairs[i + 1]);
    }
    return map;
  }

  private static String fixture(String path) throws IOException {
    return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
  }

  private static String read(MvcTestResult result, String jsonPath) throws Exception {
    return JsonPath.read(result.getResponse().getContentAsString(), jsonPath).toString();
  }
}
