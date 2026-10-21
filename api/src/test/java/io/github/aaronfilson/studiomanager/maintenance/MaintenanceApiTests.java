package io.github.aaronfilson.studiomanager.maintenance;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONObject;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.TestUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Assets, recurring tasks counted in days or firings, completing them, and who may. */
@IntegrationTest
class MaintenanceApiTests {

    @Autowired
    MockMvcTester mvc;

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
        assertThat(mvc.put().uri(path("/members/" + accountId(monitor) + "/duties")).with(owner.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"duties\": [\"MONITOR\"]}")).hasStatusOk();
    }

    @Test
    void kilnTasksKeepTheirOwnClocksSoOneRepairDoesntRestartAnother() throws Exception {
        String kiln = read(post(owner, path("/kilns"), "{\"name\": \"Skutt 1027\", \"kind\": \"ELECTRIC\"}"), "$.id");
        // A new kiln is maintained without anyone adding it
        MvcTestResult assets = mvc.get().uri(path("/maintenance/assets")).with(owner.token()).exchange();
        assertThat(assets).hasStatusOk().bodyJson()
                .hasPathSatisfying("$[0].kind", k -> assertThat(k).isEqualTo("KILN"))
                .hasPathSatisfying("$[0].name", n -> assertThat(n).isEqualTo("Skutt 1027"))
                .hasPathSatisfying("$[0].kilnId", id -> assertThat(id).isEqualTo(kiln));
        String asset = read(assets, "$[0].id");

        String elements = read(post(owner, path("/maintenance/assets/" + asset + "/tasks"), """
                {"title": "Check element resistance", "everyFirings": 2,
                 "checklist": ["Read each element", "Note any over 10% high"]}
                """), "$.id");
        String wiring = read(post(owner, path("/maintenance/assets/" + asset + "/tasks"), """
                {"title": "Wiring check", "everyDays": 365, "everyFirings": 30}
                """), "$.id");

        fire(kiln);
        assertThat(field(elements, "status")).isEqualTo("DUE_SOON");
        assertThat(field(elements, "firingsLeft")).isEqualTo(1);
        fire(kiln);

        // Due now, and on Today
        assertThat(field(elements, "status")).isEqualTo("DUE");
        assertThat(mvc.get().uri(path("/today")).with(owner.token())).hasStatusOk().bodyJson()
                .hasPathSatisfying("$.maintenanceDue[*].title", t -> assertThat(t).asArray()
                        .containsExactly("Check element resistance"));

        assertThat(post(owner, path("/maintenance/tasks/" + elements + "/completions"), """
                {"note": "All within 5%", "cost": "0", "checked": ["Read each element"]}
                """)).hasStatusOk().bodyJson()
                .hasPathSatisfying("$.status", s -> assertThat(s).isEqualTo("OK"))
                .hasPathSatisfying("$.firingsLeft", n -> assertThat(n).isEqualTo(2));
        // The other task's count carries on
        assertThat(field(wiring, "firingsLeft")).isEqualTo(28);

        // A repair outside any task restarts nothing
        assertThat(post(owner, path("/maintenance/assets/" + asset + "/records"), """
                {"note": "Replaced the lid hinge pin", "cost": "12.50"}
                """)).hasStatus(HttpStatus.CREATED);
        assertThat(field(wiring, "firingsLeft")).isEqualTo(28);
        assertThat(field(elements, "firingsLeft")).isEqualTo(2);

        assertThat(mvc.get().uri(path("/maintenance/assets/" + asset + "/records")).with(owner.token()))
                .hasStatusOk().bodyJson()
                .hasPathSatisfying("$[0].note", n -> assertThat(n).isEqualTo("Replaced the lid hinge pin"))
                .hasPathSatisfying("$[0].taskId", t -> assertThat(t).isNull())
                .hasPathSatisfying("$[1].taskTitle", t -> assertThat(t).isEqualTo("Check element resistance"))
                .hasPathSatisfying("$[1].atFiring", n -> assertThat(n).isEqualTo(2))
                .hasPathSatisfying("$[1].checked", c -> assertThat(c).asArray().containsExactly("Read each element"))
                .hasPathSatisfying("$[1].doneByName", n -> assertThat(n).isEqualTo("Olive"));

        // Ticking a line that isn't on the checklist is refused
        assertThat(post(owner, path("/maintenance/tasks/" + elements + "/completions"), """
                {"checked": ["Something else"]}
                """)).hasStatus(HttpStatus.BAD_REQUEST);
        // A kiln is marked down on the kiln, not here
        assertThat(mvc.put().uri(path("/maintenance/assets/" + asset + "/availability")).with(owner.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"downReason\": \"Relay\", \"expectedVersion\": 0}"))
                .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void aDutyHolderSeesAndCompletesTheirTasksAndNobodyElseCan() throws Exception {
        String pugMill = read(post(owner, path("/maintenance/assets"), """
                {"kind": "PUG_MILL", "name": "Peter Pugger", "location": "Reclaim room"}
                """), "$.id");
        String trap = read(post(owner, path("/maintenance/assets"), """
                {"kind": "CLAY_TRAP", "name": "Sink trap"}
                """), "$.id");
        // Only kilns count firings
        assertThat(post(owner, path("/maintenance/assets/" + pugMill + "/tasks"), """
                {"title": "Clean the auger", "everyFirings": 10}
                """)).hasStatus(HttpStatus.BAD_REQUEST);
        String empty = read(post(owner, path("/maintenance/assets/" + trap + "/tasks"), """
                {"title": "Empty the trap", "everyDays": 7, "assignedDuty": "MONITOR"}
                """), "$.id");
        read(post(owner, path("/maintenance/assets/" + pugMill + "/tasks"), """
                {"title": "Clean the auger", "everyDays": 30}
                """), "$.id");

        assertThat(mvc.get().uri(path("/maintenance/my-tasks")).with(monitor.token())).hasStatusOk().bodyJson()
                .hasPathSatisfying("$[*].title", t -> assertThat(t).asArray().containsExactly("Empty the trap"))
                .hasPathSatisfying("$[0].assetName", n -> assertThat(n).isEqualTo("Sink trap"));
        assertThat(mvc.get().uri(path("/maintenance/my-tasks")).with(maker.token())).hasStatusOk().bodyJson()
                .extractingPath("$.length()").isEqualTo(0);

        assertThat(post(maker, path("/maintenance/tasks/" + empty + "/completions"), "{}"))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(post(monitor, path("/maintenance/tasks/" + empty + "/completions"), """
                {"note": "Half full"}
                """)).hasStatusOk();
        // Members can't see or change the asset list
        assertThat(mvc.get().uri(path("/maintenance/assets")).with(monitor.token())).hasStatus(HttpStatus.FORBIDDEN);

        // Down and back, then archived
        assertThat(mvc.put().uri(path("/maintenance/assets/" + pugMill + "/availability")).with(owner.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"downReason\": \"Auger seal leaking\", \"expectedVersion\": 0}"))
                .hasStatusOk().bodyJson().extractingPath("$.available").isEqualTo(false);
        assertThat(mvc.put().uri(path("/maintenance/assets/" + pugMill + "/availability")).with(owner.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\": 0}"))
                .hasStatus(HttpStatus.CONFLICT);
        assertThat(mvc.delete().uri(path("/maintenance/assets/" + pugMill)).with(owner.token()))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri(path("/maintenance/assets")).with(owner.token())).hasStatusOk().bodyJson()
                .extractingPath("$[*].name").asArray().containsExactly("Sink trap");

        // Kilns are added on the kilns page
        assertThat(post(owner, path("/maintenance/assets"), "{\"kind\": \"KILN\", \"name\": \"Gas\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void suggestsPublishedTasksOnlyWhereThereIsASchedule() throws Exception {
        read(post(owner, path("/kilns"), "{\"name\": \"L&L e23\", \"kind\": \"ELECTRIC\"}"), "$.id");
        read(post(owner, path("/kilns"), "{\"name\": \"Gas car kiln\", \"kind\": \"GAS\"}"), "$.id");
        String assets = mvc.get().uri(path("/maintenance/assets")).with(owner.token()).exchange().getResponse()
                .getContentAsString();
        String electric = JsonPath.<List<String>>read(assets, "$[?(@.name == 'L&L e23')].id").getFirst();
        String gas = JsonPath.<List<String>>read(assets, "$[?(@.name == 'Gas car kiln')].id").getFirst();
        String safety = read(post(owner, path("/maintenance/assets"), "{\"kind\": \"SAFETY\", \"name\": \"Extinguisher\"}"),
                "$.id");
        String wheel = read(post(owner, path("/maintenance/assets"), "{\"kind\": \"WHEEL\", \"name\": \"Brent 1\"}"),
                "$.id");

        MvcTestResult suggested = mvc.get().uri(path("/maintenance/assets/" + electric + "/suggested-tasks"))
                .with(owner.token()).exchange();
        assertThat(suggested).hasStatusOk().bodyJson()
                .hasPathSatisfying("$[*].task.title", t -> assertThat(t).asArray().containsExactly(
                        "Clean and inspect before firing", "Check elements and the circuit", "Wiring and terminal check"))
                .hasPathSatisfying("$[2].task.everyDays", d -> assertThat(d).isEqualTo(365))
                .hasPathSatisfying("$[2].task.everyFirings", f -> assertThat(f).isEqualTo(30))
                .hasPathSatisfying("$[0].source", s -> assertThat(s).asString().contains("L&L"));
        // Adding one as it is takes it off the list
        Map<String, Object> elements = JsonPath.read(suggested.getResponse().getContentAsString(), "$[1].task");
        assertThat(post(owner, path("/maintenance/assets/" + electric + "/tasks"), new JSONObject(elements).toJSONString()))
                .hasStatus(HttpStatus.CREATED);
        assertThat(mvc.get().uri(path("/maintenance/assets/" + electric + "/suggested-tasks")).with(owner.token()))
                .hasStatusOk().bodyJson().extractingPath("$.length()").isEqualTo(2);

        assertThat(mvc.get().uri(path("/maintenance/assets/" + gas + "/suggested-tasks")).with(owner.token()))
                .hasStatusOk().bodyJson().extractingPath("$.length()").isEqualTo(0);
        assertThat(mvc.get().uri(path("/maintenance/assets/" + wheel + "/suggested-tasks")).with(owner.token()))
                .hasStatusOk().bodyJson().extractingPath("$.length()").isEqualTo(0);
        assertThat(mvc.get().uri(path("/maintenance/assets/" + safety + "/suggested-tasks")).with(owner.token()))
                .hasStatusOk().bodyJson().hasPathSatisfying("$[0].source", s -> assertThat(s).asString().contains("OSHA"));
        assertThat(mvc.get().uri(path("/maintenance/assets/" + safety + "/suggested-tasks")).with(monitor.token()))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void aRepairTakesItsPartsOutOfSupplies() throws Exception {
        read(post(owner, path("/kilns"), "{\"name\": \"Skutt 1027\", \"kind\": \"ELECTRIC\"}"), "$.id");
        String kiln = read(mvc.get().uri(path("/maintenance/assets")).with(owner.token()).exchange(), "$[0].id");
        String element = read(post(owner, path("/supplies"), """
                {"name": "Element, 1027 top", "category": "KILN_SUPPLY", "unit": "element", "parLevel": "1"}
                """), "$.id");
        assertThat(post(owner, path("/supplies/" + element + "/movements"), """
                {"kind": "RECEIVED", "quantity": "2"}
                """)).hasStatusOk();

        assertThat(post(owner, path("/maintenance/assets/" + kiln + "/records"), """
                {"note": "Top element open; replaced", "cost": "64.00",
                 "parts": [{"itemId": "%s", "quantity": "1"}]}
                """.formatted(element))).hasStatus(HttpStatus.CREATED).bodyJson()
                .hasPathSatisfying("$.parts[0].itemName", n -> assertThat(n).isEqualTo("Element, 1027 top"))
                .hasPathSatisfying("$.parts[0].quantity", q -> assertThat(q).isEqualTo("1"))
                .hasPathSatisfying("$.parts[0].unit", u -> assertThat(u).isEqualTo("element"));

        // Stock went down, and the movement says where it went
        assertThat(mvc.get().uri(path("/supplies")).with(owner.token())).hasStatusOk().bodyJson()
                .hasPathSatisfying("$[0].onHand", n -> assertThat(n).isEqualTo("1"));
        assertThat(mvc.get().uri(path("/supplies/" + element + "/movements")).with(owner.token())).hasStatusOk()
                .bodyJson().hasPathSatisfying("$[*].note", n -> assertThat(n).asArray().contains("Repair: Skutt 1027"));
        assertThat(mvc.get().uri(path("/maintenance/assets/" + kiln + "/records")).with(owner.token())).hasStatusOk()
                .bodyJson().hasPathSatisfying("$[0].parts[0].itemId", i -> assertThat(i).isEqualTo(element));

        // The same part twice in one repair is refused, and so is a part from nowhere, with nothing recorded
        assertThat(post(owner, path("/maintenance/assets/" + kiln + "/records"), """
                {"note": "x", "parts": [{"itemId": "%1$s", "quantity": "1"}, {"itemId": "%1$s", "quantity": "1"}]}
                """.formatted(element))).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(post(owner, path("/maintenance/assets/" + kiln + "/records"), """
                {"note": "Ghost part", "parts": [{"itemId": "%s", "quantity": "1"}]}
                """.formatted(UUID.randomUUID()))).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri(path("/maintenance/assets/" + kiln + "/records")).with(owner.token())).hasStatusOk()
                .bodyJson().extractingPath("$.length()").isEqualTo(1);

        // Completing a task can draw parts too, but only staff draw stock
        String check = read(post(owner, path("/maintenance/assets/" + kiln + "/tasks"), """
                {"title": "Check elements", "everyFirings": 10, "assignedDuty": "MONITOR"}
                """), "$.id");
        assertThat(post(monitor, path("/maintenance/tasks/" + check + "/completions"), """
                {"parts": [{"itemId": "%s", "quantity": "1"}]}
                """.formatted(element))).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(post(owner, path("/maintenance/tasks/" + check + "/completions"), """
                {"note": "Bottom element replaced", "parts": [{"itemId": "%s", "quantity": "1"}]}
                """.formatted(element))).hasStatusOk();
        assertThat(mvc.get().uri(path("/supplies")).with(owner.token())).hasStatusOk().bodyJson()
                .hasPathSatisfying("$[0].onHand", n -> assertThat(n).isEqualTo("0"));
        assertThat(mvc.get().uri(path("/maintenance/assets/" + kiln + "/records")).with(owner.token())).hasStatusOk()
                .bodyJson().hasPathSatisfying("$[0].taskTitle", t -> assertThat(t).isEqualTo("Check elements"))
                .hasPathSatisfying("$[0].parts[0].itemName", n -> assertThat(n).isEqualTo("Element, 1027 top"));
    }

    @Test
    void exportsTheHistoryForAnInsurerAsCsv() throws Exception {
        String trap = read(post(owner, path("/maintenance/assets"), "{\"kind\": \"CLAY_TRAP\", \"name\": \"Sink trap\"}"),
                "$.id");
        String empty = read(post(owner, path("/maintenance/assets/" + trap + "/tasks"), """
                {"title": "Empty the trap", "everyDays": 7, "checklist": ["Scoop", "Wipe"]}
                """), "$.id");
        assertThat(post(owner, path("/maintenance/tasks/" + empty + "/completions"), """
                {"note": "=1+1 half full", "checked": ["Scoop", "Wipe"]}
                """)).hasStatusOk();
        assertThat(post(owner, path("/maintenance/assets/" + trap + "/records"), """
                {"note": "New baffle, fitted", "cost": "12.50"}
                """)).hasStatus(HttpStatus.CREATED);
        LocalDate today = LocalDate.now(ZoneId.of("America/Chicago"));

        MvcTestResult csv = mvc.get().uri(path("/maintenance/records/export?from={from}&to={to}"), today.minusDays(1),
                today.plusDays(1)).with(owner.token()).exchange();
        assertThat(csv).hasStatusOk().hasContentType("text/csv;charset=UTF-8");
        assertThat(csv.getResponse().getContentAsString().lines().toList()).containsExactly(
                "Date,Asset,Task,Note,Cost,Checked,At firing,Done by",
                today + ",Sink trap,Empty the trap,'=1+1 half full,,Scoop; Wipe,,Olive",
                today + ",Sink trap,Repair or other work,\"New baffle, fitted\",12.50,,,Olive");

        // Nothing outside the range, a backwards range is refused, and members can't export
        assertThat(mvc.get().uri(path("/maintenance/records/export?from={from}&to={to}"), today.plusDays(2),
                today.plusDays(3)).with(owner.token()).exchange().getResponse().getContentAsString().lines())
                .hasSize(1);
        assertThat(mvc.get().uri(path("/maintenance/records/export?from={from}&to={to}"), today, today.minusDays(1))
                .with(owner.token())).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(mvc.get().uri(path("/maintenance/records/export?from={from}&to={to}"), today, today)
                .with(monitor.token())).hasStatus(HttpStatus.FORBIDDEN);
    }

    /** One field of a task, as the asset list reports it now. */
    private Object field(String taskId, String field) throws Exception {
        String assets = mvc.get().uri(path("/maintenance/assets")).with(owner.token()).exchange()
                .getResponse().getContentAsString();
        List<Object> found = JsonPath.read(assets, "$[*].tasks[?(@.id == '%s')].%s".formatted(taskId, field));
        assertThat(found).as("%s of task %s", field, taskId).hasSize(1);
        return found.getFirst();
    }

    private void fire(String kiln) throws Exception {
        String piece = read(post(maker, path("/pieces"), "{\"size\": {}}"), "$.id");
        String firing = read(post(owner, path("/kilns/" + kiln + "/firings"),
                "{\"kind\": \"BISQUE\", \"pieceIds\": [\"%s\"]}".formatted(piece)), "$.id");
        assertThat(post(owner, path("/firings/" + firing + "/transitions"), "{\"to\": \"FIRING\", \"expectedVersion\": 0}"))
                .hasStatusOk();
        assertThat(post(owner, path("/firings/" + firing + "/transitions"), "{\"to\": \"COMPLETE\", \"expectedVersion\": 1}"))
                .hasStatusOk();
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
