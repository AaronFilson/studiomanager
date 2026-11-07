package io.github.aaronfilson.studiomanager.ledger;

import static io.github.aaronfilson.studiomanager.ledger.PayablesChecks.blankToNull;
import static io.github.aaronfilson.studiomanager.ledger.PayablesChecks.notInFuture;
import static io.github.aaronfilson.studiomanager.ledger.PayablesChecks.requireMoney;
import static io.github.aaronfilson.studiomanager.ledger.PayablesChecks.requireShare;
import static io.github.aaronfilson.studiomanager.ledger.PayablesChecks.requireStaff;

import io.github.aaronfilson.studiomanager.ledger.PayablesModel.ConsignmentItem;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.DiscountsFrom;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.Payee;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.PayeeChanges;
import io.github.aaronfilson.studiomanager.platform.Capped;
import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.UniqueViolations;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What the studio owes the people it pays: consignors for work sold, instructors for their share of
 * a class. Each sale, share and payment is one balanced entry in the studio's journal, so a payee's
 * balance is a sum of {@link LedgerAccount#PAYEES_PAYABLE} lines like every other balance.
 *
 * <p>This class holds payees and taking work in, which other modules call. Selling consigned work
 * is in {@link Consignments}, shares and payments in {@link PayeePayments}, and the statement and
 * the 1099 list in {@link PayablesReports}.
 *
 * <p>The research found the bookkeeping pain here, not in sales sync: a month's shares printed to
 * PDF and re-keyed, nothing recording whether a payment went out, and no W-9 or 1099 status.
 */
@Service
public class PayablesService {

  /** Consigned work is retail: the same tax category as clay and tools sold at the desk. */
  static final String RETAIL = "RETAIL";

  /** The most payees {@link #payees} returns. */
  static final int PAYEES_LIMIT = 1000;

  private final PayeeRepository payees;
  private final ConsignmentItemRepository items;
  private final PayablesQueries queries;
  private final LedgerService ledger;
  private final StudioMembers members;
  private final AccountDirectory accounts;
  private final Clock clock;

  PayablesService(
      PayeeRepository payees,
      ConsignmentItemRepository items,
      PayablesQueries queries,
      LedgerService ledger,
      StudioMembers members,
      AccountDirectory accounts,
      Clock clock) {
    this.payees = payees;
    this.items = items;
    this.queries = queries;
    this.ledger = ledger;
    this.members = members;
    this.accounts = accounts;
    this.clock = clock;
  }

  // ---- Payees

  @Transactional(readOnly = true)
  public Capped<Payee> payees() {
    requireStaff();
    Capped<PayeeEntity> found =
        Capped.fromFetched(
            payees.findInOrder(Limit.of(Capped.fetchSize(PAYEES_LIMIT))), PAYEES_LIMIT);
    return found.withItems(views(found.items()));
  }

  @Transactional(readOnly = true)
  public Payee payee(UUID payeeId) {
    requireStaff();
    return view(find(payeeId));
  }

  @Transactional
  public Payee createPayee(PayeeChanges changes) {
    requireStaff();
    PayeeEntity payee = new PayeeEntity(validate(changes));
    saveTaken(payee);
    return view(payee);
  }

  @Transactional
  public Payee updatePayee(UUID payeeId, PayeeChanges changes) {
    requireStaff();
    PayeeChanges checked = validate(changes);
    PayeeEntity payee = find(payeeId);
    payee.change(checked, clock.instant());
    saveTaken(payee);
    return view(payee);
  }

  // ---- Consignment

  /**
   * Takes in work to sell for an artist. The terms (the artist's share and who bears discounts) are
   * copied from the payee unless given, so changing a payee's terms later doesn't reprice work
   * already on the shelf.
   */
  @Transactional
  public ConsignmentItem takeIn(
      UUID payeeId,
      String description,
      BigDecimal retailPrice,
      int quantity,
      @Nullable BigDecimal consignorShare,
      @Nullable LocalDate receivedOn) {
    requireStaff();
    PayeeEntity payee = requireActive(payeeId);
    String text = description == null ? "" : description.strip();
    if (text.isEmpty() || text.length() > 120) {
      throw new InvalidInputException("Describe the work in 1 to 120 characters.");
    }
    requireMoney(retailPrice, ledger.currency());
    if (quantity < 1 || quantity > 10_000) {
      throw new InvalidInputException("Take in from 1 to 10,000 of it.");
    }
    BigDecimal share = consignorShare != null ? consignorShare : payee.consignorShare();
    if (share == null) {
      throw new InvalidInputException("Set the artist's share, here or on the payee.");
    }
    requireShare(share);
    LocalDate today = ledger.today();
    LocalDate received = receivedOn == null ? today : notInFuture(receivedOn, today);
    Integer days = payee.consignmentDays();
    LocalDate returnBy = days == null ? null : received.plusDays(days);
    ConsignmentItemEntity item =
        new ConsignmentItemEntity(payee, text, retailPrice, share, quantity, received, returnBy);
    items.saveAndFlush(item);
    return item.toView(today);
  }

  // ---- Rules

  /**
   * The 1099-NEC box 1 threshold for payments made in {@code year}: $600 through 2025, $2,000 for
   * 2026 (IRS instructions for Forms 1099-MISC and 1099-NEC). Later years may be indexed for
   * inflation; when the IRS publishes a new figure, add it here.
   */
  static BigDecimal necThreshold(int year) {
    return year < 2026 ? new BigDecimal("600") : new BigDecimal("2000");
  }

  /** The artist's share of a sale, by the item's terms, never more than was paid. */
  static BigDecimal consignorAmount(
      ConsignmentItem item, BigDecimal retail, BigDecimal paid, int digits) {
    BigDecimal base = item.discountsFrom() == DiscountsFrom.STUDIO ? retail : paid;
    return base.multiply(item.consignorShare()).setScale(digits, RoundingMode.HALF_UP).min(paid);
  }

  // ---- For the other payables services

  PayeeEntity find(UUID payeeId) {
    return payees.findById(payeeId).orElseThrow(() -> new NotFoundException("No such payee."));
  }

  PayeeEntity requireActive(UUID payeeId) {
    PayeeEntity payee = find(payeeId);
    if (payee.archived()) {
      throw new InvalidInputException(
          "%s is archived. Restore them first.".formatted(payee.name()));
    }
    return payee;
  }

  /** A payee as staff see it: with their member's name and what they're owed now. */
  Payee view(PayeeEntity payee) {
    return views(List.of(payee)).getFirst();
  }

  /** Several payees, with one query for what they're owed and one for their members' names. */
  List<Payee> views(List<PayeeEntity> found) {
    // The balances are summed in SQL, which can't see entity changes not yet written
    payees.flush();
    Map<UUID, BigDecimal> owed = queries.owed(found.stream().map(PayeeEntity::id).toList());
    Map<UUID, String> names =
        accounts.displayNames(
            found.stream().map(PayeeEntity::accountId).filter(Objects::nonNull).toList());
    return found.stream()
        .map(payee -> {
          UUID accountId = payee.accountId();
          return payee.toView(
              accountId == null ? null : names.get(accountId),
              owed.getOrDefault(payee.id(), BigDecimal.ZERO));
        })
        .toList();
  }

  // ---- Internals

  /**
   * Writes the payee now, inside the try, so a member who is already a payee is reported here
   * rather than at commit.
   */
  private void saveTaken(PayeeEntity payee) {
    try {
      payees.saveAndFlush(payee);
    } catch (DataIntegrityViolationException e) {
      if (UniqueViolations.isUniqueViolation(e)) {
        throw ConflictException.duplicate("That member is already a payee.");
      }
      throw e;
    }
  }

  /** Checks the changes and returns them with their text trimmed, as they are stored. */
  private PayeeChanges validate(PayeeChanges changes) {
    String name = changes.name() == null ? "" : changes.name().strip();
    if (name.isEmpty() || name.length() > 120) {
      throw new InvalidInputException("Name the payee in 1 to 120 characters.");
    }
    String email = blankToNull(changes.email());
    if (email != null && (email.length() > 254 || !email.contains("@"))) {
      throw new InvalidInputException("That email address doesn't look right.");
    }
    UUID accountId = changes.accountId();
    if (accountId != null && members.roleOf(accountId).isEmpty()) {
      throw new InvalidInputException("That person isn't a member of this studio.");
    }
    BigDecimal share = changes.consignorShare();
    if (share != null) {
      requireShare(share);
    }
    LocalDate w9 = changes.w9ReceivedOn();
    if (w9 != null) {
      notInFuture(w9, ledger.today());
    }
    Integer days = changes.consignmentDays();
    if (days != null && (days < 1 || days > 3650)) {
      throw new InvalidInputException("A consignment period is from 1 to 3,650 days.");
    }
    return new PayeeChanges(
        name,
        email,
        accountId,
        changes.files1099(),
        w9,
        share,
        changes.discountsFrom(),
        days,
        changes.archived());
  }
}
