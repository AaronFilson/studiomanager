package io.github.aaronfilson.studiomanager.studio;

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

/** Pasting a list of addresses invites each of them by email, and each email's link joins the studio. */
@IntegrationTest
class InviteEmailTests {

    private static final Duration WAIT = Duration.ofSeconds(10);
    private static final Pattern LINK = Pattern.compile("http://localhost:5173/invites/(\\S+)");

    @Autowired
    MockMvcTester mvc;

    @Autowired
    RecordingEmailSender emails;

    private final TestUser owner = TestUser.named("Olive");
    private final TestUser staff = TestUser.named("Sam");
    private final TestUser mo = TestUser.named("Mo");
    private final TestUser nia = TestUser.named("Nia");
    private UUID studioId;

    @BeforeEach
    void studio() throws Exception {
        studioId = UUID.fromString(read(post(owner, "/api/v1/studios", """
                {"name": "Clay Club", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
                """), "$.id"));
        String code = read(post(owner, path("/invites"), "{\"role\": \"STAFF\"}"), "$.code");
        assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(staff.token())).hasStatusOk();
    }

    @Test
    void eachPastedAddressGetsAnInviteItsLinkJoinsTheStudio() throws Exception {
        MvcTestResult result = emailInvites(staff, "Mo <%s>, %s; not an address, %s".formatted(
                mo.email(), nia.email().toUpperCase(), owner.email()), "MEMBER");

        assertThat(result).hasStatusOk().bodyJson()
                .hasPathSatisfying("$.invited[*].email", e -> assertThat(e).asArray().containsExactly(mo.email(), nia.email()))
                .hasPathSatisfying("$.skipped[0].email", e -> assertThat(e).isEqualTo(owner.email()))
                .hasPathSatisfying("$.skipped[0].reason", r -> assertThat(r).isEqualTo("ALREADY_MEMBER"));

        Email email = awaitEmailTo(mo.email(), 1).getFirst();
        assertThat(email.subject()).isEqualTo("You're invited to join Clay Club");
        assertThat(email.text()).startsWith("Sam invited you to join Clay Club as a member.");
        assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", codeIn(email)).with(mo.token()))
                .hasStatusOk().bodyJson().extractingPath("$.role").isEqualTo("MEMBER");

        // Pasting the list again invites nobody twice
        assertThat(emailInvites(staff, mo.email() + "\n" + nia.email(), "MEMBER")).hasStatusOk().bodyJson()
                .hasPathSatisfying("$.invited.length()", n -> assertThat(n).isEqualTo(0))
                .hasPathSatisfying("$.skipped[*].reason", r -> assertThat(r).asArray()
                        .containsExactly("ALREADY_MEMBER", "ALREADY_INVITED"));
        // Mo's invite is used; Nia's is still open
        assertThat(mvc.get().uri(path("/invites")).with(staff.token())).hasStatusOk().bodyJson()
                .extractingPath("$[*].email").asArray().contains(nia.email()).doesNotContain(mo.email());
    }

    @Test
    void resendingGivesANewCodeAndTheOldOneStopsWorking() throws Exception {
        String id = read(emailInvites(staff, nia.email(), "MEMBER"), "$.invited[0].id");
        String first = codeIn(awaitEmailTo(nia.email(), 1).getFirst());

        assertThat(mvc.post().uri(path("/invites/" + id + "/resend")).with(staff.token())).hasStatusOk();
        String second = codeIn(awaitEmailTo(nia.email(), 2).get(1));

        assertThat(second).isNotEqualTo(first);
        assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", first).with(nia.token()))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", second).with(nia.token())).hasStatusOk();
    }

    @Test
    void aRevokedInvitesLinkDoesNothing() throws Exception {
        String id = read(emailInvites(staff, nia.email(), "MEMBER"), "$.invited[0].id");
        String code = codeIn(awaitEmailTo(nia.email(), 1).getFirst());

        assertThat(mvc.delete().uri(path("/invites/" + id)).with(staff.token())).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(nia.token()))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri(path("/invites")).with(staff.token())).hasStatusOk().bodyJson()
                .extractingPath("$[*].id").asArray().doesNotContain(id);
    }

    @Test
    void onlyStaffInviteAndNeverAboveTheirOwnRole() throws Exception {
        assertThat(emailInvites(staff, nia.email(), "OWNER")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(emailInvites(staff, "nobody here", "MEMBER")).hasStatus(HttpStatus.BAD_REQUEST);

        String code = read(post(owner, path("/invites"), "{\"role\": \"MEMBER\"}"), "$.code");
        assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(mo.token())).hasStatusOk();
        assertThat(emailInvites(mo, nia.email(), "MEMBER")).hasStatus(HttpStatus.FORBIDDEN);

        // A link invite is listed without an address, so there's nothing to resend to
        String linkInvite = read(post(owner, path("/invites"), "{\"role\": \"MEMBER\"}"), "$.id");
        assertThat(mvc.get().uri(path("/invites")).with(owner.token())).hasStatusOk().bodyJson()
                .hasPathSatisfying("$[0].id", i -> assertThat(i).isEqualTo(linkInvite))
                .hasPathSatisfying("$[0].email", e -> assertThat(e).isNull());
        assertThat(mvc.post().uri(path("/invites/" + linkInvite + "/resend")).with(owner.token()))
                .hasStatus(HttpStatus.CONFLICT);
    }

    private List<Email> awaitEmailTo(String address, int count) {
        await().atMost(WAIT).until(() -> emails.sentTo(address).size() >= count);
        return emails.sentTo(address);
    }

    private static String codeIn(Email email) {
        Matcher matcher = LINK.matcher(email.text());
        assertThat(matcher.find()).as("a link in: %s", email.text()).isTrue();
        return matcher.group(1);
    }

    private MvcTestResult emailInvites(TestUser user, String text, String role) {
        return mvc.post().uri(path("/invites/emails")).with(user.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\": %s, \"role\": \"%s\"}".formatted(json(text), role)).exchange();
    }

    private static String json(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
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
