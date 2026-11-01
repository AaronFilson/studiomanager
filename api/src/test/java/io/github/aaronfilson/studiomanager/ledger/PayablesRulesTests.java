package io.github.aaronfilson.studiomanager.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.ledger.PayablesModel.ConsignmentItem;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.DiscountsFrom;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.NecLine;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.NecReport;
import org.junit.jupiter.api.Test;

/** The arithmetic behind payables, without a database. */
class PayablesRulesTests {

    @Test
    void aSharedDiscountComesOffBothSharesAndAStudioDiscountOnlyOffTheCommission() {
        BigDecimal retail = new BigDecimal("40.00");
        BigDecimal discounted = new BigDecimal("30.00");

        // 60% to the artist of what was paid
        assertThat(PayablesService.consignorAmount(item("0.6", DiscountsFrom.SHARED), retail, discounted, 2))
                .isEqualByComparingTo("18.00");
        // The Clay Studio's terms: the artist gets their share of the retail price regardless
        assertThat(PayablesService.consignorAmount(item("0.5", DiscountsFrom.STUDIO), retail, discounted, 2))
                .isEqualByComparingTo("20.00");
        // ...but never more than the buyer paid
        assertThat(PayablesService.consignorAmount(item("0.6", DiscountsFrom.STUDIO), retail, new BigDecimal("10.00"), 2))
                .isEqualByComparingTo("10.00");
    }

    @Test
    void sharesRoundHalfUpToTheCurrency() {
        assertThat(PayablesService.consignorAmount(item("0.3333", DiscountsFrom.SHARED), new BigDecimal("10.00"),
                new BigDecimal("10.00"), 2)).isEqualByComparingTo("3.33");
        assertThat(PayablesService.consignorAmount(item("0.6", DiscountsFrom.SHARED), new BigDecimal("1000"),
                new BigDecimal("999"), 0)).isEqualByComparingTo("599");
    }

    @Test
    void theNecThresholdRoseToTwoThousandDollarsFor2026() {
        assertThat(PayablesService.necThreshold(2025)).isEqualByComparingTo("600");
        assertThat(PayablesService.necThreshold(2026)).isEqualByComparingTo("2000");
    }

    @Test
    void theNecCsvFlagsMissingW9sAndGuardsNames() {
        String csv = PayablesController.necCsv(new NecReport(2026, new BigDecimal("2000"), List.of(
                new NecLine(UUID.randomUUID(), "=Ana", null, null, new BigDecimal("2400.00"), new BigDecimal("0.00"), true),
                new NecLine(UUID.randomUUID(), "Ben, Guild", "ben@example.test", LocalDate.of(2026, 2, 1),
                        new BigDecimal("500.00"), new BigDecimal("340.00"), false))));

        assertThat(csv.lines().toList()).containsExactly(
                "Payee,Email,W-9 received,Box 1 nonemployee compensation,"
                        + "Paid through card or payment networks (on a 1099-K; not included),At or over threshold",
                "'=Ana,,MISSING,2400.00,0.00,Yes",
                "\"Ben, Guild\",ben@example.test,2026-02-01,500.00,340.00,No");
    }

    private static ConsignmentItem item(String share, DiscountsFrom discountsFrom) {
        return new ConsignmentItem(UUID.randomUUID(), UUID.randomUUID(), "Ana", "Mug", new BigDecimal("40.00"),
                new BigDecimal(share), discountsFrom, 3, 0, 0, LocalDate.of(2026, 10, 1),
                null, false);
    }

}
