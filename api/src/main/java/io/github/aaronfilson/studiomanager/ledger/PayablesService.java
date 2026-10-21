package io.github.aaronfilson.studiomanager.ledger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.EntryKind;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Line;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Provider;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.ConsignmentItem;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.ConsignmentSale;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.DiscountsFrom;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.Earning;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.EarningKind;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.NecLine;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.NecReport;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.Payee;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.PayeeChanges;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.PayeeStatement;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.Payment;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.PaymentMethod;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.StatementLine;
import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;

import org.jspecify.annotations.Nullable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What the studio owes the people it pays: consignors for work sold, instructors for their share
 * of a class. Each sale, share and payment is one balanced entry in the studio's journal, so a
 * payee's balance is a sum of {@link LedgerAccount#PAYEES_PAYABLE} lines like every other balance.
 *
 * <p>The research found the bookkeeping pain here, not in sales sync: a month's shares printed to
 * PDF and re-keyed, nothing recording whether a payment went out, and no W-9 or 1099 status.
 */
@Service
public class PayablesService {

    /** Consigned work is retail: the same tax category as clay and tools sold at the desk. */
    static final String RETAIL = "RETAIL";

    private final JdbcClient jdbc;
    private final LedgerService ledger;
    private final StudioMembers members;
    private final AccountDirectory accounts;

    PayablesService(JdbcClient jdbc, LedgerService ledger, StudioMembers members, AccountDirectory accounts) {
        this.jdbc = jdbc;
        this.ledger = ledger;
        this.members = members;
        this.accounts = accounts;
    }

    // ---- Payees

    @Transactional(readOnly = true)
    public List<Payee> payees() {
        requireStaff();
        return payeeQuery("", Map.of());
    }

    @Transactional(readOnly = true)
    public Payee payee(UUID payeeId) {
        requireStaff();
        return find(payeeId);
    }

    @Transactional
    public Payee createPayee(PayeeChanges changes) {
        TenantContext caller = requireStaff();
        validate(changes);
        UUID id = Uuids.v7();
        try {
            jdbc.sql("""
                    insert into payee (id, studio_id, name, email, account_id, files_1099, w9_received_on, consignor_share,
                                       discounts_from, archived_at, created_at)
                    values (:id, :studioId, :name, :email, :account, :files, :w9, :share, :discounts, null, :now)
                    """)
                    .param("id", id)
                    .param("studioId", caller.studioId())
                    .param("name", changes.name().strip())
                    .param("email", blankToNull(changes.email()))
                    .param("account", changes.accountId())
                    .param("files", changes.files1099())
                    .param("w9", changes.w9ReceivedOn())
                    .param("share", changes.consignorShare())
                    .param("discounts", changes.discountsFrom().name())
                    .param("now", now())
                    .update();
        } catch (DuplicateKeyException taken) {
            throw new ConflictException("That member is already a payee.");
        }
        return find(id);
    }

    @Transactional
    public Payee updatePayee(UUID payeeId, PayeeChanges changes) {
        requireStaff();
        validate(changes);
        find(payeeId);
        try {
            jdbc.sql("""
                    update payee set name = :name, email = :email, account_id = :account, files_1099 = :files,
                                     w9_received_on = :w9, consignor_share = :share, discounts_from = :discounts,
                                     archived_at = case when :archived then coalesce(archived_at, :now) end
                    where id = :id
                    """)
                    .param("id", payeeId)
                    .param("name", changes.name().strip())
                    .param("email", blankToNull(changes.email()))
                    .param("account", changes.accountId())
                    .param("files", changes.files1099())
                    .param("w9", changes.w9ReceivedOn())
                    .param("share", changes.consignorShare())
                    .param("discounts", changes.discountsFrom().name())
                    .param("archived", changes.archived())
                    .param("now", now())
                    .update();
        } catch (DuplicateKeyException taken) {
            throw new ConflictException("That member is already a payee.");
        }
        return find(payeeId);
    }

    // ---- Consignment

    /**
     * Takes in work to sell for an artist. The terms (the artist's share and who bears discounts)
     * are copied from the payee unless given, so changing a payee's terms later doesn't reprice
     * work already on the shelf.
     */
    @Transactional
    public ConsignmentItem takeIn(UUID payeeId, String description, BigDecimal retailPrice, int quantity,
            @Nullable BigDecimal consignorShare, @Nullable LocalDate receivedOn) {
        TenantContext caller = requireStaff();
        Payee payee = requireActive(payeeId);
        String text = description == null ? "" : description.strip();
        if (text.isEmpty() || text.length() > 120) {
            throw new InvalidInputException("Describe the work in 1 to 120 characters.");
        }
        requireMoney(retailPrice);
        if (quantity < 1 || quantity > 10_000) {
            throw new InvalidInputException("Take in from 1 to 10,000 of it.");
        }
        BigDecimal share = consignorShare != null ? consignorShare : payee.consignorShare();
        if (share == null) {
            throw new InvalidInputException("Set the artist's share, here or on the payee.");
        }
        requireShare(share);
        LocalDate received = receivedOn == null ? ledger.today() : notInFuture(receivedOn);
        UUID id = Uuids.v7();
        jdbc.sql("""
                insert into consignment_item (id, studio_id, payee_id, description, retail_price, consignor_share,
                                              discounts_from, quantity, received_on, created_at)
                values (:id, :studioId, :payee, :description, :price, :share, :discounts, :quantity, :received, :now)
                """)
                .param("id", id)
                .param("studioId", caller.studioId())
                .param("payee", payeeId)
                .param("description", text)
                .param("price", retailPrice)
                .param("share", share)
                .param("discounts", payee.discountsFrom().name())
                .param("quantity", quantity)
                .param("received", received)
                .param("now", now())
                .update();
        return item(id);
    }

    /** Consigned work, newest first; only what's still on the shelf unless asked for everything. */
    @Transactional(readOnly = true)
    public List<ConsignmentItem> consignmentItems(@Nullable UUID payeeId, boolean inStockOnly) {
        requireStaff();
        String where = "where (cast(:payee as uuid) is null or i.payee_id = :payee)"
                + (inStockOnly ? " and i.sold + i.returned < i.quantity" : "");
        return jdbc.sql(itemSelect(where) + " order by i.received_on desc, i.created_at desc limit 500")
                .param("payee", payeeId)
                .query(PayablesService::item)
                .list();
    }

    /**
     * Sells consigned work. {@code price} is what the buyer paid before tax, after any discount;
     * left out, it is the retail price. The artist's share is worked out by the item's terms:
     * a share of the price paid, or (when the studio bears discounts) a share of the retail
     * price, never more than was paid.
     */
    @Transactional
    public ConsignmentSale sell(UUID itemId, int quantity, @Nullable BigDecimal price, Provider provider) {
        TenantContext caller = requireStaff();
        // Lock the item so two sales at once can't both take the last one
        jdbc.sql("select id from consignment_item where id = :id for update").param("id", itemId)
                .query(UUID.class).optional().orElseThrow(() -> new NotFoundException("No such consigned item."));
        ConsignmentItem item = item(itemId);
        if (quantity < 1) {
            throw new InvalidInputException("Sell at least one.");
        }
        if (quantity > item.inStock()) {
            throw new InvalidInputException("Only %d left in stock.".formatted(item.inStock()));
        }
        BigDecimal retail = item.retailPrice().multiply(BigDecimal.valueOf(quantity));
        BigDecimal paid = price == null ? retail : price;
        requireMoney(paid);
        BigDecimal consignorAmount = consignorAmount(item, retail, paid, ledger.currency().getDefaultFractionDigits());
        LocalDate day = ledger.today();
        BigDecimal tax = ledger.taxOn(paid, RETAIL, day, ledger.currency());

        UUID saleId = Uuids.v7();
        UUID entryId = ledger.post(EntryKind.CONSIGNMENT_SALE, day,
                truncate("Sold %d × %s (%s)".formatted(quantity, item.description(), item.payeeName())), itemId,
                "consignment-sale:" + saleId, saleLines(item.payeeId(), paid, consignorAmount, tax, provider));
        jdbc.sql("""
                insert into consignment_sale (id, studio_id, item_id, quantity, price, consignor_amount, tax, provider,
                                              sold_on, entry_id, created_at)
                values (:id, :studioId, :item, :quantity, :price, :consignor, :tax, :provider, :day, :entry, :now)
                """)
                .param("id", saleId)
                .param("studioId", caller.studioId())
                .param("item", itemId)
                .param("quantity", quantity)
                .param("price", paid)
                .param("consignor", consignorAmount)
                .param("tax", tax)
                .param("provider", provider.name())
                .param("day", day)
                .param("entry", entryId)
                .param("now", now())
                .update();
        jdbc.sql("update consignment_item set sold = sold + :quantity where id = :id")
                .param("quantity", quantity).param("id", itemId).update();
        return sale(saleId);
    }

    /**
     * Refunds a sale in full: the artist's share, the commission and the tax are all reversed,
     * and the work goes back on the shelf. If the artist has already been paid, they owe it back,
     * and it comes off their next sales.
     *
     * <p>Money given back in cash leaves the till now. Money given back through a processor
     * comes out of a later payout, whose import debits {@link LedgerAccount#REFUNDS}; this credits
     * REFUNDS, so the two meet there instead of counting the refund twice.
     */
    @Transactional
    public ConsignmentSale refund(UUID saleId) {
        requireStaff();
        jdbc.sql("select id from consignment_sale where id = :id for update").param("id", saleId)
                .query(UUID.class).optional().orElseThrow(() -> new NotFoundException("No such sale."));
        ConsignmentSale sale = sale(saleId);
        if (sale.refundedOn() != null) {
            throw new ConflictException("That sale was already refunded.");
        }
        ConsignmentItem item = item(sale.itemId());
        List<Line> lines = new ArrayList<>();
        for (Line line : saleLines(item.payeeId(), sale.price(), sale.consignorAmount(), sale.tax(), sale.provider())) {
            if (line.account() == LedgerAccount.PROCESSOR_CLEARING && sale.provider() != Provider.CASH) {
                lines.add(Line.credit(LedgerAccount.REFUNDS, line.amount()).provider(sale.provider()));
            } else {
                lines.add(line.reversed());
            }
        }
        LocalDate day = ledger.today();
        UUID entryId = ledger.post(EntryKind.CONSIGNMENT_REFUND, day,
                truncate("Refunded %d × %s (%s)".formatted(sale.quantity(), item.description(), item.payeeName())),
                item.id(), "consignment-refund:" + saleId, lines);
        jdbc.sql("update consignment_sale set refunded_on = :day, refund_entry_id = :entry where id = :id")
                .param("day", day).param("entry", entryId).param("id", saleId).update();
        jdbc.sql("update consignment_item set sold = sold - :quantity where id = :id")
                .param("quantity", sale.quantity()).param("id", item.id()).update();
        return sale(saleId);
    }

    /** Gives unsold work back to the artist. */
    @Transactional
    public ConsignmentItem returnToArtist(UUID itemId, int quantity) {
        requireStaff();
        jdbc.sql("select id from consignment_item where id = :id for update").param("id", itemId)
                .query(UUID.class).optional().orElseThrow(() -> new NotFoundException("No such consigned item."));
        ConsignmentItem item = item(itemId);
        if (quantity < 1 || quantity > item.inStock()) {
            throw new InvalidInputException("Return from 1 to %d.".formatted(item.inStock()));
        }
        jdbc.sql("update consignment_item set returned = returned + :quantity where id = :id")
                .param("quantity", quantity).param("id", itemId).update();
        return item(itemId);
    }

    /** Sales of a payee's work, or of everyone's, newest first. */
    @Transactional(readOnly = true)
    public List<ConsignmentSale> consignmentSales(@Nullable UUID payeeId) {
        requireStaff();
        return jdbc.sql("""
                select s.id, s.item_id, i.description, s.quantity, s.price, s.consignor_amount, s.tax, s.provider,
                       s.sold_on, s.refunded_on
                from consignment_sale s join consignment_item i on i.id = s.item_id
                where cast(:payee as uuid) is null or i.payee_id = :payee
                order by s.sold_on desc, s.created_at desc limit 200
                """)
                .param("payee", payeeId)
                .query(PayablesService::sale)
                .list();
    }

    // ---- Shares, fees and payments

    /**
     * Records a share or fee a payee has earned, such as an instructor's 70% of a class. Give
     * either the amount, or what the class took and the payee's share of it. Only an adjustment
     * can be negative, to claw back an amount recorded in error.
     */
    @Transactional
    public Earning recordEarning(UUID payeeId, EarningKind kind, String description, @Nullable LocalDate earnedOn,
            @Nullable BigDecimal amount, @Nullable BigDecimal gross, @Nullable BigDecimal share) {
        TenantContext caller = requireStaff();
        requireActive(payeeId);
        String text = description == null ? "" : description.strip();
        if (text.isEmpty() || text.length() > 120) {
            throw new InvalidInputException("Describe what it's for in 1 to 120 characters.");
        }
        int digits = ledger.currency().getDefaultFractionDigits();
        BigDecimal earned;
        if (gross != null || share != null) {
            if (gross == null || share == null || amount != null) {
                throw new InvalidInputException("Give the amount, or what the class took and the share, not both.");
            }
            requireMoney(gross);
            requireShare(share);
            earned = gross.multiply(share).setScale(digits, RoundingMode.HALF_UP);
        } else if (amount != null) {
            requireScale(amount);
            earned = amount;
        } else {
            throw new InvalidInputException("Give the amount, or what the class took and the share.");
        }
        if (earned.signum() == 0) {
            throw new InvalidInputException("The amount must not be zero.");
        }
        if (earned.signum() < 0 && kind != EarningKind.ADJUSTMENT) {
            throw new InvalidInputException("Only an adjustment can take money back.");
        }
        LocalDate day = earnedOn == null ? ledger.today() : notInFuture(earnedOn);
        UUID id = Uuids.v7();
        List<Line> lines = List.of(
                Line.debit(LedgerAccount.CONTRACT_SERVICES, earned),
                Line.credit(LedgerAccount.PAYEES_PAYABLE, earned).payee(payeeId));
        UUID entryId = ledger.post(EntryKind.PAYEE_EARNING, day, truncate(text), id, "payee-earning:" + id, lines);
        jdbc.sql("""
                insert into payee_earning (id, studio_id, payee_id, kind, description, earned_on, gross, share, amount,
                                           entry_id, created_by, created_at)
                values (:id, :studioId, :payee, :kind, :description, :day, :gross, :share, :amount, :entry, :by, :now)
                """)
                .param("id", id)
                .param("studioId", caller.studioId())
                .param("payee", payeeId)
                .param("kind", kind.name())
                .param("description", text)
                .param("day", day)
                .param("gross", gross)
                .param("share", share)
                .param("amount", earned)
                .param("entry", entryId)
                .param("by", caller.accountId())
                .param("now", now())
                .update();
        return new Earning(id, payeeId, kind, text, day, gross, share, earned);
    }

    /**
     * Records money sent to a payee, up to what they're owed. Studio credit pays a member-payee
     * by taking it off what they owe the studio, so an artist's sales can cover their firing fees.
     */
    @Transactional
    public Payment pay(UUID payeeId, BigDecimal amount, PaymentMethod method, @Nullable String reference,
            @Nullable LocalDate paidOn) {
        TenantContext caller = requireStaff();
        // Lock the payee so two payments at once can't both pay the same balance
        jdbc.sql("select id from payee where id = :id for update").param("id", payeeId)
                .query(UUID.class).optional().orElseThrow(() -> new NotFoundException("No such payee."));
        Payee payee = find(payeeId);
        requireMoney(amount);
        if (amount.compareTo(payee.owed()) > 0) {
            throw new InvalidInputException("Only %s is owed to %s.".formatted(payee.owed().toPlainString(), payee.name()));
        }
        if (method == PaymentMethod.STUDIO_CREDIT
                && (payee.accountId() == null || members.roleOf(payee.accountId()).isEmpty())) {
            throw new InvalidInputException("Only a payee who is a member here can be paid in studio credit.");
        }
        String ref = blankToNull(reference);
        if (ref != null && ref.length() > 80) {
            throw new InvalidInputException("Keep the reference to 80 characters.");
        }
        LocalDate day = paidOn == null ? ledger.today() : notInFuture(paidOn);
        UUID id = Uuids.v7();
        List<Line> lines = List.of(
                Line.debit(LedgerAccount.PAYEES_PAYABLE, amount).payee(payeeId),
                source(method, amount, payee.accountId()));
        UUID entryId = ledger.post(EntryKind.PAYEE_PAYMENT, day,
                truncate("Paid %s, %s%s".formatted(payee.name(), describe(method), ref == null ? "" : " " + ref)),
                id, "payee-payment:" + id, lines);
        jdbc.sql("""
                insert into payee_payment (id, studio_id, payee_id, paid_on, amount, method, reference, entry_id,
                                           created_by, created_at)
                values (:id, :studioId, :payee, :day, :amount, :method, :reference, :entry, :by, :now)
                """)
                .param("id", id)
                .param("studioId", caller.studioId())
                .param("payee", payeeId)
                .param("day", day)
                .param("amount", amount)
                .param("method", method.name())
                .param("reference", ref)
                .param("entry", entryId)
                .param("by", caller.accountId())
                .param("now", now())
                .update();
        return new Payment(id, payeeId, day, amount, method, ref);
    }

    // ---- Reports

    /** What a payee was owed at the start, each sale, share and payment over the period, and the end. */
    @Transactional(readOnly = true)
    public PayeeStatement statement(UUID payeeId, LocalDate from, LocalDate to) {
        requireStaff();
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 366) {
            throw new InvalidInputException("Ask for a range of up to a year.");
        }
        Payee payee = find(payeeId);
        BigDecimal opening = jdbc.sql("""
                select coalesce(-sum(l.amount), 0) from journal_line l join journal_entry e on e.id = l.entry_id
                where l.account = 'PAYEES_PAYABLE' and l.payee_id = :payee and e.occurred_on < :from
                """)
                .param("payee", payeeId).param("from", from).query(BigDecimal.class).single();
        List<StatementLine> lines = jdbc.sql("""
                select e.occurred_on, e.kind, e.memo, -sum(l.amount) as amount
                from journal_line l join journal_entry e on e.id = l.entry_id
                where l.account = 'PAYEES_PAYABLE' and l.payee_id = :payee and e.occurred_on between :from and :to
                group by e.id, e.occurred_on, e.kind, e.memo, e.created_at
                order by e.occurred_on, e.created_at
                """)
                .param("payee", payeeId).param("from", from).param("to", to)
                .query((rs, row) -> new StatementLine(rs.getObject("occurred_on", LocalDate.class),
                        EntryKind.valueOf(rs.getString("kind")), rs.getString("memo"), rs.getBigDecimal("amount")))
                .list();
        BigDecimal closing = lines.stream().map(StatementLine::amount).reduce(opening, BigDecimal::add);
        return new PayeeStatement(payeeId, payee.name(), from, to, opening, lines, closing);
    }

    /**
     * Payments in a calendar year to each payee the studio files a 1099-NEC for. Only payments
     * by cheque, bank transfer, cash or studio credit count: the IRS has card and payment-network
     * payments reported on the network's 1099-K instead. A payee is flagged once the reportable
     * total reaches the year's threshold.
     */
    @Transactional(readOnly = true)
    public NecReport necReport(int year) {
        requireStaff();
        if (year < 2000 || year > ledger.today().getYear()) {
            throw new InvalidInputException("Ask for a year that has started.");
        }
        BigDecimal threshold = necThreshold(year);
        List<String> reportable = java.util.Arrays.stream(PaymentMethod.values())
                .filter(PaymentMethod::countsTowardsNec).map(Enum::name).toList();
        List<NecLine> lines = jdbc.sql("""
                select p.id, p.name, p.email, p.w9_received_on,
                       coalesce(sum(m.amount) filter (where m.method in (:reportable)), 0) as reportable,
                       coalesce(sum(m.amount) filter (where m.method not in (:reportable)), 0) as networks
                from payee p join payee_payment m on m.payee_id = p.id
                where p.files_1099 and m.paid_on >= :start and m.paid_on < :end
                group by p.id, p.name, p.email, p.w9_received_on
                order by p.name
                """)
                .param("reportable", reportable)
                .param("start", LocalDate.of(year, 1, 1))
                .param("end", LocalDate.of(year + 1, 1, 1))
                .query((rs, row) -> {
                    BigDecimal total = rs.getBigDecimal("reportable");
                    return new NecLine(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("email"),
                            rs.getObject("w9_received_on", LocalDate.class), total, rs.getBigDecimal("networks"),
                            total.compareTo(threshold) >= 0);
                })
                .list();
        return new NecReport(year, threshold, lines);
    }

    /**
     * The 1099-NEC box 1 threshold for payments made in {@code year}: $600 through 2025, $2,000
     * for 2026 (IRS instructions for Forms 1099-MISC and 1099-NEC). Later years may be indexed
     * for inflation; when the IRS publishes a new figure, add it here.
     */
    static BigDecimal necThreshold(int year) {
        return year < 2026 ? new BigDecimal("600") : new BigDecimal("2000");
    }

    /** The artist's share of a sale, by the item's terms, never more than was paid. */
    static BigDecimal consignorAmount(ConsignmentItem item, BigDecimal retail, BigDecimal paid, int digits) {
        BigDecimal base = item.discountsFrom() == DiscountsFrom.STUDIO ? retail : paid;
        return base.multiply(item.consignorShare()).setScale(digits, RoundingMode.HALF_UP).min(paid);
    }

    // ---- Internals

    private static List<Line> saleLines(UUID payeeId, BigDecimal paid, BigDecimal consignorAmount, BigDecimal tax,
            Provider provider) {
        List<Line> lines = new ArrayList<>();
        lines.add(Line.debit(LedgerAccount.PROCESSOR_CLEARING, paid.add(tax)).provider(provider));
        if (consignorAmount.signum() != 0) {
            lines.add(Line.credit(LedgerAccount.PAYEES_PAYABLE, consignorAmount).payee(payeeId));
        }
        BigDecimal commission = paid.subtract(consignorAmount);
        if (commission.signum() != 0) {
            lines.add(Line.credit(LedgerAccount.REVENUE_COMMISSIONS, commission).taxCategory(RETAIL));
        }
        if (tax.signum() != 0) {
            lines.add(Line.credit(LedgerAccount.SALES_TAX_PAYABLE, tax).taxCategory(RETAIL));
        }
        return lines;
    }

    /** Where the money for a payment comes from. */
    private static Line source(PaymentMethod method, BigDecimal amount, @Nullable UUID accountId) {
        return switch (method) {
            case CHECK, BANK_TRANSFER -> Line.credit(LedgerAccount.BANK, amount);
            case CASH -> Line.credit(LedgerAccount.PROCESSOR_CLEARING, amount).provider(Provider.CASH);
            case PAYPAL -> Line.credit(LedgerAccount.PROCESSOR_CLEARING, amount).provider(Provider.PAYPAL);
            case STRIPE -> Line.credit(LedgerAccount.PROCESSOR_CLEARING, amount).provider(Provider.STRIPE);
            case STUDIO_CREDIT -> Line.credit(LedgerAccount.MEMBER_RECEIVABLE, amount).member(Objects.requireNonNull(accountId));
        };
    }

    private static String describe(PaymentMethod method) {
        return switch (method) {
            case CHECK -> "cheque";
            case BANK_TRANSFER -> "bank transfer";
            case CASH -> "cash";
            case PAYPAL -> "PayPal";
            case STRIPE -> "Stripe";
            case STUDIO_CREDIT -> "studio credit";
        };
    }

    private Payee find(UUID payeeId) {
        return payeeQuery("where p.id = :id", Map.of("id", payeeId)).stream().findFirst()
                .orElseThrow(() -> new NotFoundException("No such payee."));
    }

    private Payee requireActive(UUID payeeId) {
        Payee payee = find(payeeId);
        if (payee.archived()) {
            throw new InvalidInputException("%s is archived. Restore them first.".formatted(payee.name()));
        }
        return payee;
    }

    private List<Payee> payeeQuery(String where, Map<String, Object> params) {
        var spec = jdbc.sql("""
                select p.id, p.name, p.email, p.account_id, p.files_1099, p.w9_received_on, p.consignor_share,
                       p.discounts_from, p.archived_at is not null as archived,
                       coalesce(-(select sum(l.amount) from journal_line l
                                  where l.account = 'PAYEES_PAYABLE' and l.payee_id = p.id), 0) as owed
                from payee p %s order by p.archived_at nulls first, p.name limit 500
                """.formatted(where));
        for (var param : params.entrySet()) {
            spec = spec.param(param.getKey(), param.getValue());
        }
        List<Payee> raw = spec.query((rs, row) -> new Payee(rs.getObject("id", UUID.class), rs.getString("name"),
                rs.getString("email"), rs.getObject("account_id", UUID.class), null, rs.getBoolean("files_1099"),
                rs.getObject("w9_received_on", LocalDate.class), rs.getBigDecimal("consignor_share"),
                DiscountsFrom.valueOf(rs.getString("discounts_from")), rs.getBoolean("archived"),
                rs.getBigDecimal("owed"))).list();
        Map<UUID, String> names = accounts.displayNames(raw.stream().map(Payee::accountId).filter(Objects::nonNull).toList());
        return raw.stream()
                .map(p -> new Payee(p.id(), p.name(), p.email(), p.accountId(),
                        p.accountId() == null ? null : names.get(p.accountId()), p.files1099(), p.w9ReceivedOn(),
                        p.consignorShare(), p.discountsFrom(), p.archived(), p.owed()))
                .toList();
    }

    private ConsignmentItem item(UUID itemId) {
        return jdbc.sql(itemSelect("where i.id = :id")).param("id", itemId).query(PayablesService::item).optional()
                .orElseThrow(() -> new NotFoundException("No such consigned item."));
    }

    private static String itemSelect(String where) {
        return """
                select i.id, i.payee_id, p.name as payee_name, i.description, i.retail_price, i.consignor_share,
                       i.discounts_from, i.quantity, i.sold, i.returned, i.received_on
                from consignment_item i join payee p on p.id = i.payee_id
                """ + where;
    }

    private static ConsignmentItem item(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new ConsignmentItem(rs.getObject("id", UUID.class), rs.getObject("payee_id", UUID.class),
                rs.getString("payee_name"), rs.getString("description"), rs.getBigDecimal("retail_price"),
                rs.getBigDecimal("consignor_share"), DiscountsFrom.valueOf(rs.getString("discounts_from")),
                rs.getInt("quantity"), rs.getInt("sold"), rs.getInt("returned"),
                rs.getObject("received_on", LocalDate.class));
    }

    private ConsignmentSale sale(UUID saleId) {
        return jdbc.sql("""
                select s.id, s.item_id, i.description, s.quantity, s.price, s.consignor_amount, s.tax, s.provider,
                       s.sold_on, s.refunded_on
                from consignment_sale s join consignment_item i on i.id = s.item_id where s.id = :id
                """)
                .param("id", saleId).query(PayablesService::sale).optional()
                .orElseThrow(() -> new NotFoundException("No such sale."));
    }

    private static ConsignmentSale sale(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new ConsignmentSale(rs.getObject("id", UUID.class), rs.getObject("item_id", UUID.class),
                rs.getString("description"), rs.getInt("quantity"), rs.getBigDecimal("price"), rs.getBigDecimal("consignor_amount"),
                rs.getBigDecimal("tax"), Provider.valueOf(rs.getString("provider")),
                rs.getObject("sold_on", LocalDate.class), rs.getObject("refunded_on", LocalDate.class));
    }

    private void validate(PayeeChanges changes) {
        String name = changes.name() == null ? "" : changes.name().strip();
        if (name.isEmpty() || name.length() > 120) {
            throw new InvalidInputException("Name the payee in 1 to 120 characters.");
        }
        String email = blankToNull(changes.email());
        if (email != null && (email.length() > 254 || !email.contains("@"))) {
            throw new InvalidInputException("That email address doesn't look right.");
        }
        if (changes.accountId() != null && members.roleOf(changes.accountId()).isEmpty()) {
            throw new InvalidInputException("That person isn't a member of this studio.");
        }
        if (changes.consignorShare() != null) {
            requireShare(changes.consignorShare());
        }
        if (changes.w9ReceivedOn() != null) {
            notInFuture(changes.w9ReceivedOn());
        }
    }

    private void requireMoney(BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new InvalidInputException("The amount must be more than zero.");
        }
        requireScale(amount);
    }

    private void requireScale(BigDecimal amount) {
        if (amount.stripTrailingZeros().scale() > ledger.currency().getDefaultFractionDigits()) {
            throw new InvalidInputException("Too many decimal places for " + ledger.currency().getCurrencyCode() + ".");
        }
    }

    private static void requireShare(BigDecimal share) {
        if (share.signum() <= 0 || share.compareTo(BigDecimal.ONE) > 0 || share.stripTrailingZeros().scale() > 4) {
            throw new InvalidInputException("A share is a fraction above 0 and up to 1 (0.6 is 60%).");
        }
    }

    private LocalDate notInFuture(LocalDate day) {
        if (day.isAfter(ledger.today())) {
            throw new InvalidInputException("That date hasn't happened yet.");
        }
        return day;
    }

    private static @Nullable String blankToNull(@Nullable String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }

    private static String truncate(String text) {
        return text.length() <= 200 ? text : text.substring(0, 200);
    }

    private static java.time.OffsetDateTime now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC);
    }

    private static TenantContext requireStaff() {
        TenantContext caller = TenantContext.require();
        if (caller.role() == null || !caller.role().includes(StudioRole.STAFF)) {
            throw new AccessDeniedException("Only staff can do this.");
        }
        return caller;
    }

}
