package io.github.aaronfilson.studiomanager.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.TestUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Payables through the API: consignment, instructors' shares, payments, statements and the 1099-NEC list. */
@IntegrationTest
class PayablesApiTests {

    private static final ZoneId CHICAGO = ZoneId.of("America/Chicago");

    @Autowired
    MockMvcTester mvc;

    private final TestUser owner = TestUser.named("Olive");
    private final TestUser maker = TestUser.named("Mo");
    private UUID studioId;
    private String makerId;

    @BeforeEach
    void studioWithRetailTax() throws Exception {
        studioId = UUID.fromString(read(post(owner, "/api/v1/studios", """
                {"name": "Payables Clay", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
                """), "$.id"));
        String code = read(post(owner, path("/invites"), "{\"role\": \"MEMBER\"}"), "$.code");
        assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(maker.token())).hasStatusOk();
        makerId = read(mvc.get().uri("/api/v1/me").with(maker.token()).exchange(), "$.account.id");
        assertThat(mvc.put().uri(path("/tax-rates")).with(owner.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"taxCategory\": \"RETAIL\", \"rate\": \"0.08\", \"effectiveFrom\": \"2026-01-01\"}"))
                .hasStatusOk();
    }

    @Test
    void consignedWorkSoldBecomesWhatTheArtistIsOwedAndARefundTakesItBack() throws Exception {
        String ana = payee("{\"name\": \"Ana Ruiz\", \"files1099\": false, \"consignorShare\": \"0.6\"}");
        MvcTestResult takenIn = post(owner, path("/consignment-items"), """
                {"payeeId": "%s", "description": "Celadon mug", "retailPrice": "40.00", "quantity": 3}
                """.formatted(ana));
        assertThat(takenIn).hasStatus(HttpStatus.CREATED).bodyJson()
                .hasPathSatisfying("$.consignorShare", s -> assertThat(s).isEqualTo("0.6"))
                .hasPathSatisfying("$.inStock", s -> assertThat(s).isEqualTo(3));
        String mug = read(takenIn, "$.id");

        // At full price: 60% of $40 to Ana, $16 commission, and 8% tax on top
        MvcTestResult full = post(owner, path("/consignment-items/" + mug + "/sales"), "{\"provider\": \"SQUARE\"}");
        assertThat(full).hasStatus(HttpStatus.CREATED).bodyJson()
                .hasPathSatisfying("$.description", d -> assertThat(d).isEqualTo("Celadon mug"))
                .hasPathSatisfying("$.consignorAmount", a -> assertThat(a).isEqualTo("24.00"))
                .hasPathSatisfying("$.tax", t -> assertThat(t).isEqualTo("3.20"));
        // At a discount, split by the shares
        assertThat(post(owner, path("/consignment-items/" + mug + "/sales"), """
                {"price": "30.00", "provider": "CASH"}
                """)).hasStatus(HttpStatus.CREATED).bodyJson().extractingPath("$.consignorAmount").isEqualTo("18.00");
        assertThat(owed(ana)).isEqualTo("42.00");
        assertThat(post(owner, path("/consignment-items/" + mug + "/sales"), """
                {"quantity": 2, "provider": "CASH"}
                """)).hasStatus(HttpStatus.BAD_REQUEST);

        // A refund reverses her share and puts the mug back on the shelf, once
        String sale = read(full, "$.id");
        assertThat(post(owner, path("/consignment-sales/" + sale + "/refund"), "")).hasStatusOk().bodyJson()
                .extractingPath("$.refundedOn").isNotNull();
        assertThat(post(owner, path("/consignment-sales/" + sale + "/refund"), "")).hasStatus(HttpStatus.CONFLICT);
        assertThat(owed(ana)).isEqualTo("18.00");
        assertThat(mvc.get().uri(path("/consignment-items")).with(owner.token())).hasStatusOk().bodyJson()
                .extractingPath("$[0].inStock").isEqualTo(2);

        // The rest goes back to her, and is off the shelf
        assertThat(post(owner, path("/consignment-items/" + mug + "/returns"), "{\"quantity\": 2}")).hasStatusOk();
        assertThat(mvc.get().uri(path("/consignment-items")).with(owner.token())).hasStatusOk().bodyJson()
                .extractingPath("$.length()").isEqualTo(0);

        // The commission is revenue in the books; Ana's share isn't
        LocalDate today = LocalDate.now(CHICAGO);
        MvcTestResult export = mvc.get().uri(path("/ledger/export?from={d}&to={d}"), today, today).with(owner.token()).exchange();
        assertThat(export.getResponse().getContentAsString())
                .contains("Revenue: consignment commissions (RETAIL),,12.00")
                .contains("Owed to artists and instructors,,18.00");
    }

    @Test
    void whenTheStudioBearsDiscountsTheArtistStillGetsTheirShareOfRetail() throws Exception {
        String ben = payee("{\"name\": \"Ben\", \"files1099\": false, \"consignorShare\": \"0.5\", \"discountsFrom\": \"STUDIO\"}");
        String bowl = read(post(owner, path("/consignment-items"), """
                {"payeeId": "%s", "description": "Bowl", "retailPrice": "60.00"}
                """.formatted(ben)), "$.id");

        assertThat(post(owner, path("/consignment-items/" + bowl + "/sales"), """
                {"price": "45.00", "provider": "STRIPE"}
                """)).hasStatus(HttpStatus.CREATED).bodyJson().extractingPath("$.consignorAmount").isEqualTo("30.00");
    }

    @Test
    void anInstructorsShareIsPaidOnceAndOnlyNonNetworkPaymentsCountTowardsThe1099() throws Exception {
        String cy = payee("{\"name\": \"Cy Instructor\", \"email\": \"cy@example.test\", \"files1099\": true}");

        MvcTestResult share = post(owner, path("/payees/" + cy + "/earnings"), """
                {"kind": "CLASS_SHARE", "description": "Wheel 101, spring session", "gross": "1200.00", "share": "0.7"}
                """);
        assertThat(share).hasStatus(HttpStatus.CREATED).bodyJson().extractingPath("$.amount").isEqualTo("840.00");
        assertThat(post(owner, path("/payees/" + cy + "/earnings"), """
                {"kind": "SERVICE", "description": "Wrong", "amount": "-5.00"}
                """)).hasStatus(HttpStatus.BAD_REQUEST);

        assertThat(post(owner, path("/payees/" + cy + "/payments"), """
                {"amount": "500.00", "method": "CHECK", "reference": "1043"}
                """)).hasStatus(HttpStatus.CREATED);
        // No paying more than is owed
        assertThat(post(owner, path("/payees/" + cy + "/payments"), """
                {"amount": "400.00", "method": "BANK_TRANSFER"}
                """)).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(post(owner, path("/payees/" + cy + "/payments"), """
                {"amount": "340.00", "method": "PAYPAL"}
                """)).hasStatus(HttpStatus.CREATED);
        assertThat(owed(cy)).isEqualTo("0.00");

        LocalDate today = LocalDate.now(CHICAGO);
        MvcTestResult statement = mvc.get().uri(path("/payees/" + cy + "/statement?from={f}&to={t}"),
                today.withDayOfMonth(1), today).with(owner.token()).exchange();
        assertThat(statement).hasStatusOk().bodyJson()
                .hasPathSatisfying("$.opening", o -> assertThat(o).isEqualTo("0"))
                .hasPathSatisfying("$.lines.length()", n -> assertThat(n).isEqualTo(3))
                .hasPathSatisfying("$.lines[0].amount", a -> assertThat(a).isEqualTo("840.00"))
                .hasPathSatisfying("$.lines[1].memo", m -> assertThat(m).isEqualTo("Paid Cy Instructor, cheque 1043"))
                .hasPathSatisfying("$.closing", c -> assertThat(c).isEqualTo("0.00"));

        MvcTestResult report = mvc.get().uri(path("/payables/1099-nec?year={y}"), today.getYear()).with(owner.token()).exchange();
        assertThat(report).hasStatusOk().bodyJson()
                .hasPathSatisfying("$.payees[0].reportable", r -> assertThat(r).isEqualTo("500.00"))
                .hasPathSatisfying("$.payees[0].paidThroughNetworks", r -> assertThat(r).isEqualTo("340.00"))
                .hasPathSatisfying("$.payees[0].meetsThreshold", m -> assertThat(m).isEqualTo(false));
        MvcTestResult csv = mvc.get().uri(path("/payables/1099-nec/export?year={y}"), today.getYear()).with(owner.token()).exchange();
        assertThat(csv).hasStatusOk();
        assertThat(csv.getResponse().getContentAsString()).contains("Cy Instructor,cy@example.test,MISSING,500.00,340.00,No");
    }

    @Test
    void aMemberArtistCanBePaidInStudioCreditAgainstWhatTheyOwe() throws Exception {
        assertThat(mvc.put().uri(path("/fee-schedule")).with(owner.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"ruleType\": \"FLAT\", \"rate\": \"10\", \"rounding\": \"NONE\", \"minimum\": \"0\"}"))
                .hasStatusOk();
        assertThat(post(maker, path("/pieces"), "{\"size\": {}}")).hasStatus(HttpStatus.CREATED);
        String mo = payee("{\"name\": \"Mo\", \"accountId\": \"%s\", \"files1099\": false, \"consignorShare\": \"0.6\"}"
                .formatted(makerId));
        assertThat(post(owner, path("/payees"), "{\"name\": \"Mo again\", \"accountId\": \"%s\", \"files1099\": false}"
                .formatted(makerId))).hasStatus(HttpStatus.CONFLICT);
        String cup = read(post(owner, path("/consignment-items"), """
                {"payeeId": "%s", "description": "Cup", "retailPrice": "25.00"}
                """.formatted(mo)), "$.id");
        post(owner, path("/consignment-items/" + cup + "/sales"), "{\"provider\": \"CASH\"}");

        assertThat(post(owner, path("/payees/" + mo + "/payments"), """
                {"amount": "10.00", "method": "STUDIO_CREDIT"}
                """)).hasStatus(HttpStatus.CREATED);
        assertThat(mvc.get().uri(path("/members/" + makerId + "/statement")).with(maker.token())).hasStatusOk()
                .bodyJson().extractingPath("$.owed").isEqualTo("0.00");
        assertThat(owed(mo)).isEqualTo("5.00");

        String outsider = payee("{\"name\": \"Outside artist\", \"files1099\": false, \"consignorShare\": \"0.6\"}");
        String plate = read(post(owner, path("/consignment-items"), """
                {"payeeId": "%s", "description": "Plate", "retailPrice": "25.00"}
                """.formatted(outsider)), "$.id");
        post(owner, path("/consignment-items/" + plate + "/sales"), "{\"provider\": \"CASH\"}");
        assertThat(post(owner, path("/payees/" + outsider + "/payments"), """
                {"amount": "5.00", "method": "STUDIO_CREDIT"}
                """)).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void anArchivedPayeeTakesNothingNewAndMembersSeeNoneOfIt() throws Exception {
        String dee = payee("{\"name\": \"Dee\", \"files1099\": false, \"consignorShare\": \"0.6\"}");
        assertThat(mvc.put().uri(path("/payees/" + dee)).with(owner.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Dee\", \"files1099\": false, \"consignorShare\": \"0.6\", \"archived\": true}"))
                .hasStatusOk().bodyJson().extractingPath("$.archived").isEqualTo(true);
        assertThat(post(owner, path("/consignment-items"), """
                {"payeeId": "%s", "description": "Vase", "retailPrice": "80.00"}
                """.formatted(dee))).hasStatus(HttpStatus.BAD_REQUEST);

        for (String uri : List.of("/payees", "/consignment-items", "/consignment-sales")) {
            assertThat(mvc.get().uri(path(uri)).with(maker.token())).hasStatus(HttpStatus.FORBIDDEN);
        }
        assertThat(post(maker, path("/payees"), "{\"name\": \"Me\", \"files1099\": false}")).hasStatus(HttpStatus.FORBIDDEN);
    }

    private String payee(String json) throws Exception {
        MvcTestResult created = post(owner, path("/payees"), json);
        assertThat(created).hasStatus(HttpStatus.CREATED);
        return read(created, "$.id");
    }

    private String owed(String payeeId) throws Exception {
        MvcTestResult payees = mvc.get().uri(path("/payees")).with(owner.token()).exchange();
        List<String> owed = JsonPath.read(payees.getResponse().getContentAsString(), "$[?(@.id == '%s')].owed".formatted(payeeId));
        return owed.getFirst();
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
