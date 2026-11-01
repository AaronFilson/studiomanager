package io.github.aaronfilson.studiomanager.ledger;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.EntryKind;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Line;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.MemberJoined;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Balances members carried in from before the studio used this app. Each member has at most one:
 * its idempotency key is the member, so importing the same spreadsheet twice posts once.
 *
 * <p>Someone imported by email has no account yet, so their balance is held against their invite
 * and posted when they accept it.
 */
@Service
public class OpeningBalances {

    private static final Logger log = LoggerFactory.getLogger(OpeningBalances.class);

    private final JdbcClient jdbc;
    private final LedgerService ledger;
    private final TransactionOperations transactions;

    OpeningBalances(JdbcClient jdbc, LedgerService ledger, TransactionOperations transactions) {
        this.jdbc = jdbc;
        this.ledger = ledger;
        this.transactions = transactions;
    }

    /** Whether this member already has an opening balance. */
    @Transactional(readOnly = true)
    public boolean hasOpeningBalance(UUID memberId) {
        return jdbc.sql("select exists (select 1 from journal_entry where idempotency_key = :key)")
                .param("key", key(memberId)).query(Boolean.class).single();
    }

    /**
     * Checks an amount and date before anything is written: a dry run calls this too. Positive
     * means the member owes the studio; negative means they're in credit.
     */
    public void validate(BigDecimal amount, @Nullable LocalDate asOf) {
        if (amount.signum() == 0) {
            throw new InvalidInputException("An opening balance can't be zero; leave it blank instead.");
        }
        if (amount.stripTrailingZeros().scale() > ledger.currency().getDefaultFractionDigits()) {
            throw new InvalidInputException("Too many decimal places for " + ledger.currency().getCurrencyCode() + ".");
        }
        if (amount.abs().compareTo(new BigDecimal("1000000")) >= 0) {
            throw new InvalidInputException("That balance is too large to be right.");
        }
        if (asOf != null && asOf.isAfter(ledger.today())) {
            throw new InvalidInputException("The balance date hasn't happened yet.");
        }
    }

    /** Posts a member's opening balance now. Returns false if they already had one. */
    @Transactional
    public boolean post(UUID memberId, BigDecimal amount, @Nullable LocalDate asOf) {
        requireStaff();
        validate(amount, asOf);
        if (hasOpeningBalance(memberId)) {
            return false;
        }
        postEntry(memberId, amount, asOf == null ? ledger.today() : asOf);
        return true;
    }

    /** Holds a balance for someone invited by email, to post when they join. */
    @Transactional
    public void holdForInvite(UUID inviteId, BigDecimal amount, @Nullable LocalDate asOf) {
        TenantContext caller = requireStaff();
        validate(amount, asOf);
        jdbc.sql("""
                insert into pending_opening_balance (id, studio_id, invite_id, amount, as_of, created_by, created_at)
                values (:id, :studioId, :invite, :amount, :asOf, :by, :now)
                """)
                .param("id", Uuids.v7())
                .param("studioId", caller.studioId())
                .param("invite", inviteId)
                .param("amount", amount)
                .param("asOf", asOf == null ? ledger.today() : asOf)
                .param("by", caller.accountId())
                .param("now", Instant.now().truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC))
                .update();
    }

    /**
     * Posts a held balance once its invitee joins. Binds the studio before starting a transaction,
     * as row-level security needs (see InviteEmails); the outbox retries it if it fails.
     */
    @Async
    @TransactionalEventListener
    void on(MemberJoined joined) {
        TenantContext member = TenantContext.forAccount(joined.accountId()).inStudio(joined.studioId(), StudioRole.MEMBER);
        TenantContext.callAs(member, () -> transactions.execute(tx -> {
            List<Held> held = jdbc.sql("""
                    select b.id, b.amount, b.as_of from pending_opening_balance b
                    join studio_invite i on i.id = b.invite_id
                    where i.accepted_by = :account and b.entry_id is null
                    order by b.created_at
                    for update of b
                    """)
                    .param("account", joined.accountId())
                    .query(Held.class).list();
            for (Held balance : held) {
                // Two invites with balances for one person: the first wins, as with a re-import
                UUID entryId = postEntry(joined.accountId(), balance.amount(), balance.asOf());
                jdbc.sql("update pending_opening_balance set entry_id = :entry where id = :id")
                        .param("entry", entryId).param("id", balance.id()).update();
                log.debug("Posted the opening balance held for {}", joined.accountId());
            }
            return null;
        }));
    }

    private UUID postEntry(UUID memberId, BigDecimal amount, LocalDate day) {
        return ledger.post(EntryKind.OPENING_BALANCE, day, "Opening balance", memberId, key(memberId), List.of(
                Line.debit(LedgerAccount.MEMBER_RECEIVABLE, amount).member(memberId),
                Line.credit(LedgerAccount.OPENING_BALANCE_EQUITY, amount)));
    }

    private static String key(UUID memberId) {
        return "opening-balance:" + memberId;
    }

    private static TenantContext requireStaff() {
        TenantContext caller = TenantContext.require();
        if (caller.role() == null || !caller.role().includes(StudioRole.STAFF)) {
            throw new org.springframework.security.access.AccessDeniedException("Only staff can do this.");
        }
        return caller;
    }

    record Held(UUID id, BigDecimal amount, LocalDate asOf) {
    }

}
