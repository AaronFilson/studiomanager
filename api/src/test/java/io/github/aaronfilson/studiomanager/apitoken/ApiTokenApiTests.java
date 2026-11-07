package io.github.aaronfilson.studiomanager.apitoken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.RecordingEmailSender;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.mail.Email;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionTemplate;

/** Personal API tokens, end to end through the real security filter chain. */
@IntegrationTest
class ApiTokenApiTests {

  @Autowired MockMvcTester mvc;

  @Autowired JdbcClient jdbc;

  @Autowired TransactionTemplate transactions;

  @Autowired RecordingEmailSender emails;

  private final TestUser owner = TestUser.named("Olive");
  private final TestUser sam = TestUser.named("Sam");
  private final TestUser mo = TestUser.named("Mo");
  private UUID studioId;
  private UUID otherStudioId;

  @BeforeEach
  void setUp() {
    studioId = studio(owner, "Clay Club");
    otherStudioId = studio(owner, "Second Studio");
  }

  @Test
  void aTokenActsAsItsPersonInsideItsStudioAndScopes() {
    MvcTestResult created =
        post(
            owner,
            path("/api-tokens"),
            """
            {"name": "Claude desktop", "scopes": ["read", "write:pieces"], "expiresInDays": 30}
            """);
    assertThat(created).hasStatus(HttpStatus.CREATED);
    String secret = read(created, "$.secret");
    assertThat(secret).startsWith("smpat_").hasSize(49);
    assertThat(created)
        .bodyJson()
        .extractingPath("$.token.lastFour")
        .isEqualTo(secret.substring(45));
    // Never shown again
    assertThat(mvc.get().uri(path("/api-tokens")).with(owner.token()))
        .hasStatusOk()
        .bodyText()
        .contains("Claude desktop")
        .doesNotContain(secret);

    assertThat(withToken(secret, "GET", path(""))).hasStatusOk();
    assertThat(withToken(secret, "GET", path("/kilns"))).hasStatusOk();
    assertThat(withToken(secret, "GET", path("/today"))).hasStatusOk();

    // Outside its studio, its scopes, or into money and administration: refused
    assertThat(withToken(secret, "GET", "/api/v1/me")).hasStatus(HttpStatus.FORBIDDEN);
    assertThat(withToken(secret, "GET", "/api/v1/studios/" + otherStudioId))
        .hasStatus(HttpStatus.FORBIDDEN);
    assertThat(withToken(secret, "GET", path("/ledger/journal?from=2026-01-01&to=2026-02-01")))
        .hasStatus(HttpStatus.FORBIDDEN);
    assertThat(withToken(secret, "GET", path("/export"))).hasStatus(HttpStatus.FORBIDDEN);
    assertThat(withToken(secret, "GET", path("/api-tokens"))).hasStatus(HttpStatus.FORBIDDEN);
    MvcTestResult noScope =
        mvc.post()
            .uri(path("/supplies"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + secret)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}")
            .exchange();
    assertThat(noScope).hasStatus(HttpStatus.FORBIDDEN);
    assertThat(noScope).bodyText().contains("write:supplies");
  }

  @Test
  void aRevokedOrMadeUpTokenIsRefused() {
    String secret = token(owner, "Script");
    String id = JsonPath.read(body(owner, path("/api-tokens")), "$[0].id");

    assertThat(mvc.delete().uri(path("/api-tokens/" + id)).with(owner.token()))
        .hasStatus(HttpStatus.NO_CONTENT);

    assertThat(withToken(secret, "GET", path(""))).hasStatus(HttpStatus.UNAUTHORIZED);
    assertThat(withToken("smpat_" + "x".repeat(43), "GET", path("")))
        .hasStatus(HttpStatus.UNAUTHORIZED);
    assertThat(withToken("smpat_short", "GET", path(""))).hasStatus(HttpStatus.UNAUTHORIZED);
    // People's sign-in is untouched
    assertThat(mvc.get().uri(path("")).with(owner.token())).hasStatusOk();
  }

  @Test
  void tokensAreForStaffAndStopWhenTheirPersonIsNoLonger() {
    join(mo, "MEMBER");
    assertThat(post(mo, path("/api-tokens"), "{\"name\": \"Mine\", \"scopes\": [\"read\"]}"))
        .hasStatus(HttpStatus.FORBIDDEN);

    UUID samId = join(sam, "STAFF");
    String secret = token(sam, "Sam's assistant");
    assertThat(withToken(secret, "GET", path(""))).hasStatusOk();
    // Sam can't see or revoke the owner's tokens, but the owner sees Sam's
    token(owner, "Owner's");
    assertThat(mvc.get().uri(path("/api-tokens")).with(sam.token()))
        .bodyJson()
        .extractingPath("$.length()")
        .isEqualTo(1);
    assertThat(mvc.get().uri(path("/api-tokens")).with(owner.token()))
        .bodyJson()
        .extractingPath("$.length()")
        .isEqualTo(2);

    demote(samId);

    assertThat(withToken(secret, "GET", path(""))).hasStatus(HttpStatus.FORBIDDEN);
  }

  @Test
  void aTokensOwnerIsEmailedWhenItIsRevoked() {
    join(sam, "STAFF");
    token(sam, "Sam's assistant");
    String id = JsonPath.read(body(sam, path("/api-tokens")), "$[0].id");

    assertThat(mvc.delete().uri(path("/api-tokens/" + id)).with(owner.token()))
        .hasStatus(HttpStatus.NO_CONTENT);
    // Revoking again changes nothing and sends nothing more
    assertThat(mvc.delete().uri(path("/api-tokens/" + id)).with(owner.token()))
        .hasStatus(HttpStatus.NO_CONTENT);

    await().atMost(Duration.ofSeconds(10)).until(() -> !emails.sentTo(sam.email()).isEmpty());
    Email email = emails.sentTo(sam.email()).getFirst();
    assertThat(email.subject()).isEqualTo("Your API token \"Sam's assistant\" was revoked");
    assertThat(email.text()).contains("Olive revoked it.");
    assertThat(emails.sentTo(sam.email())).hasSize(1);
  }

  @Test
  void aTokenNotesWhereItWasUsedAndEachCallIsLogged() {
    String secret = token(owner, "Sync script");
    UUID tokenId = UUID.fromString(JsonPath.read(body(owner, path("/api-tokens")), "$[0].id"));

    assertThat(withToken(secret, "GET", path("/kilns"))).hasStatusOk();
    assertThat(withToken(secret, "GET", path("/export"))).hasStatus(HttpStatus.FORBIDDEN);
    assertThat(withToken(secret, "GET", path("/today"))).hasStatusOk();

    assertThat(
            asOwner(() ->
                jdbc.sql("select last_used_from from api_token where id = :id")
                    .param("id", tokenId)
                    .query(String.class)
                    .single()))
        .isEqualTo("127.0.0.1");
    assertThat((String) JsonPath.read(body(owner, path("/api-tokens")), "$[0].lastUsedFrom"))
        .isEqualTo("127.0.0.1");
    // Refused before it reached a route, by the token's scopes, so only the two that ran are logged
    assertThat(
            asOwner(() ->
                jdbc.sql(
                        """
                        select target || ' ' || outcome || ' ' || status from api_call_log
                        where token_id = :id and channel = 'TOKEN' order by created_at
                        """)
                    .param("id", tokenId)
                    .query(String.class)
                    .list()))
        .containsExactly(
            "GET /api/v1/studios/{studioId}/kilns OK 200",
            "GET /api/v1/studios/{studioId}/today OK 200");
  }

  @Test
  void aTokenUnusedFor90DaysStopsWorking() {
    String secret = token(owner, "Forgotten script");
    UUID tokenId = UUID.fromString(JsonPath.read(body(owner, path("/api-tokens")), "$[0].id"));
    asOwner(() ->
        jdbc.sql(
                """
                update api_token set created_at = now() - interval '100 days',
                                     last_used_at = now() - interval '91 days'
                where id = :id
                """)
            .param("id", tokenId)
            .update());

    assertThat(withToken(secret, "GET", path(""))).hasStatus(HttpStatus.UNAUTHORIZED);
    // Its expiry shows the day it stopped, not the one it was made with
    String expiresAt = JsonPath.read(body(owner, path("/api-tokens")), "$[0].expiresAt");
    assertThat(java.time.OffsetDateTime.parse(expiresAt)).isBefore(java.time.OffsetDateTime.now());
  }

  private <T> T asOwner(java.util.function.Supplier<T> work) {
    UUID ownerId =
        UUID.fromString(
            read(mvc.get().uri("/api/v1/me").with(owner.token()).exchange(), "$.account.id"));
    return TenantContext.callAs(
        new TenantContext(ownerId, studioId, StudioRole.OWNER),
        () -> transactions.execute(tx -> work.get()));
  }

  private void demote(UUID accountId) {
    asOwner(() ->
        jdbc.sql(
                "update studio_membership set role = 'MEMBER' where studio_id = :studio and account_id = :account")
            .param("studio", studioId)
            .param("account", accountId)
            .update());
  }

  private String token(TestUser who, String name) {
    return read(
        post(
            who,
            path("/api-tokens"),
            """
            {"name": "%s", "scopes": ["read"]}
            """
                .formatted(name)),
        "$.secret");
  }

  private MvcTestResult withToken(String secret, String method, String uri) {
    return mvc.method(org.springframework.http.HttpMethod.valueOf(method))
        .uri(uri)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + secret)
        .exchange();
  }

  private UUID studio(TestUser who, String name) {
    return UUID.fromString(
        read(
            post(
                who,
                "/api/v1/studios",
                """
                {"name": "%s", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
                """
                    .formatted(name)),
            "$.id"));
  }

  private UUID join(TestUser who, String role) {
    String code =
        read(post(owner, path("/invites"), "{\"role\": \"%s\"}".formatted(role)), "$.code");
    assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(who.token()))
        .hasStatusOk();
    return UUID.fromString(
        read(mvc.get().uri("/api/v1/me").with(who.token()).exchange(), "$.account.id"));
  }

  private String body(TestUser who, String uri) {
    try {
      return mvc.get().uri(uri).with(who.token()).exchange().getResponse().getContentAsString();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
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

  private static String read(MvcTestResult result, String jsonPath) {
    try {
      return JsonPath.read(result.getResponse().getContentAsString(), jsonPath).toString();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
