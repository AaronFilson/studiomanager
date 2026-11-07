package io.github.aaronfilson.studiomanager.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.StatementCounts;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Payables and the ledger on Hibernate (ADR 21): lists at a fixed number of statements, and
 * postings, which carry {@code @TenantId}, seen only in their own studio.
 */
@IntegrationTest
class LedgerPersistenceTests {

  @Autowired MockMvcTester mvc;
  @Autowired StatementCounts statements;
  @Autowired PayablesService payables;
  @Autowired Consignments consignments;
  @Autowired Payouts payouts;
  @Autowired JournalEntryRepository entries;
  @Autowired TransactionOperations transactions;

  private final TestUser owner = TestUser.named("Ori");
  private UUID studioId;
  private UUID ownerAccountId;

  @BeforeEach
  void studio() throws Exception {
    studioId = UUID.fromString(read(createStudio(owner, "Fixed Books"), "$.id"));
    ownerAccountId = accountOf(owner);
  }

  @Test
  void payeesItemsAndSalesListAtTheSameNumberOfQueriesForOneOrMany() throws Exception {
    sell(takeIn(payee("Ana")));
    long payeesForOne = count(payables::payees);
    long itemsForOne = count(() -> consignments.consignmentItems(null, false, false));
    long salesForOne = count(() -> consignments.consignmentSales(null));

    sell(takeIn(payee("Bea")));
    sell(takeIn(payee("Cy")));

    assertThat(count(payables::payees)).isEqualTo(payeesForOne);
    assertThat(count(() -> consignments.consignmentItems(null, false, false)))
        .isEqualTo(itemsForOne);
    assertThat(count(() -> consignments.consignmentSales(null))).isEqualTo(salesForOne);
    assertThat(inStudio(() -> consignments.consignmentSales(null)).items()).hasSize(3);
  }

  @Test
  void payoutsListAtTheSameNumberOfQueriesForOneOrMany() throws Exception {
    importPayout("po_1");
    long forOne = count(payouts::payouts);

    importPayout("po_2");
    importPayout("po_3");

    assertThat(count(payouts::payouts)).isEqualTo(forOne);
  }

  @Test
  void hibernateSeesOnlyTheBoundStudiosPostingsAndNoneWithNoStudioBound() throws Exception {
    importPayout("po_ours");
    String key = "payout:STRIPE:po_ours";
    TestUser stranger = TestUser.named("Sol");
    UUID theirStudio = UUID.fromString(read(createStudio(stranger, "Elsewhere"), "$.id"));
    TenantContext theirs = new TenantContext(accountOf(stranger), theirStudio, StudioRole.OWNER);

    assertThat(TenantContext.callAs(inStudio(), () -> findEntry(key))).isPresent();
    assertThat(TenantContext.callAs(theirs, () -> findEntry(key))).isEmpty();
    assertThat(TenantContext.callAs(TenantContext.forAccount(ownerAccountId), () -> findEntry(key)))
        .isEmpty();
  }

  private Optional<UUID> findEntry(String key) {
    return transactions.execute(status -> entries.findIdByKey(key));
  }

  private long count(Callable<?> listing) throws Exception {
    return statements.of(() -> inStudio(listing::call)).statements();
  }

  private <T> T inStudio(ScopedValue.CallableOp<T, Exception> work) throws Exception {
    return TenantContext.callAs(
        new TenantContext(ownerAccountId, studioId, StudioRole.OWNER), work);
  }

  private TenantContext inStudio() {
    return new TenantContext(ownerAccountId, studioId, StudioRole.OWNER);
  }

  private String payee(String name) throws Exception {
    return read(
        created(
            post(
                "/payees",
                "{\"name\": \"%s\", \"files1099\": false, \"consignorShare\": \"0.6\"}"
                    .formatted(name))),
        "$.id");
  }

  private String takeIn(String payeeId) throws Exception {
    return read(
        created(
            post(
                "/consignment-items",
                """
                {"payeeId": "%s", "description": "Mug", "retailPrice": "40.00", "quantity": 2}
                """
                    .formatted(payeeId))),
        "$.id");
  }

  private void sell(String itemId) {
    created(post("/consignment-items/" + itemId + "/sales", "{\"provider\": \"CASH\"}"));
  }

  private void importPayout(String externalId) {
    assertThat(
            post(
                "/payouts",
                """
                {"provider": "STRIPE", "externalId": "%s", "arrivalOn": "2026-01-05",
                 "gross": "100.00", "refunds": "0", "fees": "3.20", "adjustments": "0", "net": "96.80"}
                """
                    .formatted(externalId)))
        .hasStatusOk();
  }

  private static MvcTestResult created(MvcTestResult result) {
    assertThat(result).hasStatus(HttpStatus.CREATED);
    return result;
  }

  private MvcTestResult post(String suffix, String json) {
    return mvc.post()
        .uri("/api/v1/studios/" + studioId + suffix)
        .with(owner.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .exchange();
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
            {"name": "%s", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
            """
                .formatted(name))
        .exchange();
  }

  private static String read(MvcTestResult result, String jsonPath) throws Exception {
    return JsonPath.read(result.getResponse().getContentAsString(), jsonPath).toString();
  }
}
