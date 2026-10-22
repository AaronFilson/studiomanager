package io.github.aaronfilson.studiomanager.ledger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/** The ledger's types. */
public final class LedgerModel {

    private LedgerModel() {
    }

    /** Where money came from or went. CASH is the till; the rest are processors. */
    public enum Provider {
        STRIPE,
        SQUARE,
        SHOPIFY,
        PAYPAL,
        CASH,
        OTHER
    }

    public enum EntryKind {
        PIECE_FEE,
        RETAIL_SALE,
        PAYMENT,
        STORED_VALUE_SALE,
        STORED_VALUE_USE,
        STORED_VALUE_EXPIRY,
        PAYOUT,
        REFUND,
        ADJUSTMENT,
        CONSIGNMENT_SALE,
        CONSIGNMENT_REFUND,
        PAYEE_EARNING,
        PAYEE_PAYMENT
    }

    public enum StoredValueKind {
        GIFT_CARD,
        FIRING_CARD,
        CLASS_PACK,
        DEPOSIT,
        CREDIT
    }

    /** One line of a journal entry. Debits are positive, credits negative. */
    public record Line(LedgerAccount account, BigDecimal amount, @Nullable UUID member, @Nullable UUID storedValue,
            @Nullable Provider provider, @Nullable String taxCategory, @Nullable UUID payee) {

        public static Line debit(LedgerAccount account, BigDecimal amount) {
            return new Line(account, amount, null, null, null, null, null);
        }

        public static Line credit(LedgerAccount account, BigDecimal amount) {
            return new Line(account, amount.negate(), null, null, null, null, null);
        }

        public Line member(UUID id) {
            return new Line(account, amount, id, storedValue, provider, taxCategory, payee);
        }

        public Line storedValue(UUID id) {
            return new Line(account, amount, member, id, provider, taxCategory, payee);
        }

        public Line provider(Provider p) {
            return new Line(account, amount, member, storedValue, p, taxCategory, payee);
        }

        public Line taxCategory(String category) {
            return new Line(account, amount, member, storedValue, provider, category, payee);
        }

        public Line payee(UUID id) {
            return new Line(account, amount, member, storedValue, provider, taxCategory, id);
        }

        /** The same line on the other side, for reversing an entry. */
        public Line reversed() {
            return new Line(account, amount.negate(), member, storedValue, provider, taxCategory, payee);
        }
    }

    public record MemberBalance(UUID memberId, @Nullable String memberName, BigDecimal owed) {
    }

    public record StoredValueBalance(UUID id, StoredValueKind kind, @Nullable UUID holder, @Nullable String holderName,
            @Nullable String code, @Nullable String label, @Nullable LocalDate expiresOn, BigDecimal remaining) {
    }

    /** Opening, sold, used, expired and closing for one kind of stored value over a period. */
    public record StoredValueMovement(StoredValueKind kind, BigDecimal opening, BigDecimal sold, BigDecimal used,
            BigDecimal expired, BigDecimal closing) {
    }

    public record ClearingBalance(Provider provider, BigDecimal awaitingPayout) {
    }

    /** One account's totals on one day: what the export and accounting sync post. */
    public record SummaryLine(LocalDate date, LedgerAccount account, @Nullable String taxCategory, BigDecimal debits,
            BigDecimal credits) {
    }

    public record Payout(UUID id, Provider provider, String externalId, LocalDate arrivalOn, BigDecimal gross,
            BigDecimal refunds, BigDecimal fees, BigDecimal adjustments, BigDecimal net, String currency) {
    }

    public record TaxRate(UUID id, String taxCategory, BigDecimal rate, LocalDate effectiveFrom) {
    }

    public record Statement(UUID memberId, BigDecimal owed, List<StoredValueBalance> storedValue) {
    }

}
