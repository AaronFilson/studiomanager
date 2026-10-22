package io.github.aaronfilson.studiomanager.ledger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.EntryKind;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Provider;

import org.jspecify.annotations.Nullable;

/** Payables' types: payees, consigned work, shares earned and payments sent. */
public final class PayablesModel {

    private PayablesModel() {
    }

    /** Who bears a discount on consigned work: split by the shares, or all from the studio's commission. */
    public enum DiscountsFrom {
        SHARED,
        STUDIO
    }

    public enum EarningKind {
        CLASS_SHARE,
        SERVICE,
        ADJUSTMENT
    }

    /** How a payee was paid. Card and payment-network payments go on a 1099-K, not a 1099-NEC. */
    public enum PaymentMethod {
        CHECK(true),
        BANK_TRANSFER(true),
        CASH(true),
        PAYPAL(false),
        STRIPE(false),
        STUDIO_CREDIT(true);

        private final boolean countsTowardsNec;

        PaymentMethod(boolean countsTowardsNec) {
            this.countsTowardsNec = countsTowardsNec;
        }

        /** Whether the studio reports it on a 1099-NEC, rather than the network on a 1099-K. */
        public boolean countsTowardsNec() {
            return countsTowardsNec;
        }
    }

    /** {@code owed} is what the studio owes them now; negative if they owe the studio. */
    public record Payee(UUID id, String name, @Nullable String email, @Nullable UUID accountId,
            @Nullable String accountName, boolean files1099, @Nullable LocalDate w9ReceivedOn,
            @Nullable BigDecimal consignorShare, DiscountsFrom discountsFrom, boolean archived, BigDecimal owed) {
    }

    public record PayeeChanges(String name, @Nullable String email, @Nullable UUID accountId, boolean files1099,
            @Nullable LocalDate w9ReceivedOn, @Nullable BigDecimal consignorShare, DiscountsFrom discountsFrom,
            boolean archived) {
    }

    public record ConsignmentItem(UUID id, UUID payeeId, String payeeName, String description, BigDecimal retailPrice,
            BigDecimal consignorShare, DiscountsFrom discountsFrom, int quantity, int sold, int returned,
            LocalDate receivedOn) {

        public int inStock() {
            return quantity - sold - returned;
        }
    }

    public record ConsignmentSale(UUID id, UUID itemId, String description, int quantity, BigDecimal price,
            BigDecimal consignorAmount, BigDecimal tax, Provider provider, LocalDate soldOn, @Nullable LocalDate refundedOn) {
    }

    public record Earning(UUID id, UUID payeeId, EarningKind kind, String description, LocalDate earnedOn,
            @Nullable BigDecimal gross, @Nullable BigDecimal share, BigDecimal amount) {
    }

    public record Payment(UUID id, UUID payeeId, LocalDate paidOn, BigDecimal amount, PaymentMethod method,
            @Nullable String reference) {
    }

    /** One line of a payee's statement: positive adds to what they're owed, negative pays it down. */
    public record StatementLine(LocalDate date, EntryKind kind, String memo, BigDecimal amount) {
    }

    public record PayeeStatement(UUID payeeId, String payeeName, LocalDate from, LocalDate to, BigDecimal opening,
            List<StatementLine> lines, BigDecimal closing) {
    }

    /** One payee's payments in a calendar year, split by whether a 1099-NEC covers them. */
    public record NecLine(UUID payeeId, String name, @Nullable String email, @Nullable LocalDate w9ReceivedOn,
            BigDecimal reportable, BigDecimal paidThroughNetworks, boolean meetsThreshold) {
    }

    public record NecReport(int year, BigDecimal threshold, List<NecLine> payees) {
    }

}
