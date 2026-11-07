package io.github.aaronfilson.studiomanager.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.invoice.InvoiceReaderKind;
import io.github.aaronfilson.studiomanager.invoice.PlatformReaderRules;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.io.IOException;
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

/** The admin's controls for the platform-paid invoice readers (docs/planning/20). */
@IntegrationTest
class InvoiceReaderAdminApiTests {

  @Autowired MockMvcTester mvc;

  @Autowired JdbcClient jdbc;

  @Autowired TransactionTemplate transactions;

  @Autowired PlatformReaderRules rules;

  private final TestUser admin = TestUser.named("Ada");
  private final TestUser owner = TestUser.named("Olive");

  @BeforeEach
  void signIn() {
    // Each person's account exists before anything names it
    assertThat(mvc.get().uri("/api/v1/me").with(admin.adminToken())).hasStatusOk();
    assertThat(mvc.get().uri("/api/v1/me").with(owner.token())).hasStatusOk();
  }

  @AfterEach
  void backToTheDefaults() {
    assertThat(send(admin, "DELETE", "/api/v1/admin/invoice-readers", "")).hasStatusOk();
  }

  @Test
  void withNoDemoStudioConfiguredTheDefaultsAreOffForEveryone() {
    MvcTestResult overview =
        mvc.get().uri("/api/v1/admin/invoice-readers").with(admin.adminToken()).exchange();

    assertThat(overview).hasStatusOk();
    assertThat(overview).bodyJson().extractingPath("$.saved").isEqualTo(false);
    assertThat(overview).bodyJson().extractingPath("$.settings.readers.length()").isEqualTo(2);
    assertThat(overview).bodyJson().extractingPath("$.settings.readers[0].scope").isEqualTo("NONE");
    assertThat(overview).bodyJson().extractingPath("$.usage[0].pages").isEqualTo(0);
    assertThat(rules.current().rule(InvoiceReaderKind.GOOGLE_DOCUMENT_AI).covers(UUID.randomUUID()))
        .isFalse();
  }

  @Test
  void theAdminTurnsAReaderOnForOneStudioWithItsOwnOrder() {
    UUID studioId = createStudio(owner);
    String body =
        """
        {"readers": [{"reader": "GOOGLE_DOCUMENT_AI", "enabled": true, "scope": "CHOSEN",
                      "studioIds": ["%s"], "studioMonthlyPages": 20, "platformMonthlyPages": 100}],
         "studioOrders": [{"studioId": "%s", "readers": ["GOOGLE_DOCUMENT_AI"]}]}
        """
            .formatted(studioId, studioId);

    MvcTestResult saved = send(admin, "PUT", "/api/v1/admin/invoice-readers", body);

    assertThat(saved).hasStatusOk();
    assertThat(saved).bodyJson().extractingPath("$.saved").isEqualTo(true);
    // Azure was left out, so it's off
    assertThat(saved)
        .bodyJson()
        .extractingPath("$.settings.readers[1].reader")
        .isEqualTo("AZURE_DOCUMENT_INTELLIGENCE");
    assertThat(saved).bodyJson().extractingPath("$.settings.readers[1].enabled").isEqualTo(false);
    PlatformReaderRules.Rules now = rules.current();
    assertThat(now.rule(InvoiceReaderKind.GOOGLE_DOCUMENT_AI).covers(studioId)).isTrue();
    assertThat(now.rule(InvoiceReaderKind.GOOGLE_DOCUMENT_AI).covers(UUID.randomUUID())).isFalse();
    assertThat(now.order(studioId)).containsExactly(InvoiceReaderKind.GOOGLE_DOCUMENT_AI);
    assertThat(now.order(UUID.randomUUID())).startsWith(InvoiceReaderKind.CLAUDE);
  }

  @Test
  void rulesForClaudeOrUnknownStudiosAreRefused() {
    assertThat(
            send(
                admin,
                "PUT",
                "/api/v1/admin/invoice-readers",
                """
                {"readers": [{"reader": "CLAUDE", "enabled": true, "scope": "ALL", "studioIds": [],
                              "studioMonthlyPages": 1, "platformMonthlyPages": 1}],
                 "studioOrders": []}
                """))
        .hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(
            send(
                admin,
                "PUT",
                "/api/v1/admin/invoice-readers",
                """
                {"readers": [], "studioOrders": [{"studioId": "%s", "readers": ["CLAUDE"]}]}
                """
                    .formatted(UUID.randomUUID())))
        .hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(mvc.get().uri("/api/v1/admin/invoice-readers").with(owner.token()))
        .hasStatus(HttpStatus.FORBIDDEN);
  }

  @Test
  void onlyAnAdminSeesEachStudiosUseButAnyoneSeesThePlatformTotal() {
    UUID ownerId =
        UUID.fromString(
            read(mvc.get().uri("/api/v1/me").with(owner.token()).exchange(), "$.account.id"));
    TenantContext person = TenantContext.forAccount(ownerId);

    assertThatThrownBy(() ->
            TenantContext.callAs(
                person,
                () ->
                    transactions.execute(tx ->
                        jdbc.sql("select studio_id from app.platform_reader_usage_by_studio(now())")
                            .query(UUID.class)
                            .list())))
        .rootCause()
        .hasMessageContaining("Only a platform admin");
    List<String> totals =
        TenantContext.callAs(
            person,
            () ->
                transactions.execute(tx ->
                    jdbc.sql(
                            "select reader from app.platform_reader_pages(now() - interval '1 day')")
                        .query(String.class)
                        .list()));
    assertThat(totals).isNotNull();
  }

  private UUID createStudio(TestUser user) {
    return UUID.fromString(
        read(
            send(
                user,
                "POST",
                "/api/v1/studios",
                """
                {"name": "Glaze Lab", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
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
