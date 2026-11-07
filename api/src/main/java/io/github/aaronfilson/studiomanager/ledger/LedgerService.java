package io.github.aaronfilson.studiomanager.ledger;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.EntryKind;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Line;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.MemberBalance;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.PaymentChannel;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.SummaryLine;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioPermissions;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The studio's books: what members owe and pay, and the daily summary the export and accounting
 * sync post. Every money event is one balanced journal entry ({@link Journal}); every balance is a
 * sum ({@link LedgerQueries}).
 *
 * <p>Stored value is in {@link StoredValues}, processor payouts in {@link Payouts}, tax rates in
 * {@link TaxRates}, and the charges other modules raise in {@link MemberCharges}.
 */
@Service
public class LedgerService {

  private final Journal journal;
  private final LedgerQueries queries;
  private final StudioBooks books;
  private final StudioMembers members;
  private final AccountDirectory accounts;
  private final StudioPermissions permissions;

  LedgerService(
      Journal journal,
      LedgerQueries queries,
      StudioBooks books,
      StudioMembers members,
      AccountDirectory accounts,
      StudioPermissions permissions) {
    this.permissions = permissions;
    this.journal = journal;
    this.queries = queries;
    this.books = books;
    this.members = members;
    this.accounts = accounts;
  }

  /** A member pays some or all of what they owe, at the desk or online. */
  @Transactional
  public MemberBalance recordPayment(
      UUID memberId, BigDecimal amount, PaymentChannel provider, @Nullable String reference) {
    TenantContext caller = permissions.require(Permission.MONEY_TAKE_PAYMENT, ONLY_STAFF);
    requireMember(memberId);
    requirePositive(amount);
    String memo =
        "Payment" + (reference == null || reference.isBlank() ? "" : " " + reference.strip());
    journal.post(
        EntryKind.PAYMENT,
        books.today(),
        truncate(memo),
        null,
        "payment:" + Uuids.v7(),
        List.of(
            Line.debit(LedgerAccount.PROCESSOR_CLEARING, amount).provider(provider),
            Line.credit(LedgerAccount.MEMBER_RECEIVABLE, amount).member(memberId)));
    return balanceOf(memberId, caller);
  }

  /** Everyone who owes something (or is in credit), largest first. Staff only. */
  @Transactional(readOnly = true)
  public List<MemberBalance> balances() {
    permissions.require(Permission.MONEY_READ, ONLY_STAFF);
    List<MemberBalance> raw = queries.memberBalances();
    Map<UUID, String> names =
        accounts.displayNames(raw.stream().map(MemberBalance::memberId).toList());
    return raw.stream()
        .map(b -> new MemberBalance(b.memberId(), names.get(b.memberId()), b.owed()))
        .toList();
  }

  /** One member's balance owed. Members can see their own. */
  @Transactional(readOnly = true)
  public MemberBalance balanceOf(UUID memberId) {
    return balanceOf(memberId, TenantContext.require());
  }

  /**
   * Totals per day and account (and tax category, for revenue and tax) over a range. This is what
   * the CSV export and the accounting sync post: summaries, not every transaction, because
   * per-transaction sync is what QuickBooks users complain about and what its API meters.
   */
  @Transactional(readOnly = true)
  public List<SummaryLine> dailySummary(LocalDate from, LocalDate to) {
    permissions.require(Permission.MONEY_READ, ONLY_STAFF);
    requireDateRange(from, to);
    return queries.dailySummary(from, to);
  }

  // ---- For the other ledger services

  void requireMember(UUID accountId) {
    if (members.roleOf(accountId).isEmpty()) {
      throw new InvalidInputException("That person isn't a member of this studio.");
    }
  }

  void requirePositive(BigDecimal amount) {
    if (amount.signum() <= 0) {
      throw new InvalidInputException("The amount must be more than zero.");
    }
    Currency currency = books.currency();
    if (amount.stripTrailingZeros().scale() > currency.getDefaultFractionDigits()) {
      throw new InvalidInputException(
          "Too many decimal places for " + currency.getCurrencyCode() + ".");
    }
  }

  static void requireDateRange(LocalDate from, LocalDate to) {
    if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 366) {
      throw new InvalidInputException("Ask for a range of up to a year.");
    }
  }

  static String truncate(String text) {
    return truncate(text, 200);
  }

  static String truncate(String text, int max) {
    return text.length() <= max ? text : text.substring(0, max);
  }

  /** Why a change only staff may make was refused. */
  static final String ONLY_STAFF = "Only staff can do this.";

  private MemberBalance balanceOf(UUID memberId, TenantContext caller) {
    if (!memberId.equals(caller.accountId()) && !caller.holds(Permission.MONEY_READ)) {
      throw new AccessDeniedException("You can see only your own balance.");
    }
    return new MemberBalance(
        memberId, accounts.displayNames(List.of(memberId)).get(memberId), queries.owedBy(memberId));
  }
}
