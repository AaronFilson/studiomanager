package io.github.aaronfilson.studiomanager.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpServer;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.StatementCounts;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.json.JsonMapper;

/**
 * The supplies inbox end to end: upload to a real Azurite, read by a local stand-in for Anthropic's
 * Messages API answering with a recorded eval answer, matched, checked, and confirmed into stock.
 */
@IntegrationTest
class InvoiceApiTests {

  private static final String KEY = "sk-ant-api03-" + "a".repeat(40) + "Wxyz";
  private static final String REFUSED_KEY = "sk-ant-api03-" + "b".repeat(40) + "Nope";
  private static final byte[] PDF = "%PDF-1.7\n% an invoice\n".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0};
  private static final AtomicInteger messagesCalls = new AtomicInteger();
  private static final AtomicReference<String> lastRequest = new AtomicReference<>("");
  private static final HttpServer ANTHROPIC = start();

  @DynamicPropertySource
  static void anthropic(DynamicPropertyRegistry registry) {
    registry.add(
        "studiomanager.ai.anthropic-base-url",
        () -> "http://127.0.0.1:" + ANTHROPIC.getAddress().getPort());
  }

  @AfterAll
  static void stop() {
    ANTHROPIC.stop(0);
  }

  @Autowired MockMvcTester mvc;
  @Autowired StatementCounts statements;
  @Autowired InvoiceInbox inbox;
  @Autowired InvoiceSweep invoiceSweep;
  @Autowired org.springframework.jdbc.core.simple.JdbcClient jdbc;
  @Autowired org.springframework.transaction.support.TransactionTemplate transactions;

  private final HttpClient http = HttpClient.newHttpClient();
  private final TestUser owner = TestUser.named("Olive");
  private final TestUser member = TestUser.named("Mo");
  private UUID studioId;
  private String bMix;
  private String speckledBuff;

  @BeforeEach
  void aStudioWithSupplies() {
    studioId = UUID.fromString(read(studio(owner, "Clay Club"), "$.id"));
    bMix = item("B-Mix 5", "50 lb box");
    speckledBuff = item("Speckled Buff", "25 lb bag");
  }

  @Test
  void anInvoiceIsReadMatchedAndConfirmedIntoStock() throws Exception {
    setKey(KEY);
    String invoice = upload("PDF", PDF);

    MvcTestResult read = awaitRead(invoice);
    assertThat(read).bodyJson().extractingPath("$.status").isEqualTo("READY");
    assertThat(read).bodyJson().extractingPath("$.reader").isEqualTo("CLAUDE");
    assertThat(read).bodyJson().extractingPath("$.total").isEqualTo("265.03");
    assertThat(read).bodyJson().extractingPath("$.lines[0].supplyItemId").isEqualTo(bMix);
    assertThat(read).bodyJson().extractingPath("$.lines[0].matchSource").isEqualTo("NAME");
    assertThat(read).bodyJson().extractingPath("$.problems").asArray().isEmpty();
    assertThat(read).bodyJson().extractingPath("$.fileUrl").asString().contains("invoices%2F");
    // The model got the file and the instructions, and nothing about the studio's people
    assertThat(lastRequest.get()).contains("\"type\":\"document\"").doesNotContain("Olive");

    assertThat(post(owner, path("/invoices/" + invoice + "/confirm"), "{\"expectedVersion\": 0}"))
        .hasStatus(HttpStatus.CONFLICT);
    long version = version(read);
    assertThat(
            post(
                owner,
                path("/invoices/" + invoice + "/confirm"),
                "{\"expectedVersion\": %d}".formatted(version)))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo("CONFIRMED");
    // There's no single-item read; the item comes from the supplies list
    List<String> onHand =
        JsonPath.read(
            get(owner, path("/supplies")).getResponse().getContentAsString(),
            "$[?(@.id == '%s')].onHand".formatted(bMix));
    assertThat(onHand).singleElement().asString().startsWith("5");
    // Confirming also records the item's last purchase unit price and timestamp
    assertThat(supplyField(bMix, "lastPurchaseUnitPrice")).isEqualTo("32.50");
    assertThat(supplyField(bMix, "lastPurchasedAt")).isNotBlank();

    // The vendor's next invoice is matched by what staff confirmed: its SKU
    String next = upload("PDF", PDF);
    assertThat(awaitRead(next))
        .bodyJson()
        .extractingPath("$.lines[0].matchSource")
        .isEqualTo("ALIAS_SKU");
  }

  @Test
  void confirmingComputesUnitPriceFromLineTotalWhenUnitPriceIsMissing() throws Exception {
    String invoice = upload("PNG", PNG);
    long version = version(awaitRead(invoice));

    MvcTestResult saved =
        send(
            owner,
            HttpMethod.PUT,
            path("/invoices/" + invoice),
            """
            {
              "documentType": "ORDER_FORM",
              "vendorName": "Counter Clay",
              "total": "65.00",
              "expectedVersion": %d,
              "lines": [
                {
                  "description": "B-Mix 5",
                  "quantity": "2",
                  "unit": "box",
                  "lineTotal": "65.00",
                  "kind": "ITEM",
                  "supplyItemId": "%s"
                }
              ]
            }
            """
                .formatted(version, bMix));
    assertThat(saved).hasStatusOk();
    assertThat(saved).bodyJson().extractingPath("$.documentType").isEqualTo("ORDER_FORM");
    assertThat(saved).bodyJson().extractingPath("$.payableAmount").isEqualTo("65.00");

    assertThat(
            post(
                owner,
                path("/invoices/" + invoice + "/confirm"),
                "{\"expectedVersion\": %d}".formatted(version(saved))))
        .hasStatusOk();

    assertThat(supplyField(bMix, "lastPurchaseUnitPrice")).isEqualTo("32.50");
    assertThat(supplyField(bMix, "lastPurchasedAt")).isNotBlank();
  }

  @Test
  void lineOnlyEditBumpsVersionAndRejectsStaleExpectedVersion() throws Exception {
    String invoice = upload("PNG", PNG);
    long initialVersion = version(awaitRead(invoice));

    MvcTestResult firstEdit =
        send(
            owner,
            HttpMethod.PUT,
            path("/invoices/" + invoice),
            """
            {
              "vendorName": "Cascade Clay",
              "total": "50.00",
              "expectedVersion": %d,
              "lines": [
                {"description": "B-Mix 5", "quantity": "2", "unitPrice": "25.00", "lineTotal": "50.00"}
              ]
            }
            """
                .formatted(initialVersion));
    assertThat(firstEdit).hasStatusOk();
    assertThat(firstEdit).bodyJson().extractingPath("$.status").isEqualTo("READY");
    long v1 = version(firstEdit);

    // Edit ONLY the line (match supplyItemId and tweak description); every header column and status
    // stay identical so Hibernate would skip updating invoice unless forced.
    MvcTestResult lineOnlyEdit =
        send(
            owner,
            HttpMethod.PUT,
            path("/invoices/" + invoice),
            """
            {
              "vendorName": "Cascade Clay",
              "total": "50.00",
              "expectedVersion": %d,
              "lines": [
                {
                  "description": "B-Mix 5 clay",
                  "quantity": "2",
                  "unitPrice": "25.00",
                  "lineTotal": "50.00",
                  "supplyItemId": "%s"
                }
              ]
            }
            """
                .formatted(v1, bMix));
    assertThat(lineOnlyEdit).hasStatusOk();
    long v2 = version(lineOnlyEdit);
    assertThat(v2).isGreaterThan(v1);

    // Stale expectedVersion from before the line-only edit is rejected on both edit and confirm.
    assertThat(
            send(
                owner,
                HttpMethod.PUT,
                path("/invoices/" + invoice),
                """
                {
                  "vendorName": "Cascade Clay",
                  "total": "50.00",
                  "expectedVersion": %d,
                  "lines": [
                    {"description": "B-Mix 5", "quantity": "2", "unitPrice": "25.00", "lineTotal": "50.00"}
                  ]
                }
                """
                    .formatted(v1)))
        .hasStatus(HttpStatus.CONFLICT);
    assertThat(
            post(
                owner,
                path("/invoices/" + invoice + "/confirm"),
                "{\"expectedVersion\": %d}".formatted(v1)))
        .hasStatus(HttpStatus.CONFLICT);

    // Confirming with the bumped version succeeds.
    assertThat(
            post(
                owner,
                path("/invoices/" + invoice + "/confirm"),
                "{\"expectedVersion\": %d}".formatted(v2)))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo("CONFIRMED");
  }

  @Test
  void staffEditRejectsOutOfRangeValuesAndInvalidRotation() throws Exception {
    String invoice = upload("PNG", PNG);
    long v = version(awaitRead(invoice));

    // Out-of-range invoice date is rejected with 400 rather than silently cleared.
    assertThat(
            send(
                owner,
                HttpMethod.PUT,
                path("/invoices/" + invoice),
                """
                {
                  "vendorName": "Cascade Clay",
                  "invoiceDate": "1990-01-01",
                  "total": "50.00",
                  "expectedVersion": %d,
                  "lines": [{"description": "B-Mix 5", "quantity": "2", "lineTotal": "50.00"}]
                }
                """
                    .formatted(v)))
        .hasStatus(HttpStatus.BAD_REQUEST);

    // Out-of-range amount is rejected with 400.
    assertThat(
            send(
                owner,
                HttpMethod.PUT,
                path("/invoices/" + invoice),
                """
                {
                  "vendorName": "Cascade Clay",
                  "total": "99999999.00",
                  "expectedVersion": %d,
                  "lines": [{"description": "B-Mix 5", "quantity": "2", "lineTotal": "50.00"}]
                }
                """
                    .formatted(v)))
        .hasStatus(HttpStatus.BAD_REQUEST);

    // Non-quarter-turn rotation is rejected with 400.
    assertThat(
            send(
                owner,
                HttpMethod.PUT,
                path("/invoices/" + invoice),
                """
                {
                  "vendorName": "Cascade Clay",
                  "total": "50.00",
                  "rotation": 45,
                  "expectedVersion": %d,
                  "lines": [{"description": "B-Mix 5", "quantity": "2", "lineTotal": "50.00"}]
                }
                """
                    .formatted(v)))
        .hasStatus(HttpStatus.BAD_REQUEST);
  }

  @Test
  void confirmingUsesWeightedAveragePriceIgnoresPromoLinesAndPreservesNewerDate() throws Exception {
    String sampleClay = item("Sample Porcelain", "5 lb bag");

    // Newer invoice with two positive-priced lines + one $0.00 bonus line for bMix,
    // plus a 4-decimal fractional unit price for speckledBuff (8.75 / 100 = 0.0875).
    String newerInvoice = upload("PNG", PNG);
    long newerV = version(awaitRead(newerInvoice));
    MvcTestResult newerSaved =
        send(
            owner,
            HttpMethod.PUT,
            path("/invoices/" + newerInvoice),
            """
            {
              "vendorName": "Cascade Clay",
              "invoiceNumber": "INV-NEW",
              "invoiceDate": "2026-10-20",
              "total": "168.75",
              "expectedVersion": %d,
              "lines": [
                {"description": "B-Mix 5", "quantity": "4", "unitPrice": "30.00", "lineTotal": "120.00", "supplyItemId": "%s"},
                {"description": "B-Mix 5", "quantity": "1", "unitPrice": "40.00", "lineTotal": "40.00", "supplyItemId": "%s"},
                {"description": "B-Mix 5 promo", "quantity": "1", "unitPrice": "0.00", "lineTotal": "0.00", "supplyItemId": "%s"},
                {"description": "Speckled Buff", "quantity": "100", "lineTotal": "8.75", "supplyItemId": "%s"}
              ]
            }
            """
                .formatted(newerV, bMix, bMix, bMix, speckledBuff));
    assertThat(newerSaved).hasStatusOk();
    assertThat(
            post(
                owner,
                path("/invoices/" + newerInvoice + "/confirm"),
                "{\"expectedVersion\": %d}".formatted(version(newerSaved))))
        .hasStatusOk();

    // Quantity received includes promo line (4 + 1 + 1 = 6); unit price is weighted average of
    // positive lines only: (4 * 30 + 1 * 40) / 5 = 32.00.
    assertThat(onHand(bMix)).isEqualTo("6");
    assertThat(supplyField(bMix, "lastPurchaseUnitPrice")).isEqualTo("32.00");
    assertThat(supplyField(bMix, "lastPurchasedAt")).isEqualTo("2026-10-20T00:00:00Z");
    assertThat(supplyField(speckledBuff, "lastPurchaseUnitPrice")).isEqualTo("0.0875");

    // Older backfilled invoice must not clobber bMix's newer purchase price or timestamp,
    // while an all-$0.00 invoice on a new item still records 0.00.
    String olderInvoice = upload("PNG", PNG);
    long olderV = version(awaitRead(olderInvoice));
    MvcTestResult olderSaved =
        send(
            owner,
            HttpMethod.PUT,
            path("/invoices/" + olderInvoice),
            """
            {
              "vendorName": "Cascade Clay",
              "invoiceNumber": "INV-OLD",
              "invoiceDate": "2026-10-01",
              "total": "20.00",
              "expectedVersion": %d,
              "lines": [
                {"description": "B-Mix 5", "quantity": "1", "unitPrice": "20.00", "lineTotal": "20.00", "supplyItemId": "%s"},
                {"description": "Free Sample", "quantity": "2", "unitPrice": "0.00", "lineTotal": "0.00", "supplyItemId": "%s"}
              ]
            }
            """
                .formatted(olderV, bMix, sampleClay));
    assertThat(olderSaved).hasStatusOk();
    assertThat(
            post(
                owner,
                path("/invoices/" + olderInvoice + "/confirm"),
                "{\"expectedVersion\": %d}".formatted(version(olderSaved))))
        .hasStatusOk();

    assertThat(onHand(bMix)).isEqualTo("7");
    assertThat(supplyField(bMix, "lastPurchaseUnitPrice")).isEqualTo("32.00");
    assertThat(supplyField(bMix, "lastPurchasedAt")).isEqualTo("2026-10-20T00:00:00Z");
    assertThat(supplyField(sampleClay, "lastPurchaseUnitPrice")).isEqualTo("0.00");
  }

  @Test
  void v2SplitPaymentsExplainTheGapAndSetPayableAmount() throws Exception {
    String invoice = upload("PNG", PNG);
    long version = version(awaitRead(invoice));

    MvcTestResult saved =
        send(
            owner,
            HttpMethod.PUT,
            path("/invoices/" + invoice),
            """
            {
              "documentType": "ORDER_FORM",
              "vendorName": "Cascade Clay",
              "orderNumber": "CTR-514",
              "invoiceDate": "2026-10-18",
              "invoiceDatePrinted": "10/18",
              "invoiceDateYear": "FROM_PAGE",
              "subtotal": "100.00",
              "tax": "8.00",
              "total": "60.00",
              "payments": [
                {"date": "2026-10-18", "amount": "60.00", "method": "CARD", "source": "SLIP"},
                {"date": "2026-10-19", "amount": "48.00", "method": "CARD", "source": "RECEIPT"}
              ],
              "crossedOut": ["25 lb Redart"],
              "otherPapers": "Two till receipts on top",
              "notes": ["Split across two card payments"],
              "rotation": 90,
              "expectedVersion": %d,
              "lines": [
                {"description": "Fireclay", "quantity": "100", "unit": "lb", "lineTotal": "100.00", "kind": "ITEM"}
              ]
            }
            """
                .formatted(version));

    assertThat(saved).hasStatusOk();
    assertThat(saved).bodyJson().extractingPath("$.status").isEqualTo("READY");
    assertThat(saved).bodyJson().extractingPath("$.problems").asArray().isEmpty();
    assertThat(saved).bodyJson().extractingPath("$.payableAmount").isEqualTo("108.00");
    assertThat(saved).bodyJson().extractingPath("$.rotation").isEqualTo(90);
    assertThat(saved).bodyJson().extractingPath("$.crossedOut[0]").isEqualTo("25 lb Redart");
    assertThat(saved)
        .bodyJson()
        .extractingPath("$.explanations[0]")
        .asString()
        .contains("60.00", "48.00", "108.00");
  }

  @Test
  void compareReadersScoresAgainstConfirmedInvoiceAndSupportsEvalExport() throws Exception {
    setKey(KEY);
    String invoice = upload("PDF", PDF);
    long version = version(awaitRead(invoice));

    // Export is refused until the invoice is confirmed
    assertThat(get(owner, path("/invoices/" + invoice + "/eval-export")))
        .hasStatus(HttpStatus.CONFLICT);

    // Run Compare readers before confirming: stores Claude's compared draft
    MvcTestResult compared = post(owner, path("/invoices/" + invoice + "/compare-readers"), "");
    assertThat(compared).hasStatusOk();
    assertThat(compared).bodyJson().extractingPath("$.results[0].outcome").isEqualTo("READ");

    // Confirming scores the stored comparison against the confirmed invoice
    assertThat(
            post(
                owner,
                path("/invoices/" + invoice + "/confirm"),
                "{\"expectedVersion\": %d}".formatted(version)))
        .hasStatusOk();

    // Platform admin sees Claude's accuracy on confirmed invoices
    TestUser admin = TestUser.named("Ada");
    assertThat(mvc.get().uri("/api/v1/me").with(admin.adminToken())).hasStatusOk();
    MvcTestResult overview =
        mvc.get().uri("/api/v1/admin/invoice-readers").with(admin.adminToken()).exchange();
    assertThat(overview).hasStatusOk();
    String overviewJson = overview.getResponse().getContentAsString();
    List<Number> confirmedCounts =
        JsonPath.read(overviewJson, "$.accuracy[?(@.reader == 'CLAUDE')].confirmedInvoices");
    List<Number> exactCounts =
        JsonPath.read(overviewJson, "$.accuracy[?(@.reader == 'CLAUDE')].exactMatches");
    assertThat(confirmedCounts).isNotEmpty();
    assertThat(confirmedCounts.getFirst().longValue()).isGreaterThanOrEqualTo(1L);
    assertThat(exactCounts).isNotEmpty();
    assertThat(exactCounts.getFirst().longValue()).isGreaterThanOrEqualTo(1L);

    // Now eval-export succeeds with a ZIP containing expected.json, case.json and invoice.pdf
    MvcTestResult exported = get(owner, path("/invoices/" + invoice + "/eval-export"));
    assertThat(exported).hasStatusOk();
    assertThat(exported.getResponse().getContentType()).contains("application/zip");
    Map<String, byte[]> entries = new HashMap<>();
    try (ZipInputStream zip =
        new ZipInputStream(
            new ByteArrayInputStream(exported.getResponse().getContentAsByteArray()))) {
      for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
        entries.put(entry.getName(), zip.readAllBytes());
      }
    }
    assertThat(entries.keySet())
        .contains("expected.json", "case.json", "invoice.pdf", "README.txt");
    String expectedJson =
        new String(Objects.requireNonNull(entries.get("expected.json")), StandardCharsets.UTF_8);
    assertThat(expectedJson)
        .containsIgnoringCase("Riverbend Clay Supply")
        .contains("RB-20417", "265.03")
        .doesNotContain("Olive");
  }

  @Test
  void confirmingSettlesTheOrderItDeliversAndTheSameInvoiceCannotBeReceivedTwice()
      throws Exception {
    setKey(KEY);
    // Five boxes of B-Mix were ordered; the invoice is that delivery
    MvcTestResult requested =
        post(
            owner,
            path("/purchase-requests"),
            "{\"itemId\": \"%s\", \"quantity\": \"5\"}".formatted(bMix));
    String request = read(requested, "$.id");
    MvcTestResult approved = decide(request, "APPROVED", version(requested));
    MvcTestResult ordered = decide(request, "ORDERED", version(approved));
    assertThat(ordered).bodyJson().extractingPath("$.status").isEqualTo("ORDERED");

    String invoice = upload("PDF", PDF);
    long version = version(awaitRead(invoice));
    assertThat(
            post(
                owner,
                path("/invoices/" + invoice + "/confirm"),
                "{\"expectedVersion\": %d}".formatted(version)))
        .hasStatusOk();

    // Received once, by the invoice; the request is closed (the list is the open ones) without
    // receiving it again, and nothing is on order any more
    assertThat(onHand(bMix)).startsWith("5");
    assertThat(new BigDecimal(supplyField(bMix, "onOrder"))).isZero();
    List<String> open =
        JsonPath.read(
            get(owner, path("/purchase-requests")).getResponse().getContentAsString(),
            "$[?(@.id == '%s')].status".formatted(request));
    assertThat(open).isEmpty();

    // The same invoice again: the review says so, and confirming it is refused
    String again = upload("PDF", PDF);
    MvcTestResult repeat = awaitRead(again);
    assertThat(repeat)
        .bodyJson()
        .extractingPath("$.problems")
        .asArray()
        .anySatisfy(problem ->
            assertThat(problem)
                .asString()
                .containsIgnoringCase(
                    "Invoice RB-20417 from Riverbend Clay Supply is already confirmed"));
    assertThat(
            post(
                owner,
                path("/invoices/" + again + "/confirm"),
                "{\"expectedVersion\": %d}".formatted(version(repeat))))
        .hasStatus(HttpStatus.CONFLICT);
    assertThat(onHand(bMix)).startsWith("5");
  }

  private MvcTestResult decide(String request, String to, long version) {
    return send(
        owner,
        HttpMethod.PUT,
        path("/purchase-requests/" + request + "/status"),
        "{\"to\": \"%s\", \"expectedVersion\": %d}".formatted(to, version));
  }

  private String onHand(String item) throws Exception {
    return supplyField(item, "onHand");
  }

  /** There's no single-item read; the item comes from the supplies list. */
  private String supplyField(String item, String field) throws Exception {
    List<Object> values =
        JsonPath.read(
            get(owner, path("/supplies")).getResponse().getContentAsString(),
            "$[?(@.id == '%s')].%s".formatted(item, field));
    return String.valueOf(values.getFirst());
  }

  @Test
  void staffCorrectTheDraftAndAMismatchNeedsALook() throws Exception {
    setKey(KEY);
    String invoice = upload("PDF", PDF);
    long version = version(awaitRead(invoice));

    MvcTestResult saved =
        send(
            owner,
            HttpMethod.PUT,
            path("/invoices/" + invoice),
            """
            {"vendorName": "Riverbend", "total": "999.00", "expectedVersion": %d,
             "lines": [{"description": "Pallet", "quantity": "1", "unitPrice": "10", "lineTotal": "10"}]}
            """
                .formatted(version));

    assertThat(saved).hasStatusOk();
    assertThat(saved).bodyJson().extractingPath("$.status").isEqualTo("NEEDS_LOOK");
    assertThat(saved)
        .bodyJson()
        .extractingPath("$.problems[0]")
        .isEqualTo(
            "The line adds up to 10.00. The total is 999.00, 989.00 more: check for tax, shipping"
                + " or a fee that wasn't read.");
    assertThat(saved).bodyJson().extractingPath("$.lines[0].supplyItemId").isNull();
  }

  @Test
  void withoutAKeyTheInvoiceIsTypedInByHand() throws Exception {
    int before = messagesCalls.get();
    String invoice = upload("PNG", PNG);

    MvcTestResult read = awaitRead(invoice);
    assertThat(read).bodyJson().extractingPath("$.status").isEqualTo("NEEDS_LOOK");
    assertThat(read).bodyJson().extractingPath("$.readNote").asString().contains("No reader");
    assertThat(messagesCalls.get()).isEqualTo(before);
  }

  @Test
  void compareReadersListsEveryReaderAndSaysWhyOneDidNotRun() throws Exception {
    String invoice = upload("PNG", PNG);
    awaitRead(invoice);

    // No key, and the platform's readers aren't set up here: nobody runs, and each says why
    MvcTestResult compared = post(owner, path("/invoices/" + invoice + "/compare-readers"), "");

    assertThat(compared).hasStatusOk();
    assertThat(compared).bodyJson().extractingPath("$.results.length()").isEqualTo(3);
    assertThat(compared).bodyJson().extractingPath("$.results[0].reader").isEqualTo("CLAUDE");
    assertThat(compared).bodyJson().extractingPath("$.results[0].outcome").isEqualTo("SKIPPED");
    assertThat(compared).bodyJson().extractingPath("$.results[0].skipReason").isEqualTo("NO_KEY");
    assertThat(compared)
        .bodyJson()
        .extractingPath("$.results[1].skipReason")
        .isEqualTo("NOT_SET_UP");
    assertThat(compared)
        .bodyJson()
        .extractingPath("$.results[2].note")
        .asString()
        .contains("isn't set up");

    // With a key, Claude reads and the others still say why they didn't
    setKey(KEY);
    MvcTestResult again = post(owner, path("/invoices/" + invoice + "/compare-readers"), "");
    assertThat(again).bodyJson().extractingPath("$.results[0].outcome").isEqualTo("READ");
    assertThat(again).bodyJson().extractingPath("$.results[0].total").isEqualTo("265.03");
    assertThat(again).bodyJson().extractingPath("$.results[1].outcome").isEqualTo("SKIPPED");
  }

  @Test
  void consentToThePlatformsReadersIsItsOwnSwitchAndSurvivesANewKey() {
    // No key yet: nothing to save the consent with
    assertThat(
            send(
                owner,
                HttpMethod.PUT,
                path("/ai-key/platform-readers"),
                "{\"allowPlatformReaders\": true}"))
        .hasStatus(HttpStatus.NOT_FOUND);

    setKey(KEY);
    assertThat(
            send(
                owner,
                HttpMethod.PUT,
                path("/ai-key/platform-readers"),
                "{\"allowPlatformReaders\": true}"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.allowPlatformReaders")
        .isEqualTo(true);

    // Replacing the key without saying keeps the consent
    setKey(KEY);
    assertThat(get(owner, path("/ai-key")))
        .bodyJson()
        .extractingPath("$.allowPlatformReaders")
        .isEqualTo(true);
  }

  @Test
  void aKeyAnthropicRefusesIsMarkedAndTheInvoiceIsTypedIn() throws Exception {
    setKey(REFUSED_KEY);
    String invoice = upload("PDF", PDF);

    assertThat(awaitRead(invoice))
        .bodyJson()
        .extractingPath("$.readNote")
        .asString()
        .contains("refused the key");
    assertThat(get(owner, path("/ai-key")))
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo("REJECTED");
  }

  @Test
  void aFileThatIsNotWhatItSaidIsRefused() throws Exception {
    MvcTestResult started = post(owner, path("/invoices"), "{\"fileType\": \"PDF\"}");
    String invoice = read(started, "$.invoiceId");
    assertThat(post(owner, path("/invoices/" + invoice + "/complete"), ""))
        .hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(put(read(started, "$.uploadUrl"), PNG)).isEqualTo(201);
    assertThat(post(owner, path("/invoices/" + invoice + "/complete"), ""))
        .hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(send(owner, HttpMethod.DELETE, path("/invoices/" + invoice), ""))
        .hasStatus(HttpStatus.NO_CONTENT);
  }

  @Test
  void membersAndTokensCannotReachTheInbox() {
    String code = read(post(owner, path("/invites"), "{\"role\": \"MEMBER\"}"), "$.code");
    assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(member.token()))
        .hasStatusOk();
    assertThat(get(member, path("/invoices"))).hasStatus(HttpStatus.FORBIDDEN);

    String token =
        read(
            post(owner, path("/api-tokens"), "{\"name\": \"t\", \"scopes\": [\"read\"]}"),
            "$.secret");
    assertThat(mvc.get().uri(path("/invoices")).header("Authorization", "Bearer " + token))
        .hasStatus(HttpStatus.FORBIDDEN);
  }

  @Test
  void theInboxIsTheSameNumberOfQueriesForOneInvoiceOrMany() throws Exception {
    post(owner, path("/invoices"), "{\"fileType\": \"PDF\"}");
    long forOne = countListing();

    post(owner, path("/invoices"), "{\"fileType\": \"JPEG\"}");
    post(owner, path("/invoices"), "{\"fileType\": \"PNG\"}");

    assertThat(countListing()).isEqualTo(forOne);
  }

  @Test
  void pdfOverTenPagesIsRefusedAtComplete() throws Exception {
    byte[] twelvePagePdf =
        "%PDF-1.7\n1 0 obj << /Type /Pages /Count 12 >> endobj\n"
            .getBytes(StandardCharsets.ISO_8859_1);
    MvcTestResult started = post(owner, path("/invoices"), "{\"fileType\": \"PDF\"}");
    String invoice = read(started, "$.invoiceId");
    assertThat(put(read(started, "$.uploadUrl"), twelvePagePdf)).isEqualTo(201);
    assertThat(post(owner, path("/invoices/" + invoice + "/complete"), ""))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .extractingPath("$.detail")
        .asString()
        .contains("12 pages", "10 pages");
  }

  @Test
  void confirmingWithRecordPayablePostsBalancedSupplierBillInLedger() throws Exception {
    setKey(KEY);
    String invoice = upload("PDF", PDF);
    MvcTestResult draft = awaitRead(invoice);
    long version = version(draft);

    MvcTestResult confirmed =
        post(
            owner,
            path("/invoices/" + invoice + "/confirm"),
            "{\"expectedVersion\": %d, \"recordPayable\": true}".formatted(version));
    assertThat(confirmed).hasStatusOk();
    assertThat(confirmed).bodyJson().extractingPath("$.status").isEqualTo("CONFIRMED");
    assertThat(confirmed).bodyJson().extractingPath("$.ledgerEntryId").isNotNull();

    // Daily summary includes balanced SUPPLIES_EXPENSE debit and ACCOUNTS_PAYABLE credit for 265.03
    String billDate = read(confirmed, "$.invoiceDate");
    MvcTestResult summary =
        get(owner, path("/ledger/summary?from=%s&to=%s".formatted(billDate, billDate)));
    assertThat(summary).hasStatusOk();
    String summaryBody = summary.getResponse().getContentAsString();
    assertThat(
            JsonPath.<List<String>>read(
                summaryBody, "$[?(@.account == 'SUPPLIES_EXPENSE')].debits"))
        .contains("265.03");
    assertThat(
            JsonPath.<List<String>>read(
                summaryBody, "$[?(@.account == 'ACCOUNTS_PAYABLE')].credits"))
        .contains("265.03");
  }

  @Test
  void sweepPurgesOldRawAnswersAndRemovesAbandonedUploads() throws Exception {
    setKey(KEY);
    String readInvoice = upload("PDF", PDF);
    awaitRead(readInvoice);
    // Start an abandoned upload that is never completed
    MvcTestResult abandonedStart = post(owner, path("/invoices"), "{\"fileType\": \"PNG\"}");
    String abandonedId = read(abandonedStart, "$.invoiceId");
    assertThat(put(read(abandonedStart, "$.uploadUrl"), PNG)).isEqualTo(201);

    // Running now leaves fresh rows alone
    InvoiceSweep.Result fresh = invoiceSweep.runAt(java.time.Instant.now());
    assertThat(fresh.rawAnswersPurged()).isZero();
    assertThat(fresh.abandonedUploadsRemoved()).isZero();

    // Running 91 days in the future purges raw JSON and deletes the abandoned upload
    InvoiceSweep.Result after91Days =
        invoiceSweep.runAt(java.time.Instant.now().plus(Duration.ofDays(91)));
    assertThat(after91Days.rawAnswersPurged()).isGreaterThanOrEqualTo(1);
    assertThat(after91Days.abandonedUploadsRemoved()).isGreaterThanOrEqualTo(1);

    // The read invoice itself stays readable, while the abandoned upload is gone
    assertThat(get(owner, path("/invoices/" + readInvoice))).hasStatusOk();
    assertThat(get(owner, path("/invoices/" + abandonedId))).hasStatus(HttpStatus.NOT_FOUND);

    UUID ownerId =
        UUID.fromString(
            read(mvc.get().uri("/api/v1/me").with(owner.token()).exchange(), "$.account.id"));
    boolean rawCleared =
        TenantContext.callAs(
            new TenantContext(ownerId, studioId, StudioRole.OWNER),
            () ->
                transactions.execute(tx ->
                    Boolean.TRUE.equals(
                        jdbc.sql("select raw is null from invoice where id = :id")
                            .param("id", UUID.fromString(readInvoice))
                            .query(Boolean.class)
                            .single())));
    assertThat(rawCleared).isTrue();
  }

  private long countListing() throws Exception {
    UUID ownerId =
        UUID.fromString(
            read(mvc.get().uri("/api/v1/me").with(owner.token()).exchange(), "$.account.id"));
    TenantContext bound = new TenantContext(ownerId, studioId, StudioRole.OWNER);
    return statements
        .of(() -> TenantContext.callAs(bound, () -> inbox.list(studioId)))
        .statements();
  }

  private String upload(String fileType, byte[] content) throws Exception {
    MvcTestResult started =
        post(owner, path("/invoices"), "{\"fileType\": \"%s\"}".formatted(fileType));
    assertThat(started).hasStatus(HttpStatus.CREATED);
    assertThat(put(read(started, "$.uploadUrl"), content)).isEqualTo(201);
    String invoice = read(started, "$.invoiceId");
    MvcTestResult completed = post(owner, path("/invoices/" + invoice + "/complete"), "");
    assertThat(completed).hasStatus(HttpStatus.ACCEPTED);
    return invoice;
  }

  private MvcTestResult awaitRead(String invoice) {
    AtomicReference<MvcTestResult> latest = new AtomicReference<>();
    await()
        .atMost(Duration.ofSeconds(30))
        .until(() -> {
          latest.set(get(owner, path("/invoices/" + invoice)));
          return !List.of("UPLOADING", "READING").contains(read(latest.get(), "$.status"));
        });
    return latest.get();
  }

  private void setKey(String key) {
    assertThat(
            send(
                owner,
                HttpMethod.PUT,
                path("/ai-key"),
                "{\"provider\": \"ANTHROPIC\", \"apiKey\": \"%s\"}".formatted(key)))
        .hasStatusOk();
  }

  private String item(String name, String unit) {
    return read(
        post(
            owner,
            path("/supplies"),
            "{\"name\": \"%s\", \"category\": \"CLAY\", \"unit\": \"%s\"}".formatted(name, unit)),
        "$.id");
  }

  private int put(String url, byte[] body) throws Exception {
    return http.send(
            HttpRequest.newBuilder(URI.create(url))
                .header("x-ms-blob-type", "BlockBlob")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(body))
                .build(),
            HttpResponse.BodyHandlers.discarding())
        .statusCode();
  }

  /** Answers every read with the recorded eval answer for the clay order; refuses one key. */
  private static HttpServer start() {
    try {
      String answer =
          Files.readString(
              InvoiceEvalTests.CASES.resolve("synthetic-clay-order").resolve("claude.json"));
      JsonMapper json = JsonMapper.builder().build();
      byte[] message =
          json.writeValueAsBytes(
              Map.of(
                  "id", "msg_test",
                  "type", "message",
                  "role", "assistant",
                  "model", "claude-opus-5-5",
                  "content", List.of(Map.of("type", "text", "text", answer)),
                  "stop_reason", "end_turn",
                  "usage", Map.of("input_tokens", 1200, "output_tokens", 300)));
      byte[] refused =
          """
          {"type": "error", "error": {"type": "authentication_error", "message": "invalid x-api-key"}}
          """
              .getBytes(StandardCharsets.UTF_8);
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/v1/messages",
          exchange -> {
            messagesCalls.incrementAndGet();
            lastRequest.set(
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String key = exchange.getRequestHeaders().getFirst("x-api-key");
            String bearer = exchange.getRequestHeaders().getFirst("Authorization");
            boolean good =
                (key != null && key.endsWith("Wxyz"))
                    || (bearer != null && bearer.endsWith("Wxyz"));
            byte[] body = good ? message : refused;
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(good ? 200 : 401, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
          });
      server.start();
      return server;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private MvcTestResult studio(TestUser user, String name) {
    return post(
        user,
        "/api/v1/studios",
        """
        {"name": "%s", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
        """
            .formatted(name));
  }

  private MvcTestResult get(TestUser user, String uri) {
    return mvc.get().uri(uri).with(user.token()).exchange();
  }

  private MvcTestResult post(TestUser user, String uri, String body) {
    return send(user, HttpMethod.POST, uri, body);
  }

  private MvcTestResult send(TestUser user, HttpMethod method, String uri, String body) {
    return mvc.method(method)
        .uri(uri)
        .with(user.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .exchange();
  }

  private String path(String suffix) {
    return "/api/v1/studios/" + studioId + suffix;
  }

  private static long version(MvcTestResult result) {
    try {
      Number version = JsonPath.read(result.getResponse().getContentAsString(), "$.version");
      return version.longValue();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String read(MvcTestResult result, String jsonPath) {
    try {
      Object value = JsonPath.read(result.getResponse().getContentAsString(), jsonPath);
      return String.valueOf(value);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
