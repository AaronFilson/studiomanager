package io.github.aaronfilson.studiomanager.ledger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Currency;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.ClearingBalance;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.EntryKind;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Line;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.MemberBalance;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Payout;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Provider;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.StoredValueBalance;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.StoredValueKind;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.StoredValueMovement;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.SummaryLine;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.TaxRate;
import io.github.aaronfilson.studiomanager.piece.PieceReceived;
import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;
import io.github.aaronfilson.studiomanager.studio.StudioService;
import io.github.aaronfilson.studiomanager.supplies.SupplySold;

import org.jspecify.annotations.Nullable;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The studio's books. Every money event is one balanced journal entry; every balance is a sum.
 *
 * <p>Two rules keep it honest. The database refuses an entry whose lines don't sum to zero (a
 * deferred constraint trigger), and each entry carries an idempotency key, so an event delivered
 * twice or a payout file imported twice posts once.
 */
@Service
public class LedgerService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final JdbcClient jdbc;
    private final StudioService studios;
    private final StudioMembers members;
    private final AccountDirectory accounts;

    LedgerService(JdbcClient jdbc, StudioService studios, StudioMembers members, AccountDirectory accounts) {
        this.jdbc = jdbc;
        this.studios = studios;
        this.members = members;
        this.accounts = accounts;
    }

    // ---- Posting

    /** A piece's fee becomes what its maker owes, with sales tax at the rate in force that day. */
    @EventListener
    void onPieceReceived(PieceReceived piece) {
        if (piece.fee().amount().signum() == 0) {
            return;
        }
        LocalDate day = localDate(piece.receivedAt());
        BigDecimal fee = piece.fee().amount();
        BigDecimal tax = taxOn(fee, piece.taxCategory(), day, piece.fee().currency());
        List<Line> lines = new ArrayList<>();
        lines.add(Line.debit(LedgerAccount.MEMBER_RECEIVABLE, fee.add(tax)).member(piece.makerAccountId()));
        lines.add(Line.credit(LedgerAccount.REVENUE_FIRING, fee).taxCategory(piece.taxCategory()));
        if (tax.signum() != 0) {
            lines.add(Line.credit(LedgerAccount.SALES_TAX_PAYABLE, tax).taxCategory(piece.taxCategory()));
        }
        post(EntryKind.PIECE_FEE, day, "Firing fee, piece " + piece.number(), piece.pieceId(),
                "piece-fee:" + piece.pieceId(), lines);
    }

    /** Something sold to a member (a bag of clay, a tool) goes on their balance, with tax. */
    @EventListener
    void onSupplySold(SupplySold sale) {
        LocalDate day = localDate(sale.soldAt());
        String category = sale.taxCategory() == null ? "RETAIL" : sale.taxCategory();
        BigDecimal tax = taxOn(sale.amount(), category, day, currency());
        List<Line> lines = new ArrayList<>();
        lines.add(Line.debit(LedgerAccount.MEMBER_RECEIVABLE, sale.amount().add(tax)).member(sale.memberId()));
        lines.add(Line.credit(LedgerAccount.REVENUE_RETAIL, sale.amount()).taxCategory(category));
        if (tax.signum() != 0) {
            lines.add(Line.credit(LedgerAccount.SALES_TAX_PAYABLE, tax).taxCategory(category));
        }
        if (sale.amount().signum() != 0) {
            post(EntryKind.RETAIL_SALE, day,
                    truncate("%s × %s".formatted(sale.quantity().stripTrailingZeros().toPlainString(), sale.itemName())),
                    sale.movementId(), "supply-sale:" + sale.movementId(), lines);
        }
    }

    /** A member pays some or all of what they owe, at the desk or online. */
    @Transactional
    public MemberBalance recordPayment(UUID memberId, BigDecimal amount, Provider provider, @Nullable String reference) {
        TenantContext caller = requireStaff();
        requireMember(memberId);
        requirePositive(amount);
        String memo = "Payment" + (reference == null || reference.isBlank() ? "" : " " + reference.strip());
        post(EntryKind.PAYMENT, today(), truncate(memo), null, "payment:" + Uuids.v7(), List.of(
                Line.debit(LedgerAccount.PROCESSOR_CLEARING, amount).provider(provider),
                Line.credit(LedgerAccount.MEMBER_RECEIVABLE, amount).member(memberId)));
        return balanceOf(memberId, caller);
    }

    /**
     * Sells stored value: a gift card (with a code anyone can redeem), or a firing card, class
     * pack, deposit or credit for a member. The money is a liability until used.
     */
    @Transactional
    public StoredValueBalance sellStoredValue(StoredValueKind kind, @Nullable UUID holder, @Nullable String label,
            BigDecimal amount, Provider provider, @Nullable LocalDate expiresOn) {
        TenantContext caller = requireStaff();
        requirePositive(amount);
        if (kind != StoredValueKind.GIFT_CARD && holder == null) {
            throw new InvalidInputException("Only a gift card can be sold without naming who holds it.");
        }
        if (holder != null) {
            requireMember(holder);
        }
        // US Regulation E: gift cards can't expire within five years of sale
        if (kind == StoredValueKind.GIFT_CARD && expiresOn != null && expiresOn.isBefore(today().plusYears(5))
                && "US".equals(studios.current().country())) {
            throw new InvalidInputException("In the US a gift card can't expire within five years of being sold.");
        }
        UUID id = Uuids.v7();
        String code = kind == StoredValueKind.GIFT_CARD ? newCode() : null;
        jdbc.sql("""
                insert into stored_value (id, studio_id, kind, holder, code, label, expires_on, created_at)
                values (:id, :studioId, :kind, :holder, :code, :label, :expires, :now)
                """)
                .param("id", id)
                .param("studioId", caller.studioId())
                .param("kind", kind.name())
                .param("holder", holder)
                .param("code", code)
                .param("label", label == null || label.isBlank() ? null : truncate(label.strip(), 80))
                .param("expires", expiresOn)
                .param("now", utc(now()))
                .update();
        post(EntryKind.STORED_VALUE_SALE, today(), truncate(describe(kind) + " sold"), id, "sv-sale:" + id, List.of(
                Line.debit(LedgerAccount.PROCESSOR_CLEARING, amount).provider(provider),
                Line.credit(LedgerAccount.STORED_VALUE_LIABILITY, amount).storedValue(id)));
        return storedValue(id);
    }

    /** Uses stored value to pay down what a member owes. */
    @Transactional
    public StoredValueBalance useStoredValue(UUID storedValueId, UUID memberId, BigDecimal amount) {
        requireStaff();
        requireMember(memberId);
        requirePositive(amount);
        // Lock the balance so two uses at once can't both spend it
        jdbc.sql("select id from stored_value where id = :id for update").param("id", storedValueId)
                .query(UUID.class).optional().orElseThrow(() -> new NotFoundException("No such balance."));
        StoredValueBalance balance = storedValue(storedValueId);
        if (balance.holder() != null && !balance.holder().equals(memberId)) {
            throw new InvalidInputException("That balance belongs to someone else.");
        }
        if (balance.expiresOn() != null && balance.expiresOn().isBefore(today())) {
            throw new InvalidInputException("That balance has expired.");
        }
        if (balance.remaining().compareTo(amount) < 0) {
            throw new InvalidInputException("Only %s is left on it.".formatted(balance.remaining().toPlainString()));
        }
        post(EntryKind.STORED_VALUE_USE, today(), truncate(describe(balance.kind()) + " used"), storedValueId,
                "sv-use:" + Uuids.v7(), List.of(
                        Line.debit(LedgerAccount.STORED_VALUE_LIABILITY, amount).storedValue(storedValueId),
                        Line.credit(LedgerAccount.MEMBER_RECEIVABLE, amount).member(memberId)));
        return storedValue(storedValueId);
    }

    /** Writes off what's left on a lapsed balance as breakage revenue. */
    @Transactional
    public StoredValueBalance expireStoredValue(UUID storedValueId) {
        requireStaff();
        StoredValueBalance balance = storedValue(storedValueId);
        if (balance.remaining().signum() <= 0) {
            throw new InvalidInputException("Nothing is left on it.");
        }
        post(EntryKind.STORED_VALUE_EXPIRY, today(), truncate(describe(balance.kind()) + " expired"), storedValueId,
                "sv-expiry:" + storedValueId, List.of(
                        Line.debit(LedgerAccount.STORED_VALUE_LIABILITY, balance.remaining()).storedValue(storedValueId),
                        Line.credit(LedgerAccount.REVENUE_BREAKAGE, balance.remaining())));
        return storedValue(storedValueId);
    }

    /**
     * Records a processor's payout: the deposit is what was taken, less refunds and fees, plus any
     * adjustment. Importing the same payout twice is a no-op. Works for any provider, so a studio
     * taking money through Square, Stripe and a marketplace reconciles all of them in one place.
     */
    @Transactional
    public Payout importPayout(Provider provider, String externalId, LocalDate arrivalOn, BigDecimal gross,
            BigDecimal refunds, BigDecimal fees, BigDecimal adjustments, BigDecimal net) {
        TenantContext caller = requireStaff();
        if (provider == Provider.CASH) {
            throw new InvalidInputException("Cash isn't paid out by a processor; record a bank deposit instead.");
        }
        if (net.compareTo(gross.subtract(refunds).subtract(fees).add(adjustments)) != 0) {
            throw new InvalidInputException("The payout doesn't add up: net must be gross − refunds − fees + adjustments.");
        }
        String key = "payout:%s:%s".formatted(provider, externalId);
        var existing = payouts().stream()
                .filter(p -> p.provider() == provider && p.externalId().equals(externalId)).findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }
        List<Line> lines = new ArrayList<>();
        addIfNonZero(lines, Line.debit(LedgerAccount.BANK, net));
        addIfNonZero(lines, Line.debit(LedgerAccount.PROCESSOR_FEES, fees).provider(provider));
        addIfNonZero(lines, Line.debit(LedgerAccount.REFUNDS, refunds).provider(provider));
        addIfNonZero(lines, Line.credit(LedgerAccount.PROCESSOR_CLEARING, gross).provider(provider));
        addIfNonZero(lines, Line.credit(LedgerAccount.REVENUE_OTHER, adjustments).provider(provider));
        UUID entryId = post(EntryKind.PAYOUT, arrivalOn, truncate("%s payout %s".formatted(provider, externalId)),
                null, key, lines);
        UUID id = Uuids.v7();
        try {
            jdbc.sql("""
                    insert into payout (id, studio_id, provider, external_id, arrival_on, gross, refunds, fees,
                                        adjustments, net, currency, entry_id, imported_at)
                    values (:id, :studioId, :provider, :externalId, :arrival, :gross, :refunds, :fees,
                            :adjustments, :net, :currency, :entryId, :now)
                    """)
                    .param("id", id)
                    .param("studioId", caller.studioId())
                    .param("provider", provider.name())
                    .param("externalId", externalId)
                    .param("arrival", arrivalOn)
                    .param("gross", gross)
                    .param("refunds", refunds)
                    .param("fees", fees)
                    .param("adjustments", adjustments)
                    .param("net", net)
                    .param("currency", currency().getCurrencyCode())
                    .param("entryId", entryId)
                    .param("now", utc(now()))
                    .update();
        } catch (DuplicateKeyException race) {
            throw new ConflictException("That payout was just imported by someone else.");
        }
        return payouts().stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow();
    }

    // ---- Reading

    /** Everyone who owes something (or is in credit), largest first. Staff only. */
    @Transactional(readOnly = true)
    public List<MemberBalance> balances() {
        requireStaff();
        List<MemberBalance> raw = jdbc.sql("""
                select member_id, sum(amount) as owed from journal_line
                where account = 'MEMBER_RECEIVABLE' group by member_id having sum(amount) <> 0
                order by sum(amount) desc
                """)
                .query((rs, row) -> new MemberBalance(rs.getObject("member_id", UUID.class), null, rs.getBigDecimal("owed")))
                .list();
        Map<UUID, String> names = accounts.displayNames(raw.stream().map(MemberBalance::memberId).toList());
        return raw.stream().map(b -> new MemberBalance(b.memberId(), names.get(b.memberId()), b.owed())).toList();
    }

    /** One member's balance owed. Members can see their own. */
    @Transactional(readOnly = true)
    public MemberBalance balanceOf(UUID memberId) {
        return balanceOf(memberId, TenantContext.require());
    }

    /** One member's stored-value balances. Members can see their own. */
    @Transactional(readOnly = true)
    public List<StoredValueBalance> storedValuesOf(UUID memberId) {
        TenantContext caller = TenantContext.require();
        if (!memberId.equals(caller.accountId()) && !isStaff(caller)) {
            throw new AccessDeniedException("You can see only your own balances.");
        }
        return storedValueQuery("where s.holder = :holder", Map.of("holder", memberId));
    }

    /** Every stored-value balance with something left, staff only. */
    @Transactional(readOnly = true)
    public List<StoredValueBalance> storedValues() {
        requireStaff();
        return storedValueQuery("", Map.of()).stream().filter(s -> s.remaining().signum() != 0).toList();
    }

    /** Looks up a gift card by its code, for redeeming it at the desk. */
    @Transactional(readOnly = true)
    public StoredValueBalance giftCard(String code) {
        requireStaff();
        return storedValueQuery("where s.code = :code", Map.of("code", code.strip().toUpperCase()))
                .stream().findFirst()
                .orElseThrow(() -> new NotFoundException("No gift card with that code."));
    }

    /**
     * Stored value by kind over [{@code from}, {@code to}]: opening, sold, used, expired, closing.
     * The roll-forward a bookkeeper asks for to support the liability on the balance sheet.
     */
    @Transactional(readOnly = true)
    public List<StoredValueMovement> storedValueMovements(LocalDate from, LocalDate to) {
        requireStaff();
        requireDateRange(from, to);
        Map<StoredValueKind, BigDecimal[]> totals = new EnumMap<>(StoredValueKind.class);
        for (StoredValueKind kind : StoredValueKind.values()) {
            totals.put(kind, new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
        }
        jdbc.sql("""
                select s.kind, e.kind as entry_kind, e.occurred_on < :from as before, -sum(l.amount) as amount
                from journal_line l
                join journal_entry e on e.id = l.entry_id
                join stored_value s on s.id = l.stored_value
                where l.account = 'STORED_VALUE_LIABILITY' and e.occurred_on <= :to
                -- By position: the two :from placeholders are separate parameters to Postgres
                group by 1, 2, 3
                """)
                .param("from", from)
                .param("to", to)
                .query(rs -> {
                    BigDecimal[] t = totals.get(StoredValueKind.valueOf(rs.getString("kind")));
                    BigDecimal amount = rs.getBigDecimal("amount");
                    if (rs.getBoolean("before")) {
                        t[0] = t[0].add(amount);
                    } else {
                        switch (EntryKind.valueOf(rs.getString("entry_kind"))) {
                            case STORED_VALUE_SALE -> t[1] = t[1].add(amount);
                            case STORED_VALUE_EXPIRY -> t[3] = t[3].add(amount.negate());
                            default -> t[2] = t[2].add(amount.negate());
                        }
                    }
                });
        List<StoredValueMovement> movements = new ArrayList<>();
        totals.forEach((kind, t) -> {
            BigDecimal closing = t[0].add(t[1]).subtract(t[2]).subtract(t[3]);
            if (t[0].signum() != 0 || t[1].signum() != 0 || closing.signum() != 0) {
                movements.add(new StoredValueMovement(kind, t[0], t[1], t[2], t[3], closing));
            }
        });
        return movements;
    }

    /** Money each processor has taken for the studio but not yet paid out. */
    @Transactional(readOnly = true)
    public List<ClearingBalance> clearing() {
        requireStaff();
        return jdbc.sql("""
                select provider, sum(amount) as awaiting from journal_line
                where account = 'PROCESSOR_CLEARING' group by provider having sum(amount) <> 0 order by provider
                """)
                .query((rs, row) -> new ClearingBalance(Provider.valueOf(rs.getString("provider")), rs.getBigDecimal("awaiting")))
                .list();
    }

    @Transactional(readOnly = true)
    public List<Payout> payouts() {
        requireStaff();
        return jdbc.sql("""
                select id, provider, external_id, arrival_on, gross, refunds, fees, adjustments, net, currency
                from payout order by arrival_on desc, imported_at desc limit 500
                """)
                .query((rs, row) -> new Payout(rs.getObject("id", UUID.class), Provider.valueOf(rs.getString("provider")),
                        rs.getString("external_id"), rs.getObject("arrival_on", LocalDate.class), rs.getBigDecimal("gross"),
                        rs.getBigDecimal("refunds"), rs.getBigDecimal("fees"), rs.getBigDecimal("adjustments"),
                        rs.getBigDecimal("net"), rs.getString("currency")))
                .list();
    }

    /**
     * Totals per day and account (and tax category, for revenue and tax) over a range. This is what
     * the CSV export and the accounting sync post: summaries, not every transaction, because
     * per-transaction sync is what QuickBooks users complain about and what its API meters.
     */
    @Transactional(readOnly = true)
    public List<SummaryLine> dailySummary(LocalDate from, LocalDate to) {
        requireStaff();
        requireDateRange(from, to);
        return jdbc.sql("""
                select e.occurred_on, l.account, l.tax_category,
                       coalesce(sum(l.amount) filter (where l.amount > 0), 0) as debits,
                       coalesce(-sum(l.amount) filter (where l.amount < 0), 0) as credits
                from journal_line l join journal_entry e on e.id = l.entry_id
                where e.occurred_on between :from and :to
                group by e.occurred_on, l.account, l.tax_category
                order by e.occurred_on, l.account, l.tax_category nulls first
                """)
                .param("from", from)
                .param("to", to)
                .query((rs, row) -> new SummaryLine(rs.getObject("occurred_on", LocalDate.class),
                        LedgerAccount.valueOf(rs.getString("account")), rs.getString("tax_category"),
                        rs.getBigDecimal("debits"), rs.getBigDecimal("credits")))
                .list();
    }

    // ---- Tax rates

    @Transactional(readOnly = true)
    public List<TaxRate> taxRates() {
        return jdbc.sql("select id, tax_category, rate, effective_from from tax_rate order by tax_category, effective_from")
                .query((rs, row) -> new TaxRate(rs.getObject("id", UUID.class), rs.getString("tax_category"),
                        rs.getBigDecimal("rate"), rs.getObject("effective_from", LocalDate.class)))
                .list();
    }

    /**
     * Sets the rate for a tax category from a date onwards. Rules change by date: Washington began
     * taxing group pottery classes on 2025-10-01, then exempted nonprofits' from 2026-07-01.
     */
    @Transactional
    public TaxRate setTaxRate(String taxCategory, BigDecimal rate, LocalDate effectiveFrom) {
        TenantContext caller = requireStaff();
        String category = taxCategory == null ? "" : taxCategory.strip();
        if (category.isEmpty() || category.length() > 40) {
            throw new InvalidInputException("Name the tax category in 1 to 40 characters.");
        }
        if (rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) >= 0) {
            throw new InvalidInputException("A rate is a fraction from 0 up to 1 (0.0825 is 8.25%).");
        }
        jdbc.sql("""
                insert into tax_rate (id, studio_id, tax_category, rate, effective_from)
                values (:id, :studioId, :category, :rate, :from)
                on conflict (studio_id, tax_category, effective_from) do update set rate = excluded.rate
                """)
                .param("id", Uuids.v7())
                .param("studioId", caller.studioId())
                .param("category", category)
                .param("rate", rate)
                .param("from", effectiveFrom)
                .update();
        return taxRates().stream()
                .filter(t -> t.taxCategory().equals(category) && t.effectiveFrom().equals(effectiveFrom))
                .findFirst().orElseThrow();
    }

    // ---- Internals

    /** Writes a balanced entry once per idempotency key; returns its id either way. */
    UUID post(EntryKind kind, LocalDate occurredOn, String memo, @Nullable UUID sourceId, String idempotencyKey,
            List<Line> lines) {
        TenantContext caller = TenantContext.require();
        BigDecimal total = lines.stream().map(Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() != 0 || lines.isEmpty()) {
            throw new IllegalArgumentException("An entry's lines must sum to zero, not " + total);
        }
        UUID id = jdbc.sql("""
                insert into journal_entry (id, studio_id, occurred_on, kind, memo, source_id, idempotency_key,
                                           created_by, created_at)
                values (:id, :studioId, :on, :kind, :memo, :source, :key, :by, :now)
                on conflict (studio_id, idempotency_key) do nothing
                returning id
                """)
                .param("id", Uuids.v7())
                .param("studioId", caller.studioId())
                .param("on", occurredOn)
                .param("kind", kind.name())
                .param("memo", memo)
                .param("source", sourceId)
                .param("key", idempotencyKey)
                .param("by", caller.accountId())
                .param("now", utc(now()))
                .query(UUID.class).optional().orElse(null);
        if (id == null) {
            return jdbc.sql("select id from journal_entry where idempotency_key = :key")
                    .param("key", idempotencyKey).query(UUID.class).single();
        }
        for (Line line : lines) {
            jdbc.sql("""
                    insert into journal_line (id, entry_id, studio_id, account, amount, member_id, stored_value, provider, tax_category)
                    values (:id, :entry, :studioId, :account, :amount, :member, :sv, :provider, :tax)
                    """)
                    .param("id", Uuids.v7())
                    .param("entry", id)
                    .param("studioId", caller.studioId())
                    .param("account", line.account().name())
                    .param("amount", line.amount())
                    .param("member", line.member())
                    .param("sv", line.storedValue())
                    .param("provider", line.provider() == null ? null : line.provider().name())
                    .param("tax", line.taxCategory())
                    .update();
        }
        return id;
    }

    private BigDecimal taxOn(BigDecimal amount, String taxCategory, LocalDate day, Currency currency) {
        BigDecimal rate = jdbc.sql("""
                select rate from tax_rate where tax_category = :category and effective_from <= :day
                order by effective_from desc limit 1
                """)
                .param("category", taxCategory)
                .param("day", day)
                .query(BigDecimal.class).optional().orElse(BigDecimal.ZERO);
        return amount.multiply(rate).setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
    }

    private MemberBalance balanceOf(UUID memberId, TenantContext caller) {
        if (!memberId.equals(caller.accountId()) && !isStaff(caller)) {
            throw new AccessDeniedException("You can see only your own balance.");
        }
        BigDecimal owed = jdbc.sql("""
                select coalesce(sum(amount), 0) from journal_line where account = 'MEMBER_RECEIVABLE' and member_id = :id
                """)
                .param("id", memberId).query(BigDecimal.class).single();
        return new MemberBalance(memberId, accounts.displayNames(List.of(memberId)).get(memberId), owed);
    }

    private StoredValueBalance storedValue(UUID id) {
        return storedValueQuery("where s.id = :id", Map.of("id", id)).stream().findFirst()
                .orElseThrow(() -> new NotFoundException("No such balance."));
    }

    private List<StoredValueBalance> storedValueQuery(String where, Map<String, Object> params) {
        var spec = jdbc.sql("""
                select s.id, s.kind, s.holder, s.code, s.label, s.expires_on,
                       coalesce(-(select sum(l.amount) from journal_line l where l.stored_value = s.id), 0) as remaining
                from stored_value s %s order by s.created_at desc limit 500
                """.formatted(where));
        for (var param : params.entrySet()) {
            spec = spec.param(param.getKey(), param.getValue());
        }
        List<StoredValueBalance> raw = spec.query((rs, row) -> new StoredValueBalance(rs.getObject("id", UUID.class),
                StoredValueKind.valueOf(rs.getString("kind")), rs.getObject("holder", UUID.class), null,
                rs.getString("code"), rs.getString("label"), rs.getObject("expires_on", LocalDate.class),
                rs.getBigDecimal("remaining"))).list();
        Map<UUID, String> names = accounts.displayNames(raw.stream().map(StoredValueBalance::holder)
                .filter(Objects::nonNull).toList());
        return raw.stream()
                .map(s -> new StoredValueBalance(s.id(), s.kind(), s.holder(), s.holder() == null ? null : names.get(s.holder()),
                        s.code(), s.label(), s.expiresOn(), s.remaining()))
                .toList();
    }

    private static void addIfNonZero(List<Line> lines, Line line) {
        if (line.amount().signum() != 0) {
            lines.add(line);
        }
    }

    private void requireMember(UUID accountId) {
        if (members.roleOf(accountId).isEmpty()) {
            throw new InvalidInputException("That person isn't a member of this studio.");
        }
    }

    private Currency currency() {
        return studios.current().currency();
    }

    private LocalDate today() {
        return localDate(Instant.now());
    }

    private LocalDate localDate(Instant instant) {
        ZoneId zone = studios.current().timeZone();
        return instant.atZone(zone).toLocalDate();
    }

    private void requirePositive(BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new InvalidInputException("The amount must be more than zero.");
        }
        if (amount.stripTrailingZeros().scale() > currency().getDefaultFractionDigits()) {
            throw new InvalidInputException("Too many decimal places for " + currency().getCurrencyCode() + ".");
        }
    }

    private static void requireDateRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 366) {
            throw new InvalidInputException("Ask for a range of up to a year.");
        }
    }

    private static String describe(StoredValueKind kind) {
        return switch (kind) {
            case GIFT_CARD -> "Gift card";
            case FIRING_CARD -> "Firing card";
            case CLASS_PACK -> "Class pack";
            case DEPOSIT -> "Deposit";
            case CREDIT -> "Credit";
        };
    }

    private static String newCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            if (i > 0 && i % 4 == 0) {
                code.append('-');
            }
            code.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return code.toString();
    }

    private static String truncate(String text) {
        return truncate(text, 200);
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }

    private static TenantContext requireStaff() {
        TenantContext caller = TenantContext.require();
        if (!isStaff(caller)) {
            throw new AccessDeniedException("Only staff can do this.");
        }
        return caller;
    }

    private static boolean isStaff(TenantContext caller) {
        return caller.role() != null && caller.role().includes(StudioRole.STAFF);
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

}
