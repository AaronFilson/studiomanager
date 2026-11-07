package io.github.aaronfilson.studiomanager.ledger;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.ClearingBalance;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.EntryKind;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Line;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Payout;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Provider;
import io.github.aaronfilson.studiomanager.platform.Capped;
import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.UniqueViolations;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioPermissions;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Processors' payouts into the studio's bank, and what each processor still holds. */
@Service
class Payouts {

  /** The most payouts {@link #payouts} returns. */
  static final int PAYOUTS_LIMIT = 1000;

  private final PayoutRepository payouts;
  private final LedgerQueries queries;
  private final Journal journal;
  private final StudioBooks books;
  private final StudioPermissions permissions;

  Payouts(
      PayoutRepository payouts,
      LedgerQueries queries,
      Journal journal,
      StudioBooks books,
      StudioPermissions permissions) {
    this.payouts = payouts;
    this.queries = queries;
    this.journal = journal;
    this.books = books;
    this.permissions = permissions;
  }

  /**
   * Records a processor's payout: the deposit is what was taken, less refunds and fees, plus any
   * adjustment. Importing the same payout twice is a no-op. Works for any provider, so a studio
   * taking money through Square, Stripe and a marketplace reconciles all of them in one place.
   */
  @Transactional
  public Payout importPayout(
      Provider provider,
      String externalId,
      LocalDate arrivalOn,
      BigDecimal gross,
      BigDecimal refunds,
      BigDecimal fees,
      BigDecimal adjustments,
      BigDecimal net) {
    permissions.require(Permission.MONEY_PAYOUTS, LedgerService.ONLY_STAFF);
    if (provider == Provider.CASH) {
      throw new InvalidInputException(
          "Cash isn't paid out by a processor; record a bank deposit instead.");
    }
    if (net.compareTo(gross.subtract(refunds).subtract(fees).add(adjustments)) != 0) {
      throw new InvalidInputException(
          "The payout doesn't add up: net must be gross − refunds − fees + adjustments.");
    }
    Optional<PayoutEntity> existing = payouts.findImported(provider, externalId);
    if (existing.isPresent()) {
      return existing.get().toView();
    }
    List<Line> lines = new ArrayList<>();
    addIfNonZero(lines, Line.debit(LedgerAccount.BANK, net));
    addIfNonZero(lines, Line.debit(LedgerAccount.PROCESSOR_FEES, fees).provider(provider));
    addIfNonZero(lines, Line.debit(LedgerAccount.REFUNDS, refunds).provider(provider));
    addIfNonZero(lines, Line.credit(LedgerAccount.PROCESSOR_CLEARING, gross).provider(provider));
    addIfNonZero(lines, Line.credit(LedgerAccount.REVENUE_OTHER, adjustments).provider(provider));
    UUID entryId =
        journal.post(
            EntryKind.PAYOUT,
            arrivalOn,
            LedgerService.truncate("%s payout %s".formatted(provider, externalId)),
            null,
            "payout:%s:%s".formatted(provider, externalId),
            lines);
    PayoutEntity payout =
        new PayoutEntity(
            provider,
            externalId,
            arrivalOn,
            gross,
            refunds,
            fees,
            adjustments,
            net,
            books.currency().getCurrencyCode(),
            entryId);
    try {
      payouts.saveAndFlush(payout);
    } catch (DataIntegrityViolationException e) {
      if (UniqueViolations.isUniqueViolation(e)) {
        throw ConflictException.duplicate("That payout was just imported by someone else.");
      }
      throw e;
    }
    return payout.toView();
  }

  @Transactional(readOnly = true)
  public Capped<Payout> payouts() {
    permissions.require(Permission.MONEY_PAYOUTS, LedgerService.ONLY_STAFF);
    return Capped.fromFetched(
            payouts.findNewestFirst(Limit.of(Capped.fetchSize(PAYOUTS_LIMIT))), PAYOUTS_LIMIT)
        .map(PayoutEntity::toView);
  }

  /** Money each processor has taken for the studio but not yet paid out. */
  @Transactional(readOnly = true)
  public List<ClearingBalance> clearing() {
    permissions.require(Permission.MONEY_PAYOUTS, LedgerService.ONLY_STAFF);
    return queries.clearing();
  }

  private static void addIfNonZero(List<Line> lines, Line line) {
    if (line.amount().signum() != 0) {
      lines.add(line);
    }
  }
}
