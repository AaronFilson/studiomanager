package io.github.aaronfilson.studiomanager.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionOperations;

/** M4 through the API: fees with tax, payments, stored value, payouts, reconciliation, and the export. */
@IntegrationTest
class LedgerApiTests {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    TransactionOperations transactions;

    private final TestUser owner = TestUser.named("Olive");
    private final TestUser maker = TestUser.named("Mo");
    private final TestUser other = TestUser.named("Nia");
    private UUID studioId;
    private String makerId;

    @BeforeEach
    void studioWithFeesAndTax() throws Exception {
        studioId = UUID.fromString(read(post(owner, "/api/v1/studios", """
                {"name": "Ledger Clay", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
                """), "$.id"));
        for (TestUser member : List.of(maker, other)) {
            String code = read(post(owner, path("/invites"), "{\"role\": \"MEMBER\"}"), "$.code");
            assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(member.token())).hasStatusOk();
        }
        makerId = read(mvc.get().uri("/api/v1/me").with(maker.token()).exchange(), "$.account.id");
        assertThat(mvc.put().uri(path("/fee-schedule")).with(owner.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"ruleType\": \"FLAT\", \"rate\": \"10\", \"rounding\": \"NONE\", \"minimum\": \"0\"}"))
                .hasStatusOk();
        assertThat(mvc.put().uri(path("/tax-rates")).with(owner.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"taxCategory\": \"FIRING_SERVICE\", \"rate\": \"0.0825\", \"effectiveFrom\": \"2026-01-01\"}"))
                .hasStatusOk();
    }

    @Test
    void aPiecesFeeAndTaxBecomeWhatTheMakerOwesAndPaymentsAndCardsPayItDown() throws Exception {
        assertThat(post(maker, path("/pieces"), "{\"size\": {}}")).hasStatus(HttpStatus.CREATED);
        assertThat(statement(maker)).bodyJson().extractingPath("$.owed").isEqualTo("10.83");

        assertThat(post(owner, path("/payments"), """
                {"memberId": "%s", "amount": "5.00", "provider": "SQUARE", "reference": "desk"}
                """.formatted(makerId))).hasStatusOk().bodyJson().extractingPath("$.owed").isEqualTo("5.83");

        MvcTestResult card = post(owner, path("/stored-values"), """
                {"kind": "FIRING_CARD", "holder": "%s", "label": "10-firing card", "amount": "50.00", "provider": "STRIPE"}
                """.formatted(makerId));
        assertThat(card).hasStatus(HttpStatus.CREATED).bodyJson().extractingPath("$.remaining").isEqualTo("50.00");
        String cardId = read(card, "$.id");
        assertThat(post(owner, path("/stored-values/" + cardId + "/uses"), """
                {"memberId": "%s", "amount": "5.83"}
                """.formatted(makerId))).hasStatusOk().bodyJson().extractingPath("$.remaining").isEqualTo("44.17");
        assertThat(post(owner, path("/stored-values/" + cardId + "/uses"), """
                {"memberId": "%s", "amount": "100.00"}
                """.formatted(makerId))).hasStatus(HttpStatus.BAD_REQUEST);

        assertThat(statement(maker)).bodyJson()
                .hasPathSatisfying("$.owed", owed -> assertThat(owed).isEqualTo("0.00"))
                .hasPathSatisfying("$.storedValues[0].remaining", r -> assertThat(r).isEqualTo("44.17"));
        // Members see only their own statement
        assertThat(mvc.get().uri(path("/members/" + makerId + "/statement")).with(other.token()))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void payoutsClearWhatAProcessorHeldAndImportOnce() throws Exception {
        post(owner, path("/stored-values"), """
                {"kind": "CLASS_PACK", "holder": "%s", "amount": "50.00", "provider": "STRIPE"}
                """.formatted(makerId));
        assertThat(mvc.get().uri(path("/clearing")).with(owner.token())).hasStatusOk().bodyJson()
                .hasPathSatisfying("$[0].provider", p -> assertThat(p).isEqualTo("STRIPE"))
                .hasPathSatisfying("$[0].awaitingPayout", a -> assertThat(a).isEqualTo("50.00"));

        String payout = """
                {"provider": "STRIPE", "externalId": "po_test_1", "arrivalOn": "2026-10-20",
                 "gross": "50.00", "refunds": "0", "fees": "1.75", "adjustments": "0", "net": "48.25"}
                """;
        String first = read(post(owner, path("/payouts"), payout), "$.id");
        assertThat(read(post(owner, path("/payouts"), payout), "$.id")).isEqualTo(first);
        assertThat(mvc.get().uri(path("/clearing")).with(owner.token())).hasStatusOk()
                .bodyJson().extractingPath("$.length()").isEqualTo(0);

        assertThat(post(owner, path("/payouts"), """
                {"provider": "SQUARE", "externalId": "sq_1", "arrivalOn": "2026-10-20",
                 "gross": "50.00", "refunds": "0", "fees": "1.75", "adjustments": "0", "net": "50.00"}
                """)).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void giftCardsGetACodeAndUsGiftCardsCantExpireWithinFiveYears() throws Exception {
        MvcTestResult sold = post(owner, path("/stored-values"), """
                {"kind": "GIFT_CARD", "amount": "25.00", "provider": "CASH"}
                """);
        assertThat(sold).hasStatus(HttpStatus.CREATED);
        String code = read(sold, "$.code");
        assertThat(code).matches("[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}");
        assertThat(mvc.get().uri(path("/gift-cards/" + code.toLowerCase())).with(owner.token()))
                .hasStatusOk().bodyJson().extractingPath("$.remaining").isEqualTo("25.00");

        assertThat(post(owner, path("/stored-values"), """
                {"kind": "GIFT_CARD", "amount": "25.00", "provider": "CASH", "expiresOn": "%s"}
                """.formatted(LocalDate.now().plusYears(1)))).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void theDailySummaryBalancesAndExportsAsAJournal() throws Exception {
        post(maker, path("/pieces"), "{\"size\": {}}");
        post(owner, path("/payments"), "{\"memberId\": \"%s\", \"amount\": \"10.83\", \"provider\": \"CASH\"}".formatted(makerId));
        LocalDate today = LocalDate.now(java.time.ZoneId.of("America/Chicago"));

        MvcTestResult summary = mvc.get().uri(path("/ledger/summary?from={d}&to={d}"), today, today).with(owner.token()).exchange();
        assertThat(summary).hasStatusOk();
        List<String> debits = JsonPath.read(summary.getResponse().getContentAsString(), "$[*].debits");
        List<String> credits = JsonPath.read(summary.getResponse().getContentAsString(), "$[*].credits");
        assertThat(debits.stream().map(BigDecimal::new).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(credits.stream().map(BigDecimal::new).reduce(BigDecimal.ZERO, BigDecimal::add));

        MvcTestResult export = mvc.get().uri(path("/ledger/export?from={d}&to={d}"), today, today).with(owner.token()).exchange();
        assertThat(export).hasStatusOk();
        assertThat(export.getResponse().getContentType()).startsWith("text/csv");
        assertThat(export.getResponse().getContentAsString())
                .startsWith("Journal No.,Journal Date,Account Name,Debits,Credits,Description")
                .contains("Revenue: firing (FIRING_SERVICE),,10.00");

        assertThat(mvc.get().uri(path("/stored-value-movements?from={d}&to={d}"), today, today).with(owner.token()))
                .hasStatusOk();
        assertThat(mvc.get().uri(path("/ledger/export?from={d}&to={d}"), today, today).with(maker.token()))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void theDatabaseRefusesAnUnbalancedEntry() throws Exception {
        UUID ownerId = UUID.fromString(read(mvc.get().uri("/api/v1/me").with(owner.token()).exchange(), "$.account.id"));
        TenantContext inStudio = TenantContext.forAccount(ownerId).inStudio(studioId, StudioRole.OWNER);
        UUID entry = Uuids.v7();

        assertThatThrownBy(() -> TenantContext.callAs(inStudio, () -> transactions.execute(tx -> {
            jdbc.sql("""
                    insert into journal_entry (id, studio_id, occurred_on, kind, memo, idempotency_key, created_at)
                    values (:id, :studio, current_date, 'ADJUSTMENT', 'one-sided', :key, now())
                    """).param("id", entry).param("studio", studioId).param("key", "test:" + entry).update();
            return jdbc.sql("""
                    insert into journal_line (id, entry_id, studio_id, account, amount)
                    values (:id, :entry, :studio, 'BANK', 100)
                    """).param("id", Uuids.v7()).param("entry", entry).param("studio", studioId).update();
        })))
                .rootCause()
                .hasMessageContaining("does not balance");
    }

    private MvcTestResult statement(TestUser user) {
        return mvc.get().uri(path("/members/" + makerId + "/statement")).with(user.token()).exchange();
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
