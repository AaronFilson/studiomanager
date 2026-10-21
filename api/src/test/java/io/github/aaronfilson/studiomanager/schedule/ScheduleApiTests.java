package io.github.aaronfilson.studiomanager.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

/** M3 through the API: certification-gated booking, capacity, privacy, rosters, hours, and "today". */
@IntegrationTest
class ScheduleApiTests {

    @Autowired
    MockMvcTester mvc;

    private final TestUser owner = TestUser.named("Olive");
    private final TestUser maker = TestUser.named("Mo");
    private final TestUser monitor = TestUser.named("Nia");
    private UUID studioId;
    private final Instant tomorrow = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);

    @BeforeEach
    void studio() throws Exception {
        studioId = UUID.fromString(read(post(owner, "/api/v1/studios", """
                {"name": "Wheel House", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Denver", "country": "US"}
                """), "$.id"));
        for (TestUser member : List.of(maker, monitor)) {
            String code = read(post(owner, path("/invites"), "{\"role\": \"MEMBER\"}"), "$.code");
            assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(member.token())).hasStatusOk();
        }
    }

    @Test
    void reservingTheWheelRoomNeedsAnUnexpiredOrientationAndAFreePlace() throws Exception {
        String orientation = read(post(owner, path("/certification-types"), """
                {"name": "Wheel room orientation", "validMonths": 12}
                """), "$.id");
        String room = read(post(owner, path("/resources"), """
                {"name": "Wheel room", "kind": "WHEEL_ROOM", "capacity": 1, "requiredCertification": "%s"}
                """.formatted(orientation)), "$.id");

        assertThat(reserve(maker, room, tomorrow)).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.detail").asString().contains("Wheel room orientation");

        String makerId = accountId(maker);
        assertThat(mvc.put().uri(path("/certifications/" + orientation + "/holders/" + makerId)).with(owner.token()))
                .hasStatusOk().bodyJson()
                .hasPathSatisfying("$.valid", v -> assertThat(v).isEqualTo(true))
                .hasPathSatisfying("$.expiresAt", e -> assertThat(e).isNotNull());
        assertThat(reserve(maker, room, tomorrow)).hasStatus(HttpStatus.CREATED);

        // Capacity 1: the next person (certified) finds it full
        assertThat(mvc.put().uri(path("/certifications/" + orientation + "/holders/" + accountId(monitor)))
                .with(owner.token())).hasStatusOk();
        assertThat(reserve(monitor, room, tomorrow)).hasStatus(HttpStatus.CONFLICT);

        // Others see the reservation but not who made it
        assertThat(mvc.get().uri(path("/bookings?from={from}&to={to}"), tomorrow.minusSeconds(60).toString(),
                tomorrow.plusSeconds(7200).toString()).with(monitor.token()))
                .hasStatusOk().bodyJson()
                .hasPathSatisfying("$[0].title", t -> assertThat(t).isEqualTo("Reserved"))
                .hasPathSatisfying("$[0].mine", m -> assertThat(m).isEqualTo(false));
        assertThat(mvc.get().uri(path("/bookings?from={from}&to={to}"), tomorrow.minusSeconds(60).toString(),
                tomorrow.plusSeconds(7200).toString()).with(owner.token()))
                .hasStatusOk().bodyJson().extractingPath("$[0].accountName").isEqualTo("Mo");
    }

    @Test
    void aClassCantBeBookedOverReservationsAndMembersCantBookClasses() throws Exception {
        String room = read(post(owner, path("/resources"), """
                {"name": "Studio floor", "kind": "STUDIO", "capacity": 10}
                """), "$.id");
        assertThat(reserve(maker, room, tomorrow)).hasStatus(HttpStatus.CREATED);

        assertThat(book(owner, room, "CLASS", tomorrow)).hasStatus(HttpStatus.CONFLICT);
        assertThat(book(maker, room, "CLASS", tomorrow.plus(5, ChronoUnit.HOURS))).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(book(owner, room, "CLOSURE", tomorrow.plus(5, ChronoUnit.HOURS))).hasStatus(HttpStatus.CREATED);
        assertThat(reserve(maker, room, tomorrow.plus(5, ChronoUnit.HOURS))).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void monitorsTakeOpenShiftsAndWorkedShiftsCreditHours() throws Exception {
        String monitorId = accountId(monitor);
        assertThat(mvc.put().uri(path("/members/" + monitorId + "/duties")).with(owner.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"duties\": [\"MONITOR\"]}"))
                .hasStatusOk().bodyJson().extractingPath("$.duties").asArray().containsExactly("MONITOR");

        MvcTestResult created = post(owner, path("/shifts"), """
                {"title": "Open studio monitor", "duty": "MONITOR", "firstStartsAt": "%s", "minutes": 180, "weeks": 3}
                """.formatted(tomorrow));
        assertThat(created).hasStatus(HttpStatus.CREATED).bodyJson().extractingPath("$.length()").isEqualTo(3);
        String shift = read(created, "$[0].id");

        // Someone without the duty can't take it; the monitor can
        assertThat(assign(maker, shift, accountId(maker), 0)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(assign(monitor, shift, monitorId, 0)).hasStatusOk()
                .bodyJson().extractingPath("$.accountName").isEqualTo("Nia");

        assertThat(post(owner, path("/shifts/" + shift + "/worked"), "{\"expectedVersion\": 1}")).hasStatusOk();
        assertThat(post(owner, path("/shifts/" + shift + "/worked"), "{\"expectedVersion\": 2}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(post(owner, path("/hours/" + monitorId), """
                {"minutes": -30, "note": "Left early"}
                """)).hasStatusOk();

        assertThat(mvc.get().uri(path("/hours/" + monitorId)).with(monitor.token()))
                .hasStatusOk().bodyJson()
                .hasPathSatisfying("$.totalMinutes", t -> assertThat(t).isEqualTo(150))
                .hasPathSatisfying("$.entries.length()", n -> assertThat(n).isEqualTo(2));
        assertThat(mvc.get().uri(path("/hours/" + monitorId)).with(maker.token())).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void todayShowsWhatTheCoverNeedsToKnow() throws Exception {
        post(owner, path("/kilns"), "{\"name\": \"Skutt\", \"kind\": \"ELECTRIC\"}");
        post(maker, path("/pieces"), "{\"size\": {}}");
        post(owner, path("/shifts"), """
                {"title": "Monitor", "firstStartsAt": "%s", "minutes": 60, "weeks": 1}
                """.formatted(tomorrow));

        assertThat(mvc.get().uri(path("/today")).with(owner.token()))
                .hasStatusOk().bodyJson()
                .hasPathSatisfying("$.waitingForBisque", n -> assertThat(n).isEqualTo(1))
                .hasPathSatisfying("$.kilns.length()", n -> assertThat(n).isEqualTo(1))
                .hasPathSatisfying("$.uncoveredShifts.length()", n -> assertThat(n).isEqualTo(1));
        assertThat(mvc.get().uri(path("/today")).with(maker.token())).hasStatus(HttpStatus.FORBIDDEN);
    }

    private MvcTestResult reserve(TestUser user, String resource, Instant start) {
        return book(user, resource, "RESERVATION", start);
    }

    private MvcTestResult book(TestUser user, String resource, String kind, Instant start) {
        return post(user, path("/bookings"), """
                {"resourceId": "%s", "kind": "%s", "startsAt": "%s", "endsAt": "%s"}
                """.formatted(resource, kind, start, start.plus(2, ChronoUnit.HOURS)));
    }

    private MvcTestResult assign(TestUser user, String shift, String accountId, long version) {
        return mvc.put().uri(path("/shifts/" + shift + "/assignment")).with(user.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\": \"%s\", \"expectedVersion\": %d}".formatted(accountId, version))
                .exchange();
    }

    private String accountId(TestUser user) throws Exception {
        return read(mvc.get().uri("/api/v1/me").with(user.token()).exchange(), "$.account.id");
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
