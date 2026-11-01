package io.github.aaronfilson.studiomanager.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.RecordingEmailSender;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.mail.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Moving a studio's member list, balances and consigned stock in from a spreadsheet. */
@IntegrationTest
class ImportApiTests {

    private static final Duration WAIT = Duration.ofSeconds(10);
    private static final Pattern LINK = Pattern.compile("http://localhost:5173/invites/(\\S+)");

    @Autowired
    MockMvcTester mvc;

    @Autowired
    RecordingEmailSender emails;

    private final TestUser owner = TestUser.named("Olive");
    private final TestUser mo = TestUser.named("Mo");
    private final TestUser nia = TestUser.named("Nia");
    private final TestUser pat = TestUser.named("Pat");
    private UUID studioId;
    private String moId;

    @BeforeEach
    void studioWithMo() throws Exception {
        studioId = UUID.fromString(read(post(owner, "/api/v1/studios", """
                {"name": "Import Clay", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
                """), "$.id"));
        String code = read(post(owner, path("/invites"), "{\"role\": \"MEMBER\"}"), "$.code");
        assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(mo.token())).hasStatusOk();
        moId = read(mvc.get().uri("/api/v1/me").with(mo.token()).exchange(), "$.account.id");
    }

    @Test
    void aDryRunPreviewsEachRowAndKeepsNothing() throws Exception {
        MvcTestResult preview = importMembers(true, """
                {"line": 2, "email": "%s", "balance": "$12.50"},
                {"line": 3, "email": "%s", "balance": "(4.00)", "balanceAsOf": "9/30/2026"},
                {"line": 4, "email": "%s"},
                {"line": 5, "email": "%s"}
                """.formatted(mo.email(), nia.email().toUpperCase(), nia.email(), pat.email()));

        assertThat(preview).hasStatusOk().bodyJson()
                .hasPathSatisfying("$.committed", c -> assertThat(c).isEqualTo(false))
                .hasPathSatisfying("$.rows[*].outcome", o -> assertThat(o).asArray()
                        .containsExactly("UPDATED", "CREATED", "SKIPPED", "CREATED"));
        assertThat(balance(moId)).isEqualTo("0");
        assertThat(mvc.get().uri(path("/invites")).with(owner.token())).hasStatusOk().bodyJson()
                .extractingPath("$[*].email").asArray().isEmpty();
    }

    @Test
    void oneBadRowStopsTheWholeImport() throws Exception {
        MvcTestResult result = importMembers(false, """
                {"line": 2, "email": "%s"},
                {"line": 3, "email": "not an address"},
                {"line": 4, "email": "%s", "balance": "twelve"}
                """.formatted(nia.email(), pat.email()));

        assertThat(result).hasStatusOk().bodyJson()
                .hasPathSatisfying("$.committed", c -> assertThat(c).isEqualTo(false))
                .hasPathSatisfying("$.rows[*].outcome", o -> assertThat(o).asArray()
                        .containsExactly("CREATED", "ERROR", "ERROR"))
                .hasPathSatisfying("$.rows[2].message", m -> assertThat(m).isEqualTo("'twelve' isn't an amount of money."));
        assertThat(mvc.get().uri(path("/invites")).with(owner.token())).hasStatusOk().bodyJson()
                .extractingPath("$.length()").isEqualTo(0);
    }

    @Test
    void importedBalancesPostOnceForMembersNowAndForNewcomersWhenTheyJoin() throws Exception {
        String rows = """
                {"line": 2, "email": "%s", "balance": "12.50"},
                {"line": 3, "email": "%s", "balance": "-4.00"}
                """.formatted(mo.email(), nia.email());
        assertThat(importMembers(false, rows)).hasStatusOk().bodyJson()
                .extractingPath("$.committed").isEqualTo(true);
        assertThat(balance(moId)).isEqualTo("12.50");

        // Nia's balance waits for her to join
        Email invite = awaitEmailTo(nia.email());
        assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", codeIn(invite)).with(nia.token())).hasStatusOk();
        String niaId = read(mvc.get().uri("/api/v1/me").with(nia.token()).exchange(), "$.account.id");
        await().atMost(WAIT).until(() -> balance(niaId).equals("-4.00"));

        // The same file again changes nothing
        assertThat(importMembers(false, rows)).hasStatusOk().bodyJson()
                .hasPathSatisfying("$.committed", c -> assertThat(c).isEqualTo(true))
                .hasPathSatisfying("$.rows[*].outcome", o -> assertThat(o).asArray().containsExactly("SKIPPED", "SKIPPED"));
        assertThat(balance(moId)).isEqualTo("12.50");
        assertThat(balance(niaId)).isEqualTo("-4.00");
    }

    @Test
    void consignedStockFindsOrCreatesEachArtist() throws Exception {
        assertThat(post(owner, path("/payees"), "{\"name\": \"Ana Ruiz\", \"email\": \"ana@example.test\", \"files1099\": false, \"consignorShare\": \"0.6\"}"))
                .hasStatus(HttpStatus.CREATED);

        MvcTestResult result = post(owner, path("/imports/consignment"), """
                {"rows": [
                  {"line": 2, "artist": "Ana R.", "artistEmail": "ANA@example.test", "description": "Mug", "retailPrice": "$40", "quantity": "3"},
                  {"line": 3, "artist": "Ben Lee", "description": "Bowl", "retailPrice": "1,200.00", "artistShare": "50%%", "receivedOn": "10/1/2026"},
                  {"line": 4, "artist": "ben lee", "description": "Plate", "retailPrice": "30"}
                ]}
                """.formatted());
        assertThat(result).hasStatusOk().bodyJson()
                .hasPathSatisfying("$.committed", c -> assertThat(c).isEqualTo(true))
                .hasPathSatisfying("$.rows[0].message", m -> assertThat(m).isEqualTo("Took in 3 × Mug for Ana Ruiz."))
                .hasPathSatisfying("$.rows[1].message", m -> assertThat(m).isEqualTo("Took in 1 × Bowl for Ben Lee (a new artist)."))
                .hasPathSatisfying("$.rows[2].message", m -> assertThat(m).isEqualTo("Took in 1 × Plate for Ben Lee."));

        assertThat(mvc.get().uri(path("/payees")).with(owner.token())).hasStatusOk().bodyJson()
                .extractingPath("$[*].name").asArray().containsExactly("Ana Ruiz", "Ben Lee");
        assertThat(mvc.get().uri(path("/consignment-items")).with(owner.token())).hasStatusOk().bodyJson()
                .hasPathSatisfying("$.length()", n -> assertThat(n).isEqualTo(3))
                .hasPathSatisfying("$[?(@.description == 'Bowl')].consignorShare", s -> assertThat(s).asArray().containsExactly("0.5"))
                .hasPathSatisfying("$[?(@.description == 'Bowl')].receivedOn", d -> assertThat(d).asArray().containsExactly("2026-10-01"));
    }

    @Test
    void onlyStaffImport() {
        assertThat(importMembers(mo, false, "{\"line\": 2, \"email\": \"x@example.test\"}"))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    private MvcTestResult importMembers(boolean dryRun, String rows) {
        return importMembers(owner, dryRun, rows);
    }

    private MvcTestResult importMembers(TestUser user, boolean dryRun, String rows) {
        return post(user, path("/imports/members"), "{\"dryRun\": %s, \"rows\": [%s]}".formatted(dryRun, rows));
    }

    private String balance(String memberId) throws Exception {
        return read(mvc.get().uri(path("/members/" + memberId + "/statement")).with(owner.token()).exchange(), "$.owed");
    }

    private Email awaitEmailTo(String address) {
        await().atMost(WAIT).until(() -> !emails.sentTo(address).isEmpty());
        List<Email> sent = emails.sentTo(address);
        return sent.getLast();
    }

    private static String codeIn(Email email) {
        Matcher matcher = LINK.matcher(email.text());
        assertThat(matcher.find()).as("a link in: %s", email.text()).isTrue();
        return matcher.group(1);
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
