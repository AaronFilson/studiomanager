package io.github.aaronfilson.studiomanager.studio;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.StatementCounts;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Studio, members and invites on Hibernate (ADR 21): lists at a fixed number of statements, and
 * invites, which carry {@code @TenantId}, seen only in their own studio.
 */
@IntegrationTest
class StudioPersistenceTests {

  @Autowired MockMvcTester mvc;
  @Autowired StatementCounts statements;
  @Autowired StudioMembers members;
  @Autowired StudioInvites invites;
  @Autowired StudioInviteRepository inviteRepository;
  @Autowired TransactionOperations transactions;

  private final TestUser owner = TestUser.named("Ola");
  private UUID studioId;
  private UUID ownerAccountId;

  @BeforeEach
  void studio() throws Exception {
    studioId = UUID.fromString(read(createStudio(owner, "Fixed Members"), "$.id"));
    ownerAccountId = accountOf(owner);
  }

  @Test
  void theMemberListIsTheSameNumberOfQueriesForOneMemberOrMany() throws Exception {
    long forOne = statements.of(() -> TenantContext.callAs(inStudio(), members::list)).statements();

    join(TestUser.named("Ben"));
    join(TestUser.named("Cai"));
    StatementCounts.Counted<List<MemberView>> forThree =
        statements.of(() -> TenantContext.callAs(inStudio(), members::list));

    assertThat(forThree.result()).hasSize(3);
    assertThat(forThree.statements()).isEqualTo(forOne);
  }

  @Test
  void openInvitesAreTheSameNumberOfQueriesForOneInviteOrMany() throws Exception {
    invite("MEMBER");
    long forOne =
        statements.of(() -> TenantContext.callAs(inStudio(), invites::openInvites)).statements();

    invite("MEMBER");
    invite("STAFF");
    StatementCounts.Counted<List<OpenInviteView>> forThree =
        statements.of(() -> TenantContext.callAs(inStudio(), invites::openInvites));

    assertThat(forThree.result()).hasSize(3);
    assertThat(forThree.statements()).isEqualTo(forOne);
  }

  @Test
  void hibernateSeesOnlyTheBoundStudiosInvitesAndNoneWithNoStudioBound() throws Exception {
    UUID ours = UUID.fromString(read(invite("MEMBER"), "$.id"));
    TestUser stranger = TestUser.named("Sol");
    UUID theirStudio = UUID.fromString(read(createStudio(stranger, "Elsewhere"), "$.id"));
    TenantContext theirs = new TenantContext(accountOf(stranger), theirStudio, StudioRole.OWNER);

    assertThat(TenantContext.callAs(theirs, () -> findInvite(ours))).isFalse();
    assertThat(
            TenantContext.callAs(TenantContext.forAccount(ownerAccountId), () -> findInvite(ours)))
        .isFalse();
    assertThat(TenantContext.callAs(inStudio(), () -> findInvite(ours))).isTrue();
  }

  private boolean findInvite(UUID inviteId) {
    return Boolean.TRUE.equals(
        transactions.execute(status -> inviteRepository.findById(inviteId).isPresent()));
  }

  private void join(TestUser person) throws Exception {
    String code = read(invite("MEMBER"), "$.code");
    assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(person.token()))
        .hasStatusOk();
  }

  private MvcTestResult invite(String role) {
    return mvc.post()
        .uri("/api/v1/studios/" + studioId + "/invites")
        .with(owner.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"role\": \"%s\"}".formatted(role))
        .exchange();
  }

  private TenantContext inStudio() {
    return new TenantContext(ownerAccountId, studioId, StudioRole.OWNER);
  }

  private UUID accountOf(TestUser user) throws Exception {
    return UUID.fromString(
        read(mvc.get().uri("/api/v1/me").with(user.token()).exchange(), "$.account.id"));
  }

  private MvcTestResult createStudio(TestUser user, String name) {
    return mvc.post()
        .uri("/api/v1/studios")
        .with(user.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(
            """
            {"name": "%s", "unitSystem": "METRIC", "timeZone": "Europe/London", "country": "GB"}
            """
                .formatted(name))
        .exchange();
  }

  private static String read(MvcTestResult result, String jsonPath) throws Exception {
    return JsonPath.read(result.getResponse().getContentAsString(), jsonPath).toString();
  }
}
