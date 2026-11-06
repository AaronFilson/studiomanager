package io.github.aaronfilson.studiomanager.glaze;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.TestUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** The glaze book through the API, with row-level security doing the hiding. */
@IntegrationTest
class GlazeApiTests {

    @Autowired
    MockMvcTester mvc;

    private final TestUser owner = TestUser.named("Olive");
    private final TestUser mo = TestUser.named("Mo");
    private final TestUser pat = TestUser.named("Pat");
    private UUID studioId;

    private static final String TENMOKU = """
            {"name": " Tenmoku ", "cone": "6", "atmosphere": "OXIDATION", "foodSurface": true,
             "lines": [
               {"kind": "BASE", "material": "Custer Feldspar", "amount": "45", "unit": "PARTS"},
               {"kind": "BASE", "material": "Silica", "amount": "30", "unit": "PARTS"},
               {"kind": "BASE", "material": "Whiting", "amount": "25", "unit": "PARTS"},
               {"kind": "ADDITION", "material": "Red Iron Oxide", "amount": "10.50", "unit": "PERCENT"}
             ]}
            """;

    @BeforeEach
    void setUp() {
        studioId = UUID.fromString(read(post(owner, "/api/v1/studios", """
                {"name": "Clay Club", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
                """), "$.id"));
        join(mo);
        join(pat);
    }

    @Test
    void aMembersRecipeIsHiddenFromEveryoneElseUntilShared() {
        MvcTestResult created = post(mo, path("/glazes"), TENMOKU);
        assertThat(created).hasStatus(HttpStatus.CREATED);
        assertThat(created).bodyJson().extractingPath("$.name").isEqualTo("Tenmoku");
        assertThat(created).bodyJson().extractingPath("$.ownerKind").isEqualTo("MEMBER");
        assertThat(created).bodyJson().extractingPath("$.ownerName").isEqualTo("Mo");
        assertThat(created).bodyJson().extractingPath("$.lines[3].amount").isEqualTo("10.5");
        assertThat(created).bodyJson().extractingPath("$.canEdit").isEqualTo(true);
        String id = read(created, "$.id");

        assertThat(get(mo, path("/glazes?owner=MINE"))).bodyJson().extractingPath("$.length()").isEqualTo(1);
        // Not even the owner of the studio sees it, by list or by id
        assertThat(get(owner, path("/glazes?owner=SHARED"))).bodyJson().extractingPath("$.length()").isEqualTo(0);
        assertThat(get(owner, path("/glazes/" + id))).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(get(pat, path("/glazes/" + id))).hasStatus(HttpStatus.NOT_FOUND);

        share(mo, id, 1);

        assertThat(get(pat, path("/glazes?owner=SHARED"))).bodyJson().extractingPath("$[0].name").isEqualTo("Tenmoku");
        assertThat(get(mo, path("/glazes?owner=SHARED"))).bodyJson().extractingPath("$.length()").isEqualTo(0);
        MvcTestResult seen = get(owner, path("/glazes/" + id));
        assertThat(seen).hasStatusOk();
        assertThat(seen).bodyJson().extractingPath("$.canEdit").isEqualTo(false);
        // Seeing a shared recipe isn't owning it, for staff either
        assertThat(put(owner, path("/glazes/" + id), withVersion(TENMOKU, 2))).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.delete().uri(path("/glazes/" + id)).with(pat.token())).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void onlyStaffKeepTheStudioGlazes() {
        String studioGlaze = TENMOKU.replace("{\"name\"", "{\"ownerKind\": \"STUDIO\", \"name\"");
        assertThat(post(mo, path("/glazes"), studioGlaze)).hasStatus(HttpStatus.FORBIDDEN);

        MvcTestResult created = post(owner, path("/glazes"), studioGlaze);
        assertThat(created).hasStatus(HttpStatus.CREATED);
        String id = read(created, "$.id");
        assertThat(created).bodyJson().extractingPath("$.ownerKind").isEqualTo("STUDIO");

        MvcTestResult seen = get(mo, path("/glazes?owner=STUDIO"));
        assertThat(seen).bodyJson().extractingPath("$[0].id").isEqualTo(id);
        assertThat(seen).bodyJson().extractingPath("$[0].canEdit").isEqualTo(false);
        assertThat(put(mo, path("/glazes/" + id), withVersion(studioGlaze, 1))).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(put(owner, path("/glazes/" + id), withVersion(studioGlaze.replace("Tenmoku", "Studio Tenmoku"), 1)))
                .hasStatusOk().bodyJson().extractingPath("$.name").isEqualTo("Studio Tenmoku");
        assertThat(mvc.delete().uri(path("/glazes/" + id)).with(owner.token())).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void aStaleChangeIsRefused() {
        String id = read(post(mo, path("/glazes"), TENMOKU), "$.id");
        assertThat(put(mo, path("/glazes/" + id), withVersion(TENMOKU.replace("Tenmoku", "Kaki"), 1))).hasStatusOk();
        assertThat(put(mo, path("/glazes/" + id), withVersion(TENMOKU, 1))).hasStatus(HttpStatus.CONFLICT);
        assertThat(get(mo, path("/glazes/" + id))).bodyJson().extractingPath("$.name").isEqualTo("Kaki");
    }

    @Test
    void scalesARecipeToABucket() {
        String id = read(post(mo, path("/glazes"), TENMOKU), "$.id");
        MvcTestResult batch = get(mo, path("/glazes/" + id + "/batch?size=10&unit=KILOGRAM"));
        assertThat(batch).hasStatusOk();
        assertThat(batch).bodyJson().extractingPath("$.lines[0].weight").isEqualTo("4.500");
        assertThat(batch).bodyJson().extractingPath("$.lines[3].weight").isEqualTo("1.050");
        assertThat(batch).bodyJson().extractingPath("$.total").isEqualTo("11.050");
        // Someone else's unshared recipe can't be scaled either
        assertThat(get(pat, path("/glazes/" + id + "/batch?size=10&unit=KILOGRAM"))).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(get(mo, path("/glazes/" + id + "/batch?size=0&unit=GRAM"))).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void aBaseLineIsInParts() {
        assertThat(post(mo, path("/glazes"), TENMOKU.replace("\"30\", \"unit\": \"PARTS\"", "\"30\", \"unit\": \"PERCENT\"")))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    private void share(TestUser who, String id, int version) {
        assertThat(put(who, path("/glazes/" + id), withVersion(TENMOKU.replace("\"foodSurface\": true",
                "\"foodSurface\": true, \"sharedWithStudio\": true"), version))).hasStatusOk();
    }

    private static String withVersion(String body, int version) {
        return body.replaceFirst("\\{", "{\"version\": " + version + ", ");
    }

    private void join(TestUser who) {
        String code = read(post(owner, path("/invites"), "{\"role\": \"MEMBER\"}"), "$.code");
        assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(who.token())).hasStatusOk();
    }

    private MvcTestResult get(TestUser user, String uri) {
        return mvc.get().uri(uri).with(user.token()).exchange();
    }

    private MvcTestResult post(TestUser user, String uri, String body) {
        return mvc.post().uri(uri).with(user.token()).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    private MvcTestResult put(TestUser user, String uri, String body) {
        return mvc.put().uri(uri).with(user.token()).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
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
