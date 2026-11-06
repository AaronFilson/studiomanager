package io.github.aaronfilson.studiomanager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.SystemActor;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionOperations;

/** Background jobs act as their own system accounts, which no person can become (ADR 16). */
@IntegrationTest
class SystemPrincipalTests {

  @Autowired MockMvcTester mvc;

  @Autowired JdbcClient jdbc;

  @Autowired TransactionOperations transactions;

  private final TestUser owner = TestUser.named("Olive");

  @Test
  void everyActorHasItsSeededSystemAccount() {
    for (SystemActor actor : SystemActor.values()) {
      Account account =
          jdbc.sql("select issuer, subject, kind from account where id = ?")
              .param(actor.accountId())
              .query(Account.class)
              .single();
      assertThat(account)
          .as(actor.name())
          .isEqualTo(new Account(SystemActor.ISSUER, actor.subject(), "SYSTEM"));
    }
  }

  @Test
  void aTokenNamingTheSystemIssuerCantSignIn() {
    String subject = SystemActor.WEBHOOK_SWEEP.subject();
    assertThat(
            mvc.get()
                .uri("/api/v1/me")
                .with(
                    jwt()
                        .jwt(token -> token.issuer(SystemActor.ISSUER).subject(subject))
                        .authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"))))
        .hasStatus(HttpStatus.FORBIDDEN);
  }

  @Test
  void onlyAJobCanCallTheJobOnlyFunctions() throws Exception {
    UUID studioId = createStudio();
    UUID ownerId = accountIdOf(owner);
    TenantContext person = TenantContext.forAccount(ownerId).inStudio(studioId, StudioRole.OWNER);
    String sql = "select count(*) from app.due_webhook_deliveries(now(), 10)";

    assertThatThrownBy(() -> inTenant(person, () -> jdbc.sql(sql).query(Integer.class).single()))
        .rootCause()
        .hasMessageContaining("Only a background job can call this");
    assertThatThrownBy(
            () -> transactions.execute(tx -> jdbc.sql(sql).query(Integer.class).single()))
        .rootCause()
        .hasMessageContaining("Only a background job can call this");
    assertThat(
            inTenant(
                TenantContext.system(SystemActor.WEBHOOK_SWEEP),
                () -> jdbc.sql(sql).query(Integer.class).single()))
        .isNotNull();
  }

  @Test
  void aSystemAccountCantJoinAStudio() throws Exception {
    UUID studioId = createStudio();
    TenantContext person =
        TenantContext.forAccount(accountIdOf(owner)).inStudio(studioId, StudioRole.OWNER);

    assertThatThrownBy(() ->
            inTenant(
                person,
                () ->
                    jdbc.sql(
                            """
                            insert into studio_membership (id, studio_id, account_id, role, created_at, version)
                            values (:id, :studioId, :accountId, 'STAFF', :now, 0)
                            """)
                        .param("id", Uuids.v7())
                        .param("studioId", studioId)
                        .param("accountId", SystemActor.MAINTENANCE_REMINDERS.accountId())
                        .param("now", OffsetDateTime.now())
                        .update()))
        .rootCause()
        .hasMessageContaining("A system account can't be a member of a studio");
  }

  @Test
  void aJobSeesOnlyTheStudioItIsBoundTo() throws Exception {
    UUID studioId = createStudio();
    TestUser other = TestUser.named("Ozzie");
    createStudio(other);

    assertThat(
            inTenant(
                TenantContext.system(SystemActor.MAINTENANCE_REMINDERS, studioId),
                () -> jdbc.sql("select id from studio").query(UUID.class).list()))
        .containsExactly(studioId);
    // Outside any studio it sees none: the cross-tenant functions are its only way across
    assertThat(
            inTenant(
                TenantContext.system(SystemActor.MAINTENANCE_REMINDERS),
                () -> jdbc.sql("select count(*) from studio").query(Integer.class).single()))
        .isZero();
  }

  private <T> T inTenant(TenantContext context, Supplier<T> work) {
    return TenantContext.callAs(context, () -> transactions.execute(tx -> work.get()));
  }

  private UUID createStudio() throws Exception {
    return createStudio(owner);
  }

  private UUID createStudio(TestUser user) throws Exception {
    MvcTestResult result =
        mvc.post()
            .uri("/api/v1/studios")
            .with(user.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"name": "System Clay", "unitSystem": "METRIC", "timeZone": "Europe/London", "country": "GB"}
                """)
            .exchange();
    assertThat(result).hasStatus(HttpStatus.CREATED);
    return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
  }

  private UUID accountIdOf(TestUser user) throws Exception {
    MvcTestResult result = mvc.get().uri("/api/v1/me").with(user.token()).exchange();
    assertThat(result).hasStatusOk();
    return UUID.fromString(
        JsonPath.read(result.getResponse().getContentAsString(), "$.account.id"));
  }

  record Account(String issuer, String subject, String kind) {}
}
