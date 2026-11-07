package io.github.aaronfilson.studiomanager.studio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Several co-owners per studio, and never none (plan 21, lane R8): the API refuses the last owner,
 * the owner rows are locked so a race can't leave none, and the V40 trigger refuses it even past
 * the API. Every owner is told of a change, and the billing contact is handed over.
 */
@IntegrationTest
class CoOwnerTests {

  @Autowired MockMvcTester mvc;
  @Autowired StudioService studios;
  @Autowired JdbcClient jdbc;
  @Autowired TransactionTemplate transactions;

  private final TestUser olive = TestUser.named("Olive");
  private final TestUser pat = TestUser.named("Pat");
  private final TestUser sam = TestUser.named("Sam");
  private UUID studioId;
  private UUID oliveId;
  private UUID patId;
  private UUID samId;

  @BeforeEach
  void studio() throws Exception {
    studioId =
        UUID.fromString(
            read(
                post(
                    olive,
                    "/api/v1/studios",
                    """
                    {"name": "Clay Club", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
                    """),
                "$.id"));
    for (TestUser staff : List.of(pat, sam)) {
      String code = read(post(olive, path("/invites"), "{\"role\": \"STAFF\"}"), "$.code");
      assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(staff.token()))
          .hasStatusOk();
    }
    oliveId = accountOf(olive);
    patId = accountOf(pat);
    samId = accountOf(sam);
  }

  @Test
  void anOwnerAddsACoOwnerAndEveryOwnerIsTold() throws Exception {
    assertThat(addOwner(olive, patId)).hasStatus(HttpStatus.NO_CONTENT);

    assertThat(roleOf(patId)).isEqualTo("OWNER");
    assertThat(ownerNotices(olive)).containsExactly("Pat is now an owner");
    assertThat(ownerNotices(pat)).containsExactly("Pat is now an owner");
    assertThat(ownerNotices(sam)).isEmpty();
    // The new owner can now manage owners themselves
    assertThat(addOwner(pat, samId)).hasStatus(HttpStatus.NO_CONTENT);
    assertThat(ownerNotices(olive)).contains("Sam is now an owner");
  }

  @Test
  void staffCantAddOwners() {
    assertThat(addOwner(pat, samId)).hasStatus(HttpStatus.FORBIDDEN);
    assertThat(roleOf(samId)).isEqualTo("STAFF");
  }

  @Test
  void theLastOwnerCantStepDown() throws Exception {
    MvcTestResult refused = removeOwner(olive, oliveId);

    assertThat(refused).hasStatus(HttpStatus.CONFLICT);
    assertThat(refused.getResponse().getContentAsString()).contains("Add another owner first");
    assertThat(roleOf(oliveId)).isEqualTo("OWNER");
  }

  @Test
  void anOwnerStepsDownWhileAnotherRemains() {
    assertThat(addOwner(olive, patId)).hasStatus(HttpStatus.NO_CONTENT);

    assertThat(removeOwner(olive, oliveId)).hasStatus(HttpStatus.NO_CONTENT);

    assertThat(roleOf(oliveId)).isEqualTo("STAFF");
    // Pat is now the last owner
    assertThat(removeOwner(pat, patId)).hasStatus(HttpStatus.CONFLICT);
    assertThat(addOwner(olive, samId)).hasStatus(HttpStatus.FORBIDDEN);
  }

  @Test
  void theTriggerRefusesLeavingAStudioWithNoOwner() {
    assertThatThrownBy(() ->
            asOwner(
                oliveId,
                () ->
                    transactions.execute(tx ->
                        jdbc.sql(
                                "update studio_membership set role = 'STAFF' where studio_id = :studio and account_id = :account")
                            .param("studio", studioId)
                            .param("account", oliveId)
                            .update())))
        .hasStackTraceContaining("Add another owner first");
    assertThat(roleOf(oliveId)).isEqualTo("OWNER");
  }

  @Test
  void twoOwnersDemotingEachOtherAtOnceLeaveOneOwner() throws Exception {
    assertThat(addOwner(olive, patId)).hasStatus(HttpStatus.NO_CONTENT);
    CyclicBarrier together = new CyclicBarrier(2);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      List<Future<Boolean>> outcomes = new ArrayList<>();
      outcomes.add(pool.submit(() -> demoteTogether(together, oliveId, patId)));
      outcomes.add(pool.submit(() -> demoteTogether(together, patId, oliveId)));
      int succeeded = 0;
      for (Future<Boolean> outcome : outcomes) {
        if (outcome.get(30, TimeUnit.SECONDS)) {
          succeeded++;
        }
      }
      assertThat(succeeded).isEqualTo(1);
    } finally {
      pool.shutdownNow();
    }
    assertThat(List.of(roleOf(oliveId), roleOf(patId))).containsExactlyInAnyOrder("OWNER", "STAFF");
  }

  @Test
  void theBillingContactIsHandedOverWhenTheyStopBeingAnOwner() throws Exception {
    // The creator starts as the billing contact
    assertThat(billingContact(olive)).containsEntry("accountId", oliveId.toString());
    assertThat(billingContact(pat)).isEmpty();
    assertThat(setBillingContact(olive, patId)).hasStatus(HttpStatus.CONFLICT);

    assertThat(addOwner(olive, patId)).hasStatus(HttpStatus.NO_CONTENT);
    assertThat(removeOwner(pat, oliveId)).hasStatus(HttpStatus.NO_CONTENT);

    assertThat(billingContact(pat))
        .doesNotContainKey("accountId")
        .containsEntry("chooseNeeded", true);
    assertThat(ownerNotices(pat)).contains("Olive is no longer an owner");
    assertThat(setBillingContact(pat, patId)).hasStatusOk();
    assertThat(billingContact(pat))
        .containsEntry("accountId", patId.toString())
        .containsEntry("chooseNeeded", false);

    Long logged =
        asOwner(
            patId,
            () ->
                transactions.execute(tx ->
                    jdbc.sql("select count(*) from role_change_log where kind = 'BILLING_CONTACT'")
                        .query(Long.class)
                        .single()));
    assertThat(logged).isEqualTo(2L);
  }

  @Test
  void theTriggerRefusesABillingContactWhoIsntAnOwner() {
    assertThatThrownBy(() ->
            asOwner(
                oliveId,
                () ->
                    transactions.execute(tx ->
                        jdbc.sql(
                                "update studio set billing_contact_account_id = :account where id = :studio")
                            .param("studio", studioId)
                            .param("account", samId)
                            .update())))
        .hasStackTraceContaining("billing contact must be an owner");
  }

  @Test
  void acceptingAnOwnerInviteTellsTheOwners() throws Exception {
    TestUser newcomer = TestUser.named("Nia");
    String code = read(post(olive, path("/invites"), "{\"role\": \"OWNER\"}"), "$.code");
    assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(newcomer.token()))
        .hasStatusOk();

    await()
        .atMost(Duration.ofSeconds(10))
        .until(() -> ownerNotices(olive).contains("Nia is now an owner"));
  }

  /**
   * Waits for the other thread, then demotes {@code other} as {@code self}; true if it went
   * through.
   */
  private boolean demoteTogether(CyclicBarrier together, UUID self, UUID other) throws Exception {
    together.await(10, TimeUnit.SECONDS);
    try {
      asOwner(
          self,
          () -> {
            studios.removeOwner(studioId, other);
            return other;
          });
      return true;
    } catch (RuntimeException refused) {
      return false;
    }
  }

  private <T> T asOwner(UUID accountId, ScopedValue.CallableOp<T, RuntimeException> operation) {
    return TenantContext.callAs(
        new TenantContext(accountId, studioId, StudioRole.OWNER), operation);
  }

  private String roleOf(UUID accountId) {
    return asOwner(
        oliveId,
        () ->
            transactions.execute(tx ->
                jdbc.sql(
                        "select role from studio_membership where studio_id = :studio and account_id = :account")
                    .param("studio", studioId)
                    .param("account", accountId)
                    .query(String.class)
                    .single()));
  }

  private MvcTestResult addOwner(TestUser user, UUID accountId) {
    return mvc.put().uri(path("/members/" + accountId + "/owner")).with(user.token()).exchange();
  }

  private MvcTestResult removeOwner(TestUser user, UUID accountId) {
    return mvc.delete().uri(path("/members/" + accountId + "/owner")).with(user.token()).exchange();
  }

  private MvcTestResult setBillingContact(TestUser user, UUID accountId) {
    return mvc.put()
        .uri(path("/members/billing-contact"))
        .with(user.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"accountId\": \"" + accountId + "\"}")
        .exchange();
  }

  /** The billing contact as the user sees it, or empty when they may not. */
  private Map<String, Object> billingContact(TestUser user) throws Exception {
    MvcTestResult result =
        mvc.get().uri(path("/members/billing-contact")).with(user.token()).exchange();
    if (result.getResponse().getStatus() != 200) {
      return Map.of();
    }
    return JsonPath.read(result.getResponse().getContentAsString(), "$");
  }

  /** The titles of someone's owner-change notices in the studio, newest first. */
  private List<String> ownerNotices(TestUser user) throws Exception {
    String body =
        mvc.get()
            .uri(path("/notifications"))
            .with(user.token())
            .exchange()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.items[?(@.kind == 'OWNERS_CHANGED')].title");
  }

  private UUID accountOf(TestUser user) throws Exception {
    return UUID.fromString(
        read(mvc.get().uri("/api/v1/me").with(user.token()).exchange(), "$.account.id"));
  }

  private MvcTestResult post(TestUser user, String uri, String json) {
    return mvc.post()
        .uri(uri)
        .with(user.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .exchange();
  }

  private String path(String suffix) {
    return "/api/v1/studios/" + studioId + suffix;
  }

  private static String read(MvcTestResult result, String jsonPath) throws Exception {
    return JsonPath.read(result.getResponse().getContentAsString(), jsonPath).toString();
  }
}
