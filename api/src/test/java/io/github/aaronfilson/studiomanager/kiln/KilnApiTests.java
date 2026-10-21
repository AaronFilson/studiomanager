package io.github.aaronfilson.studiomanager.kiln;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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

/** M2 through the API: kiln queues with caps, loads, unloading, batch notices and firing counts. */
@IntegrationTest
class KilnApiTests {

    @Autowired
    MockMvcTester mvc;

    private final TestUser owner = TestUser.named("Olive");
    private final TestUser maker = TestUser.named("Mo");
    private final TestUser other = TestUser.named("Nia");
    private UUID studioId;

    @BeforeEach
    void studio() throws Exception {
        studioId = UUID.fromString(read(post(owner, "/api/v1/studios", """
                {"name": "Kiln Club", "unitSystem": "METRIC", "timeZone": "Europe/London", "country": "GB"}
                """), "$.id"));
        for (TestUser member : List.of(maker, other)) {
            String code = read(post(owner, path("/invites"), "{\"role\": \"MEMBER\"}"), "$.code");
            assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(member.token())).hasStatusOk();
        }
    }

    @Test
    void theQueueCapsMembersAndALoadMustRespectIt() throws Exception {
        String kiln = kiln("""
                {"name": "Skutt 1027", "kind": "ELECTRIC", "maxPiecesPerMember": 1}
                """);
        String first = piece(maker);
        String second = piece(maker);
        String theirs = piece(other);

        assertThat(mvc.get().uri(path("/kilns/" + kiln + "/queue?kind=BISQUE")).with(owner.token()))
                .hasStatusOk().bodyJson()
                .hasPathSatisfying("$[*].piece.id", ids -> assertThat(ids).asArray().containsExactly(first, second, theirs))
                .hasPathSatisfying("$[*].fitsNextLoad", fits -> assertThat(fits).asArray().containsExactly(true, false, true));

        assertThat(load(kiln, "BISQUE", first, second)).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(load(kiln, "GLAZE", first)).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(load(kiln, "BISQUE", first, theirs)).hasStatus(HttpStatus.CREATED);
        // One open load per kiln
        assertThat(load(kiln, "BISQUE", second)).hasStatus(HttpStatus.CONFLICT);
        // Loaded pieces leave the queue
        assertThat(mvc.get().uri(path("/kilns/" + kiln + "/queue?kind=BISQUE")).with(owner.token()))
                .hasStatusOk().bodyJson().extractingPath("$[*].piece.id").asArray().containsExactly(second);
        // Members can't run kilns
        assertThat(load(kiln, "BISQUE", second)).hasStatus(HttpStatus.CONFLICT);
        assertThat(mvc.post().uri(path("/kilns/" + kiln + "/firings")).with(maker.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"kind\": \"BISQUE\", \"pieceIds\": [\"" + second + "\"]}"))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void unloadingMovesPiecesOnAndSendsEachMakerOneNoticeForTheBatch() throws Exception {
        String kiln = kiln("""
                {"name": "Big gas", "kind": "GAS"}
                """);
        String jar = piece(maker, "Jar");
        String bowl = piece(maker, "Bowl");
        String mug = piece(other, "Mug");

        fire(kiln, "BISQUE", jar, bowl, mug);
        assertThat(stageOf(jar)).isEqualTo("BISQUE_FIRED");
        // Bisque notices say the work is ready to glaze
        assertThat(mvc.get().uri(path("/notifications")).with(maker.token()))
                .hasStatusOk().bodyJson()
                .hasPathSatisfying("$.length()", n -> assertThat(n).isEqualTo(1))
                .hasPathSatisfying("$[0].title", t -> assertThat(t).isEqualTo("Your 2 pieces are bisque fired and ready to glaze"));

        for (String piece : List.of(jar, bowl, mug)) {
            assertThat(mvc.post().uri(path("/pieces/" + piece + "/moves")).with(owner.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"to\": \"GLAZED\", \"expectedVersion\": " + versionOf(piece) + "}")).hasStatusOk();
        }
        fire(kiln, "GLAZE", jar, bowl, mug);

        assertThat(stageOf(jar)).isEqualTo("READY");
        MvcTestResult inbox = mvc.get().uri(path("/notifications")).with(maker.token()).exchange();
        assertThat(inbox).hasStatusOk().bodyJson()
                .hasPathSatisfying("$.length()", n -> assertThat(n).isEqualTo(2))
                .hasPathSatisfying("$[0].kind", k -> assertThat(k).isEqualTo("PIECES_READY"))
                .hasPathSatisfying("$[0].title", t -> assertThat(t).isEqualTo("Your 2 pieces are ready to collect"));
        assertThat(read(inbox, "$[0].body")).contains(": Jar").contains(": Bowl").contains("Please collect by");
        assertThat(mvc.get().uri(path("/notifications")).with(other.token()))
                .hasStatusOk().bodyJson().extractingPath("$[0].title").isEqualTo("Your piece is ready to collect");

        // Each completed firing is counted; maintenance tasks read the count (MaintenanceApiTests)
        assertThat(mvc.get().uri(path("/kilns")).with(owner.token())).bodyJson()
                .extractingPath("$[0].totalFirings").isEqualTo(2);
    }

    @Test
    void aKilnThatIsDownCantBeLoadedAndACancelledLoadReturnsItsPieces() throws Exception {
        String kiln = kiln("""
                {"name": "Test kiln", "kind": "ELECTRIC"}
                """);
        String jar = piece(maker);

        String firing = read(load(kiln, "BISQUE", jar), "$.id");
        assertThat(post(owner, path("/firings/" + firing + "/transitions"), """
                {"to": "CANCELLED", "notes": "Element failed", "expectedVersion": 0}
                """)).hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("CANCELLED");
        assertThat(stageOf(jar)).isEqualTo("RECEIVED");

        assertThat(mvc.put().uri(path("/kilns/" + kiln + "/availability")).with(owner.token())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"downReason": "Waiting for elements", "expectedVersion": 0}
                        """)).hasStatusOk().bodyJson().extractingPath("$.available").isEqualTo(false);
        assertThat(load(kiln, "BISQUE", jar)).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void staffCanSendEveryMemberANotice() throws Exception {
        assertThat(post(owner, path("/notices"), """
                {"title": "Closed Monday", "body": "The studio is closed for kiln repairs."}
                """)).hasStatusOk().bodyJson().extractingPath("$.sent").isEqualTo(3);
        assertThat(post(maker, path("/notices"), """
                {"title": "Hi", "body": ""}
                """)).hasStatus(HttpStatus.FORBIDDEN);
        String id = read(mvc.get().uri(path("/notifications")).with(other.token()).exchange(), "$[0].id");
        assertThat(mvc.post().uri(path("/notifications/" + id + "/read")).with(other.token()))
                .hasStatus(HttpStatus.NO_CONTENT);
        // Someone else's notification is invisible
        assertThat(mvc.post().uri(path("/notifications/" + id + "/read")).with(maker.token()))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    private void fire(String kiln, String kind, String... pieces) throws Exception {
        String firing = read(load(kiln, kind, pieces), "$.id");
        assertThat(post(owner, path("/firings/" + firing + "/transitions"), """
                {"to": "FIRING", "expectedVersion": 0}
                """)).hasStatusOk();
        assertThat(post(owner, path("/firings/" + firing + "/transitions"), """
                {"to": "COMPLETE", "notes": "Cone 6, even", "expectedVersion": 1}
                """)).hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("COMPLETE");
    }

    private MvcTestResult load(String kiln, String kind, String... pieces) {
        String ids = String.join("\", \"", pieces);
        return post(owner, path("/kilns/" + kiln + "/firings"), """
                {"kind": "%s", "cone": "6", "pieceIds": ["%s"]}
                """.formatted(kind, ids));
    }

    private String kiln(String json) throws Exception {
        return read(post(owner, path("/kilns"), json), "$.id");
    }

    private String piece(TestUser user) throws Exception {
        return read(post(user, path("/pieces"), "{\"size\": {}}"), "$.id");
    }

    private String piece(TestUser user, String description) throws Exception {
        return read(post(user, path("/pieces"), "{\"description\": \"%s\", \"size\": {}}".formatted(description)), "$.id");
    }

    private String stageOf(String piece) throws Exception {
        return read(mvc.get().uri(path("/pieces/" + piece)).with(owner.token()).exchange(), "$.stage");
    }

    private String versionOf(String piece) throws Exception {
        return read(mvc.get().uri(path("/pieces/" + piece)).with(owner.token()).exchange(), "$.version");
    }

    private MvcTestResult post(TestUser user, String uri, String json) {
        return mvc.post().uri(uri).with(user.token()).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private String path(String suffix) {
        return "/api/v1/studios/" + studioId + suffix;
    }

    private static String read(MvcTestResult result, String jsonPath) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), jsonPath).toString();
    }

}
