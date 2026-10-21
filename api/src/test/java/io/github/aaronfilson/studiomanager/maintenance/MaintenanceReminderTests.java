package io.github.aaronfilson.studiomanager.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.RecordingEmailSender;
import io.github.aaronfilson.studiomanager.TestUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Whoever a task falls to hears about it once per cycle, as it comes due, by notice and email.
 * The sweep runs across every studio in the shared test database, so these assertions read each
 * person's own inbox rather than the sweep's counts.
 */
@IntegrationTest
class MaintenanceReminderTests {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    MaintenanceReminderSweep sweep;

    @Autowired
    RecordingEmailSender emails;

    private final TestUser owner = TestUser.named("Olive");
    private final TestUser monitor = TestUser.named("Mona");
    private final TestUser maker = TestUser.named("Mo");
    private UUID studioId;

    @BeforeEach
    void studio() throws Exception {
        studioId = UUID.fromString(read(post(owner, "/api/v1/studios", """
                {"name": "Clay Club", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
                """), "$.id"));
        for (TestUser member : List.of(monitor, maker)) {
            String code = read(post(owner, path("/invites"), "{\"role\": \"MEMBER\"}"), "$.code");
            assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(member.token())).hasStatusOk();
        }
        assertThat(mvc.put().uri(path("/members/" + read(mvc.get().uri("/api/v1/me").with(monitor.token()).exchange(),
                        "$.account.id") + "/duties")).with(owner.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"duties\": [\"MONITOR\"]}")).hasStatusOk();
    }

    @Test
    void remindsWhoeverTheTaskFallsToOncePerCycle() throws Exception {
        Instant start = Instant.now();
        String trap = read(post(owner, path("/maintenance/assets"), "{\"kind\": \"CLAY_TRAP\", \"name\": \"Sink trap\"}"),
                "$.id");
        String empty = read(post(owner, path("/maintenance/assets/" + trap + "/tasks"), """
                {"title": "Empty the trap", "everyDays": 7, "assignedDuty": "MONITOR",
                 "checklist": ["Scoop the settled clay", "Wipe the baffles"]}
                """), "$.id");
        String pugMill = read(post(owner, path("/maintenance/assets"), "{\"kind\": \"PUG_MILL\", \"name\": \"Peter\"}"),
                "$.id");
        post(owner, path("/maintenance/assets/" + pugMill + "/tasks"), "{\"title\": \"Clean the auger\", \"everyDays\": 30}");

        // Nothing is due yet
        sweep.runAt(start);
        assertThat(notices(monitor)).isEmpty();

        // A weekly task warns a day ahead: the monitors hear, nobody else
        sweep.runAt(start.plus(Duration.ofHours(6 * 24 + 12)));
        assertThat(notices(monitor)).containsExactly("Due soon: Empty the trap");
        assertThat(notices(maker)).isEmpty();
        assertThat(notices(owner)).isEmpty();
        await().atMost(Duration.ofSeconds(10)).until(() -> !emails.sentTo(monitor.email()).isEmpty());
        assertThat(emails.sentTo(monitor.email()).getFirst().text())
                .contains("Sink trap: Empty the trap")
                .contains("- Scoop the settled clay")
                .contains("Maintenance page");

        // Overdue now, but that's the same cycle: no second reminder
        sweep.runAt(start.plus(Duration.ofDays(8)));
        assertThat(notices(monitor)).hasSize(1);

        // Done, so a new cycle starts, and it gets its own reminder when it falls due
        assertThat(post(monitor, path("/maintenance/tasks/" + empty + "/completions"), "{}")).hasStatusOk();
        sweep.runAt(Instant.now().plus(Duration.ofDays(8)));
        assertThat(notices(monitor)).containsExactly("Due: Empty the trap", "Due soon: Empty the trap");

        // Nobody is assigned the auger, so the staff hear about it
        sweep.runAt(start.plus(Duration.ofDays(29)));
        assertThat(notices(owner)).containsExactly("Due soon: Clean the auger");
        assertThat(notices(monitor)).hasSize(2);
    }

    /** The titles of someone's maintenance notices in the studio, newest first. */
    private List<String> notices(TestUser user) throws Exception {
        String body = mvc.get().uri(path("/notifications")).with(user.token()).exchange().getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$[?(@.kind == 'MAINTENANCE_DUE')].title");
    }

    private MvcTestResult post(TestUser user, String uri, String json) {
        MvcTestResult result = mvc.post().uri(uri).with(user.token()).contentType(MediaType.APPLICATION_JSON)
                .content(json).exchange();
        assertThat(result.getResponse().getStatus()).as(uri).isIn(List.of(200, 201));
        return result;
    }

    private String path(String suffix) {
        return "/api/v1/studios/" + studioId + suffix;
    }

    private static String read(MvcTestResult result, String jsonPath) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), jsonPath).toString();
    }

}
