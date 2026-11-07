package io.github.aaronfilson.studiomanager.ledger;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.EntryKind;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Line;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.MemberJoined;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  private final PendingOpeningBalanceRepository held;
  private final Journal journal;
  private final StudioBooks books;
  private final StudioMembers members;
  private final TransactionOperations transactions;

  OpeningBalances(
      PendingOpeningBalanceRepository held,
      Journal journal,
      StudioBooks books,
      StudioMembers members,
      TransactionOperations transactions) {
    this.held = held;
    this.journal = journal;
    this.books = books;
    this.members = members;
    this.transactions = transactions;
  }

  /** Whether this member already has an opening balance. */
  @Transactional(readOnly = true)
  public boolean hasOpeningBalance(UUID memberId) {
    return journal.isPosted(key(memberId));
  }

  /**
   * Checks an amount and date before anything is written: a dry run calls this too. Positive means
   * the member owes the studio; negative means they're in credit.
   */
  public void validate(BigDecimal amount, @Nullable LocalDate asOf) {
    if (amount.signum() == 0) {
      throw new InvalidInputException("An opening balance can't be zero; leave it blank instead.");
    }
    Currency currency = books.currency();
    if (amount.stripTrailingZeros().scale() > currency.getDefaultFractionDigits()) {
      throw new InvalidInputException(
          "Too many decimal places for " + currency.getCurrencyCode() + ".");
    }
    if (amount.abs().compareTo(new BigDecimal("1000000")) >= 0) {
      throw new InvalidInputException("That balance is too large to be right.");
    }
    if (asOf != null && asOf.isAfter(books.today())) {
      throw new InvalidInputException("The balance date hasn't happened yet.");
    }
  }

  /** Posts a member's opening balance now. Returns false if they already had one. */
  @Transactional
  public boolean post(UUID memberId, BigDecimal amount, @Nullable LocalDate asOf) {
    LedgerService.requireStaff();
    validate(amount, asOf);
    if (hasOpeningBalance(memberId)) {
      return false;
    }
    postEntry(memberId, amount, asOf == null ? books.today() : asOf);
    return true;
  }

  /** Holds a balance for someone invited by email, to post when they join. */
  @Transactional
  public void holdForInvite(UUID inviteId, BigDecimal amount, @Nullable LocalDate asOf) {
    LedgerService.requireStaff();
    validate(amount, asOf);
    held.saveAndFlush(
        new PendingOpeningBalance(inviteId, amount, asOf == null ? books.today() : asOf));
  }

  /**
   * Posts a held balance once its invitee joins. Binds the studio before starting a transaction, as
   * row-level security needs (see InviteEmails); the outbox retries it if it fails.
   */
  @Async
  @TransactionalEventListener
  void on(MemberJoined joined) {
    TenantContext member =
        TenantContext.forAccount(joined.accountId()).inStudio(joined.studioId(), StudioRole.MEMBER);
    TenantContext.callAs(
        member,
        () -> {
          transactions.executeWithoutResult(tx -> postHeld(joined.accountId()));
          return joined;
        });
  }

  private void postHeld(UUID accountId) {
    List<UUID> invites = members.invitesAcceptedBy(accountId);
    if (invites.isEmpty()) {
      return;
    }
    for (PendingOpeningBalance balance : held.findUnpostedForUpdate(invites)) {
      // Two invites with balances for one person: the first wins, as with a re-import
      balance.posted(postEntry(accountId, balance.amount(), balance.asOf()));
      log.debug("Posted the opening balance held for {}", accountId);
    }
    held.flush();
  }

  private UUID postEntry(UUID memberId, BigDecimal amount, LocalDate day) {
    return journal.post(
        EntryKind.OPENING_BALANCE,
        day,
        "Opening balance",
        memberId,
        key(memberId),
        List.of(
            Line.debit(LedgerAccount.MEMBER_RECEIVABLE, amount).member(memberId),
            Line.credit(LedgerAccount.OPENING_BALANCE_EQUITY, amount)));
  }

  private static String key(UUID memberId) {
    return "opening-balance:" + memberId;
  }
}
