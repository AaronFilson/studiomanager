package io.github.aaronfilson.studiomanager.maintenance;

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

/** A member reports a problem from the wheel; the staff and technicians hear; staff resolve it. */
@IntegrationTest
class ProblemReportApiTests {

    @Autowired
    MockMvcTester mvc;

    private final TestUser owner = TestUser.named("Olive");
    private final TestUser technician = TestUser.named("Tad");
    private final TestUser maker = TestUser.named("Mo");
    private final TestUser other = TestUser.named("Ola");
    private UUID studioId;

    @BeforeEach
    void studio() throws Exception {
        studioId = UUID.fromString(read(post(owner, "/api/v1/studios", """
                {"name": "Clay Club", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
                """), "$.id"));
        for (TestUser member : List.of(technician, maker, other)) {
            String code = read(post(owner, path("/invites"), "{\"role\": \"MEMBER\"}"), "$.code");
            assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(member.token())).hasStatusOk();
        }
        String technicianId = read(mvc.get().uri("/api/v1/me").with(technician.token()).exchange(), "$.account.id");
        assertThat(mvc.put().uri(path("/members/" + technicianId + "/duties")).with(owner.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"duties\": [\"TECHNICIAN\"]}")).hasStatusOk();
    }

    @Test
    void aMemberReportsAProblemAndStaffResolveIt() throws Exception {
        String wheel = read(post(owner, path("/maintenance/assets"), "{\"kind\": \"WHEEL\", \"name\": \"Wheel 3\"}"),
                "$.id");

        // Members can see what there is to report on, though not the maintenance list itself
        assertThat(mvc.get().uri(path("/maintenance/reportable-assets")).with(maker.token())).hasStatusOk().bodyJson()
                .hasPathSatisfying("$[*].name", n -> assertThat(n).asArray().containsExactly("Wheel 3"))
                .hasPathSatisfying("$[0].available", a -> assertThat(a).isEqualTo(true));

        MvcTestResult reported = post(maker, path("/maintenance/problem-reports"), """
                {"assetId": "%s", "description": "Grinds when it speeds up"}
                """.formatted(wheel));
        assertThat(reported).hasStatus(HttpStatus.CREATED).bodyJson()
                .hasPathSatisfying("$.assetName", n -> assertThat(n).isEqualTo("Wheel 3"))
                .hasPathSatisfying("$.reportedByName", n -> assertThat(n).isEqualTo("Mo"))
                .hasPathSatisfying("$.open", o -> assertThat(o).isEqualTo(true));
        String report = read(reported, "$.id");

        // The owner and the technician hear about it; the reporter and other members don't
        assertThat(notices(owner)).containsExactly("Reported: Wheel 3");
        assertThat(notices(technician)).containsExactly("Reported: Wheel 3");
        assertThat(notices(maker)).isEmpty();
        assertThat(notices(other)).isEmpty();

        // Whoever covers the studio sees it on Today
        assertThat(mvc.get().uri(path("/today")).with(owner.token())).hasStatusOk().bodyJson()
                .hasPathSatisfying("$.openProblems[*].description",
                        d -> assertThat(d).asArray().containsExactly("Grinds when it speeds up"));

        // A member sees their own reports only
        assertThat(mvc.get().uri(path("/maintenance/problem-reports")).with(maker.token())).hasStatusOk().bodyJson()
                .extractingPath("$.length()").isEqualTo(1);
        assertThat(mvc.get().uri(path("/maintenance/problem-reports")).with(other.token())).hasStatusOk().bodyJson()
                .extractingPath("$.length()").isEqualTo(0);

        // Only staff resolve, once
        assertThat(post(maker, path("/maintenance/problem-reports/" + report + "/resolution"),
                "{\"expectedVersion\": 0}")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(post(owner, path("/maintenance/problem-reports/" + report + "/resolution"), """
                {"resolution": "New belt", "expectedVersion": 0}
                """)).hasStatusOk().bodyJson()
                .hasPathSatisfying("$.open", o -> assertThat(o).isEqualTo(false))
                .hasPathSatisfying("$.resolution", r -> assertThat(r).isEqualTo("New belt"))
                .hasPathSatisfying("$.resolvedByName", n -> assertThat(n).isEqualTo("Olive"));
        assertThat(post(owner, path("/maintenance/problem-reports/" + report + "/resolution"),
                "{\"expectedVersion\": 1}")).hasStatus(HttpStatus.CONFLICT);
        assertThat(mvc.get().uri(path("/maintenance/problem-reports?open=true")).with(owner.token())).hasStatusOk()
                .bodyJson().extractingPath("$.length()").isEqualTo(0);
        assertThat(mvc.get().uri(path("/today")).with(owner.token())).hasStatusOk().bodyJson()
                .extractingPath("$.openProblems.length()").isEqualTo(0);

        // Somewhere not on the list is fine; an unknown asset or an empty description isn't
        assertThat(post(other, path("/maintenance/problem-reports"), "{\"description\": \"Back door won't lock\"}"))
                .hasStatus(HttpStatus.CREATED);
        assertThat(notices(owner)).contains("Reported: a problem");
        assertThat(post(other, path("/maintenance/problem-reports"), """
                {"assetId": "%s", "description": "Huh"}
                """.formatted(UUID.randomUUID()))).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(post(other, path("/maintenance/problem-reports"), "{\"description\": \"  \"}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    private List<String> notices(TestUser user) throws Exception {
        String body = mvc.get().uri(path("/notifications")).with(user.token()).exchange().getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$[?(@.kind == 'PROBLEM_REPORTED')].title");
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
