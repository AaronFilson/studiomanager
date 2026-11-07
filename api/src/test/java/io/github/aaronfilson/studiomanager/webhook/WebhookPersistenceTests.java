package io.github.aaronfilson.studiomanager.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.StatementCounts;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Webhooks on Hibernate (ADR 21): the delivery log is a fixed number of statements however long it
 * is, and an event recorded twice under one key adds nothing the second time.
 */
@IntegrationTest
class WebhookPersistenceTests {

  @Autowired MockMvcTester mvc;
  @Autowired StatementCounts statements;
  @Autowired WebhookEndpoints endpoints;
  @Autowired WebhookEvents events;
  @Autowired TransactionOperations transactions;

  private final TestUser owner = TestUser.named("Wren");
  private UUID studioId;
  private UUID ownerAccountId;
  private UUID endpointId;

  @BeforeEach
  void studioWithAnEndpoint() throws Exception {
    studioId =
        UUID.fromString(
            read(
                post(
                    "/api/v1/studios",
                    """
                    {"name": "Hooked", "unitSystem": "METRIC", "timeZone": "Europe/London", "country": "GB"}
                    """),
                "$.id"));
    ownerAccountId =
        UUID.fromString(
            read(mvc.get().uri("/api/v1/me").with(owner.token()).exchange(), "$.account.id"));
    MvcTestResult created =
        post(
            "/api/v1/studios/" + studioId + "/webhooks/endpoints",
            """
            {"url": "http://127.0.0.1:9/hook", "eventTypes": ["member.joined"]}
            """);
    assertThat(created).hasStatus(HttpStatus.CREATED);
    endpointId = UUID.fromString(read(created, "$.endpoint.id"));
  }

  @Test
  void theDeliveryLogIsTheSameNumberOfQueriesForOneDeliveryOrMany() throws Exception {
    recordPing();
    long forOne = countLog();

    recordPing();
    recordPing();
    long forThree = countLog();

    assertThat(TenantContext.callAs(inStudio(), () -> endpoints.deliveries(endpointId)).items())
        .hasSize(3);
    assertThat(forThree).isEqualTo(forOne);
  }

  @Test
  void anEventRecordedTwiceUnderOneKeyIsStoredOnce() {
    String key = "ping:" + Uuids.v7();
    assertThat(record(key)).isPresent();
    assertThat(record(key)).isEmpty();
    assertThat(TenantContext.callAs(inStudio(), () -> endpoints.deliveries(endpointId)).items())
        .hasSize(1);
  }

  private void recordPing() {
    assertThat(record("ping:" + Uuids.v7())).isPresent();
  }

  private Optional<WebhookEvents.Recorded> record(String key) {
    return TenantContext.callAs(
        inStudio(),
        () ->
            transactions.execute(tx ->
                events.record(
                    WebhookEventType.PING, key, Map.of("message", "hello"), List.of(endpointId))));
  }

  private long countLog() throws Exception {
    return statements
        .of(() -> TenantContext.callAs(inStudio(), () -> endpoints.deliveries(endpointId)))
        .statements();
  }

  private TenantContext inStudio() {
    return new TenantContext(ownerAccountId, studioId, StudioRole.OWNER);
  }

  private MvcTestResult post(String uri, String json) {
    return mvc.post()
        .uri(uri)
        .with(owner.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .exchange();
  }

  private static String read(MvcTestResult result, String jsonPath) throws Exception {
    return JsonPath.read(result.getResponse().getContentAsString(), jsonPath).toString();
  }
}
