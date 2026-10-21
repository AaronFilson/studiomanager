package io.github.aaronfilson.studiomanager.notify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.RecordingEmailSender;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.mail.Email;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionTemplate;

/** Notices reach members by email, after commit, and a failed send is retried from the outbox. */
@IntegrationTest
class NotificationEmailTests {

    private static final Duration WAIT = Duration.ofSeconds(10);

    @Autowired
    MockMvcTester mvc;

    @Autowired
    RecordingEmailSender emails;

    @Autowired
    FailedEventPublications failed;

    @Autowired
    NotificationService notifications;

    @Autowired
    TransactionTemplate transactions;

    @Autowired
    JdbcClient jdbc;

    private final TestUser owner = TestUser.named("Olive");
    private final TestUser member = TestUser.named("Mo");
    private UUID studioId;

    @BeforeEach
    void studio() throws Exception {
        studioId = UUID.fromString(read(post(owner, "/api/v1/studios", """
                {"name": "Clay Club", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
                """), "$.id"));
        String code = read(post(owner, path("/invites"), "{\"role\": \"MEMBER\"}"), "$.code");
        assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(member.token())).hasStatusOk();
    }

    @Test
    void aNoticeIsEmailedToEachMember() {
        assertThat(post(owner, path("/notices"), """
                {"title": "Closed Monday", "body": "The studio is closed for kiln repairs."}
                """)).hasStatusOk();

        await().atMost(WAIT).until(() -> !emails.sentTo(member.email()).isEmpty());
        Email email = emails.sentTo(member.email()).getFirst();
        assertThat(email.subject()).isEqualTo("Clay Club: Closed Monday");
        assertThat(email.text())
                .startsWith("The studio is closed for kiln repairs.")
                .contains("http://localhost:5173/studios/" + studioId)
                .contains("a member of Clay Club");
        // Staff get studio notices too
        await().atMost(WAIT).until(() -> !emails.sentTo(owner.email()).isEmpty());
    }

    @Test
    void aNoticeThatRolledBackLeavesNothingInTheOutbox() throws Exception {
        UUID ownerId = UUID.fromString(read(mvc.get().uri("/api/v1/me").with(owner.token()).exchange(), "$.account.id"));
        String title = "Rolled back " + UUID.randomUUID();

        TenantContext.callAs(TenantContext.forAccount(ownerId).inStudio(studioId, StudioRole.OWNER),
                () -> transactions.execute(tx -> {
                    notifications.broadcast(title, "Never sent.");
                    tx.setRollbackOnly();
                    return null;
                }));

        Integer queued = jdbc.sql("select count(*) from event_publication where serialized_event like :title")
                .param("title", "%" + title + "%").query(Integer.class).single();
        assertThat(queued).isZero();
        assertThat(emails.sentTo(member.email())).isEmpty();
    }

    @Test
    void aFailedSendStaysInTheOutboxAndIsRetried() {
        emails.failNextSendTo(member.email());
        assertThat(post(owner, path("/notices"), """
                {"title": "Glaze day", "body": "New glazes are on the shelf."}
                """)).hasStatusOk();
        // The notice is saved, and visible, even though the email failed
        assertThat(mvc.get().uri(path("/notifications")).with(member.token()))
                .hasStatusOk().bodyJson().extractingPath("$[0].title").isEqualTo("Glaze day");
        await().atMost(WAIT).until(() -> !emails.sentTo(owner.email()).isEmpty());
        assertThat(emails.sentTo(member.email())).isEmpty();

        // What OutboxRetry does on its schedule, without the wait
        await().atMost(WAIT).untilAsserted(() -> {
            failed.resubmit(ResubmissionOptions.defaults());
            assertThat(emails.sentTo(member.email())).extracting(Email::subject).containsExactly("Clay Club: Glaze day");
        });
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
