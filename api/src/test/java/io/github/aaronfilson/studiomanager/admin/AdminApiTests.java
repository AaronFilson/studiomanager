package io.github.aaronfilson.studiomanager.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.tenancy.PlatformAdmin;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings;
import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings.Channel;
import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings.EffectiveLimit;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The admin API, its two layers of protection (the app role, then the database), and the rate
 * limits it changes.
 */
@IntegrationTest
class AdminApiTests {

  private static final String LIMITS =
      """
      {"mcp": {"burst": 45, "refillTokens": 2, "refillPeriodSeconds": 10, "dailyCap": 700},
       "token": {"burst": 250, "refillTokens": 3, "refillPeriodSeconds": 1, "dailyCap": 6000}}
      """;

  @Autowired MockMvcTester mvc;

  @Autowired JdbcClient jdbc;

  @Autowired TransactionTemplate transactions;

  @Autowired RateLimitSettings rateLimits;

  private final TestUser admin = TestUser.named("Ada");
  private final TestUser owner = TestUser.named("Olive");

  @BeforeEach
  void signIn() {
    // Each person's account exists before anything names it
    assertThat(mvc.get().uri("/api/v1/me").with(admin.adminToken())).hasStatusOk();
  }

  @AfterEach
  void backToNormal() {
    // The settings are shared by the whole application, so leave them as they were
    assertThat(send(admin, "DELETE", "/api/v1/admin/rate-limits/boost", "")).hasStatusOk();
    assertThat(send(admin, "DELETE", "/api/v1/admin/rate-limits/normal", "")).hasStatusOk();
  }

  @Test
  void onlyAPlatformAdminReachesTheAdminApi() {
    UUID studioId = createStudio(owner);

    assertThat(mvc.get().uri("/api/v1/admin/rate-limits").with(owner.token()))
        .hasStatus(HttpStatus.FORBIDDEN);
    assertThat(mvc.get().uri("/api/v1/admin/rate-limits").with(admin.tokenWithoutApiScope()))
        .hasStatus(HttpStatus.FORBIDDEN);
    assertThat(mvc.get().uri("/api/v1/admin/rate-limits")).hasStatus(HttpStatus.UNAUTHORIZED);

    String token =
        read(
            send(
                owner,
                "POST",
                "/api/v1/studios/" + studioId + "/api-tokens",
                "{\"name\": \"t\", \"scopes\": [\"read\"]}"),
            "$.secret");
    assertThat(
            mvc.get().uri("/api/v1/admin/rate-limits").header("Authorization", "Bearer " + token))
        .hasStatus(HttpStatus.FORBIDDEN);

    assertThat(mvc.get().uri("/api/v1/admin/rate-limits").with(admin.adminToken())).hasStatusOk();
  }

  @Test
  void meSaysWhoIsAnAdmin() {
    assertThat(mvc.get().uri("/api/v1/me").with(admin.adminToken()))
        .bodyJson()
        .extractingPath("$.platformAdmin")
        .isEqualTo(true);
    assertThat(mvc.get().uri("/api/v1/me").with(owner.token()))
        .bodyJson()
        .extractingPath("$.platformAdmin")
        .isEqualTo(false);
  }

  @Test
  void theConfiguredDefaultsApplyUntilOverriddenAndComeBackAfterwards() {
    MvcTestResult before =
        mvc.get().uri("/api/v1/admin/rate-limits").with(admin.adminToken()).exchange();
    assertThat(before).bodyJson().extractingPath("$.defaults.mcp.burst").isEqualTo(30);
    assertThat(before).bodyJson().extractingPath("$.normal.token.dailyCap").isEqualTo(5000);
    assertThat(before).bodyJson().extractingPath("$.override").isNull();

    MvcTestResult saved = send(admin, "PUT", "/api/v1/admin/rate-limits/normal", LIMITS);
    assertThat(saved).hasStatusOk();
    assertThat(saved).bodyJson().extractingPath("$.normal.mcp.burst").isEqualTo(45);
    assertThat(saved).bodyJson().extractingPath("$.override.token.dailyCap").isEqualTo(6000);
    // This replica re-reads at once after its own save
    assertThat(rateLimits.current(Channel.MCP, UUID.randomUUID()))
        .isInstanceOfSatisfying(
            EffectiveLimit.Limited.class,
            limited -> assertThat(limited.limits().burst()).isEqualTo(45));

    MvcTestResult reset = send(admin, "DELETE", "/api/v1/admin/rate-limits/normal", "");
    assertThat(reset).bodyJson().extractingPath("$.normal.mcp.burst").isEqualTo(30);
    assertThat(reset).bodyJson().extractingPath("$.override").isNull();
  }

  @Test
  void outOfRangeLimitsAreRefused() {
    assertThat(
            send(
                admin,
                "PUT",
                "/api/v1/admin/rate-limits/normal",
                LIMITS.replace("\"burst\": 45", "\"burst\": 0")))
        .hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(
            send(
                admin,
                "PUT",
                "/api/v1/admin/rate-limits/boost",
                "{\"unlimitedMinutes\": 200, \"easingMinutes\": 60}"))
        .hasStatus(HttpStatus.BAD_REQUEST);
  }

  @Test
  void aBoostForOneStudioLiftsItAndEndNowPutsItBack() {
    UUID demoStudio = UUID.randomUUID();
    MvcTestResult boosted =
        send(
            admin,
            "PUT",
            "/api/v1/admin/rate-limits/boost",
            "{\"studioId\": \"%s\", \"unlimitedMinutes\": 30, \"easingMinutes\": 60}"
                .formatted(demoStudio));

    assertThat(boosted).hasStatusOk();
    assertThat(boosted).bodyJson().extractingPath("$.boost.steps.length()").isEqualTo(4);
    assertThat(boosted).bodyJson().extractingPath("$.boost.steps[1].multiplier").isEqualTo(8);
    assertThat(rateLimits.current(Channel.MCP, demoStudio))
        .isInstanceOf(EffectiveLimit.Unlimited.class);
    assertThat(rateLimits.current(Channel.MCP, UUID.randomUUID()))
        .isInstanceOf(EffectiveLimit.Limited.class);

    MvcTestResult ended = send(admin, "DELETE", "/api/v1/admin/rate-limits/boost", "");
    assertThat(ended).bodyJson().extractingPath("$.boost").isNull();
    assertThat(rateLimits.current(Channel.MCP, demoStudio))
        .isInstanceOf(EffectiveLimit.Limited.class);
  }

  @Test
  void aScheduledBoostWaitsForItsStart() {
    OffsetDateTime later = Instant.now().plusSeconds(3_600).atOffset(ZoneOffset.UTC);
    MvcTestResult scheduled =
        send(
            admin,
            "PUT",
            "/api/v1/admin/rate-limits/boost",
            "{\"startsAt\": \"%s\", \"unlimitedMinutes\": 15, \"easingMinutes\": 0}"
                .formatted(later));

    assertThat(scheduled).hasStatusOk();
    assertThat(scheduled).bodyJson().extractingPath("$.boost.steps.length()").isEqualTo(1);
    assertThat(rateLimits.current(Channel.TOKEN, UUID.randomUUID()))
        .isInstanceOf(EffectiveLimit.Limited.class);
  }

  @Test
  void everyChangeIsInTheHistoryWithWhoMadeIt() {
    send(admin, "PUT", "/api/v1/admin/rate-limits/normal", LIMITS);
    send(admin, "DELETE", "/api/v1/admin/rate-limits/normal", "");

    MvcTestResult history =
        mvc.get().uri("/api/v1/admin/setting-changes?limit=2").with(admin.adminToken()).exchange();

    assertThat(history).hasStatusOk();
    assertThat(history).bodyJson().extractingPath("$.length()").isEqualTo(2);
    assertThat(history).bodyJson().extractingPath("$[0].key").isEqualTo("rate-limit.normal");
    assertThat(history).bodyJson().extractingPath("$[0].newValue").isNull();
    assertThat(history).bodyJson().extractingPath("$[0].oldValue.mcp.burst").isEqualTo(45);
    assertThat(history).bodyJson().extractingPath("$[0].changedByName").isEqualTo("Ada");
    assertThat(history).bodyJson().extractingPath("$[1].newValue.token.dailyCap").isEqualTo(6000);
  }

  @Test
  void theDatabaseRefusesSettingWritesWithoutTheAdminBinding() {
    UUID adminId =
        UUID.fromString(
            read(mvc.get().uri("/api/v1/me").with(admin.adminToken()).exchange(), "$.account.id"));
    TenantContext person = TenantContext.forAccount(adminId);

    assertThatThrownBy(() -> TenantContext.callAs(person, () -> writeSetting(adminId)))
        .rootCause()
        .hasMessageContaining("violates row-level security policy");
    // Bound as an admin, a write must still name the account that makes it
    assertThatThrownBy(() ->
            TenantContext.callAs(
                person, () -> PlatformAdmin.callAs(() -> writeSetting(UUID.randomUUID()))))
        .rootCause()
        .hasMessageContaining("violates row-level security policy");
    // And only an admin reads the history
    send(admin, "PUT", "/api/v1/admin/rate-limits/normal", LIMITS);
    List<String> seen =
        TenantContext.callAs(
            person,
            () ->
                transactions.execute(tx ->
                    jdbc.sql("select key from platform_setting_change")
                        .query(String.class)
                        .list()));
    assertThat(seen).isEmpty();
  }

  @Test
  void anAdminPicksFromEveryStudioButNobodyElseCanListThem() {
    UUID studioId = createStudio(owner);

    MvcTestResult studios =
        mvc.get().uri("/api/v1/admin/studios").with(admin.adminToken()).exchange();
    assertThat(studios).hasStatusOk();
    assertThat(read(studios, "$[?(@.id == '%s')].name".formatted(studioId))).contains("Clay Club");
    assertThat(mvc.get().uri("/api/v1/admin/studios").with(owner.token()))
        .hasStatus(HttpStatus.FORBIDDEN);
    MvcTestResult boosted =
        send(
            admin,
            "PUT",
            "/api/v1/admin/rate-limits/boost",
            "{\"studioId\": \"%s\", \"unlimitedMinutes\": 15, \"easingMinutes\": 0}"
                .formatted(studioId));
    assertThat(boosted).bodyJson().extractingPath("$.boost.studioName").isEqualTo("Clay Club");

    UUID ownerId =
        UUID.fromString(
            read(mvc.get().uri("/api/v1/me").with(owner.token()).exchange(), "$.account.id"));
    assertThatThrownBy(() ->
            TenantContext.callAs(
                TenantContext.forAccount(ownerId),
                () ->
                    transactions.execute(tx ->
                        jdbc.sql("select studio_id from app.studios_for_platform_admin()")
                            .query(UUID.class)
                            .list())))
        .rootCause()
        .hasMessageContaining("Only a platform admin");
  }

  private Integer writeSetting(UUID changedBy) {
    return transactions.execute(tx ->
        jdbc.sql(
                """
                insert into platform_setting (key, value, version, changed_by, changed_at)
                values ('test.forbidden', '{}', 1, :by, now())
                """)
            .param("by", changedBy)
            .update());
  }

  private UUID createStudio(TestUser user) {
    return UUID.fromString(
        read(
            send(
                user,
                "POST",
                "/api/v1/studios",
                """
                {"name": "Clay Club", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
                """),
            "$.id"));
  }

  private MvcTestResult send(TestUser user, String method, String uri, String body) {
    var request =
        mvc.method(HttpMethod.valueOf(method))
            .uri(uri)
            .with(user == admin ? user.adminToken() : user.token());
    return body.isEmpty()
        ? request.exchange()
        : request.contentType(MediaType.APPLICATION_JSON).content(body).exchange();
  }

  private static String read(MvcTestResult result, String jsonPath) {
    try {
      return JsonPath.read(result.getResponse().getContentAsString(), jsonPath).toString();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
