package io.github.aaronfilson.studiomanager.ledger;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.EntryKind;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Line;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.PaymentChannel;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.StoredValueBalance;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.StoredValueKind;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.StoredValueMovement;
import io.github.aaronfilson.studiomanager.ledger.LedgerQueries.StoredValueMovementRow;
import io.github.aaronfilson.studiomanager.platform.Capped;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioPermissions;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stored value: a gift card (with a code anyone can redeem), or a firing card, class pack, deposit
 * or credit held by a member. The money is a liability until used.
 */
@Service
class StoredValues {

  /** The most balances {@link #storedValues} returns. */
  static final int STORED_VALUES_LIMIT = 2000;

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

  private final StoredValueRepository storedValues;
  private final LedgerQueries queries;
  private final Journal journal;
  private final StudioBooks books;
  private final LedgerService ledger;
  private final AccountDirectory accounts;
  private final StudioPermissions permissions;

  StoredValues(
      StoredValueRepository storedValues,
      LedgerQueries queries,
      Journal journal,
      StudioBooks books,
      LedgerService ledger,
      AccountDirectory accounts,
      StudioPermissions permissions) {
    this.permissions = permissions;
    this.storedValues = storedValues;
    this.queries = queries;
    this.journal = journal;
    this.books = books;
    this.ledger = ledger;
    this.accounts = accounts;
  }

  /** Sells stored value. Only a gift card can be sold without naming who holds it. */
  @Transactional
  public StoredValueBalance sellStoredValue(
      StoredValueKind kind,
      @Nullable UUID holder,
      @Nullable String label,
      BigDecimal amount,
      PaymentChannel provider,
      @Nullable LocalDate expiresOn) {
    permissions.require(Permission.MONEY_STORED_VALUE_MANAGE, LedgerService.ONLY_STAFF);
    ledger.requirePositive(amount);
    if (kind != StoredValueKind.GIFT_CARD && holder == null) {
      throw new InvalidInputException("Only a gift card can be sold without naming who holds it.");
    }
    if (holder != null) {
      ledger.requireMember(holder);
    }
    // US Regulation E: gift cards can't expire within five years of sale
    if (kind == StoredValueKind.GIFT_CARD
        && expiresOn != null
        && expiresOn.isBefore(books.today().plusYears(5))
        && "US".equals(books.country())) {
      throw new InvalidInputException(
          "In the US a gift card can't expire within five years of being sold.");
    }
    String code = kind == StoredValueKind.GIFT_CARD ? newCode() : null;
    String cleanLabel =
        label == null || label.isBlank() ? null : LedgerService.truncate(label.strip(), 80);
    StoredValue sold = new StoredValue(kind, holder, code, cleanLabel, expiresOn);
    // Written before the entry, whose lines refer to it
    storedValues.saveAndFlush(sold);
    UUID id = sold.id();
    journal.post(
        EntryKind.STORED_VALUE_SALE,
        books.today(),
        LedgerService.truncate(describe(kind) + " sold"),
        id,
        "sv-sale:" + id,
        List.of(
            Line.debit(LedgerAccount.PROCESSOR_CLEARING, amount).provider(provider),
            Line.credit(LedgerAccount.STORED_VALUE_LIABILITY, amount).storedValue(id)));
    return storedValue(id);
  }

  /** Uses stored value to pay down what a member owes. */
  @Transactional
  public StoredValueBalance useStoredValue(UUID storedValueId, UUID memberId, BigDecimal amount) {
    permissions.require(Permission.MONEY_STORED_VALUE_MANAGE, LedgerService.ONLY_STAFF);
    ledger.requireMember(memberId);
    ledger.requirePositive(amount);
    // Lock the balance so two uses at once can't both spend it. Only the lock is wanted.
    var _ =
        storedValues
            .findForUpdate(storedValueId)
            .orElseThrow(() -> new NotFoundException("No such balance."));
    StoredValueBalance balance = storedValue(storedValueId);
    UUID holder = balance.holder();
    if (holder != null && !holder.equals(memberId)) {
      throw new InvalidInputException("That balance belongs to someone else.");
    }
    LocalDate expiresOn = balance.expiresOn();
    LocalDate today = books.today();
    if (expiresOn != null && expiresOn.isBefore(today)) {
      throw new InvalidInputException("That balance has expired.");
    }
    if (balance.remaining().compareTo(amount) < 0) {
      throw new InvalidInputException(
          "Only %s is left on it.".formatted(balance.remaining().toPlainString()));
    }
    journal.post(
        EntryKind.STORED_VALUE_USE,
        today,
        LedgerService.truncate(describe(balance.kind()) + " used"),
        storedValueId,
        "sv-use:" + Uuids.v7(),
        List.of(
            Line.debit(LedgerAccount.STORED_VALUE_LIABILITY, amount).storedValue(storedValueId),
            Line.credit(LedgerAccount.MEMBER_RECEIVABLE, amount).member(memberId)));
    return storedValue(storedValueId);
  }

  /** Writes off what's left on a lapsed balance as breakage revenue. */
  @Transactional
  public StoredValueBalance expireStoredValue(UUID storedValueId) {
    permissions.require(Permission.MONEY_STORED_VALUE_MANAGE, LedgerService.ONLY_STAFF);
    StoredValueBalance balance = storedValue(storedValueId);
    if (balance.remaining().signum() <= 0) {
      throw new InvalidInputException("Nothing is left on it.");
    }
    journal.post(
        EntryKind.STORED_VALUE_EXPIRY,
        books.today(),
        LedgerService.truncate(describe(balance.kind()) + " expired"),
        storedValueId,
        "sv-expiry:" + storedValueId,
        List.of(
            Line.debit(LedgerAccount.STORED_VALUE_LIABILITY, balance.remaining())
                .storedValue(storedValueId),
            Line.credit(LedgerAccount.REVENUE_BREAKAGE, balance.remaining())));
    return storedValue(storedValueId);
  }

  /** One member's stored-value balances. Members can see their own. */
  @Transactional(readOnly = true)
  public List<StoredValueBalance> storedValuesOf(UUID memberId) {
    TenantContext caller = TenantContext.require();
    if (!memberId.equals(caller.accountId()) && !caller.holds(Permission.MONEY_READ)) {
      throw new AccessDeniedException("You can see only your own balances.");
    }
    return withNames(
        queries.storedValuesHeldBy(memberId, Capped.fetchSize(STORED_VALUES_LIMIT)).stream()
            .limit(STORED_VALUES_LIMIT)
            .toList());
  }

  /**
   * Every stored-value balance with something left, newest first, at most {@value
   * #STORED_VALUES_LIMIT}. Staff only.
   */
  @Transactional(readOnly = true)
  public Capped<StoredValueBalance> storedValues() {
    permissions.require(Permission.MONEY_STORED_VALUE_MANAGE, LedgerService.ONLY_STAFF);
    Capped<StoredValueBalance> found =
        Capped.fromFetched(
            queries.storedValuesWithSomethingLeft(Capped.fetchSize(STORED_VALUES_LIMIT)),
            STORED_VALUES_LIMIT);
    return found.withItems(withNames(found.items()));
  }

  /** Looks up a gift card by its code, for redeeming it at the desk. */
  @Transactional(readOnly = true)
  public StoredValueBalance giftCard(String code) {
    permissions.require(Permission.MONEY_STORED_VALUE_MANAGE, LedgerService.ONLY_STAFF);
    return withNames(queries.giftCardByCode(code.strip().toUpperCase(Locale.ROOT))).stream()
        .findFirst()
        .orElseThrow(() -> new NotFoundException("No gift card with that code."));
  }

  /**
   * Stored value by kind over [{@code from}, {@code to}]: opening, sold, used, expired, closing.
   * The roll-forward a bookkeeper asks for to support the liability on the balance sheet.
   */
  @Transactional(readOnly = true)
  public List<StoredValueMovement> storedValueMovements(LocalDate from, LocalDate to) {
    permissions.require(Permission.MONEY_READ, LedgerService.ONLY_STAFF);
    LedgerService.requireDateRange(from, to);
    Map<StoredValueKind, Totals> totals = new EnumMap<>(StoredValueKind.class);
    for (StoredValueKind kind : StoredValueKind.values()) {
      totals.put(kind, new Totals());
    }
    for (StoredValueMovementRow row : queries.storedValueMovements(from, to)) {
      Totals kindTotals = Objects.requireNonNull(totals.get(row.kind()));
      if (row.beforeRange()) {
        kindTotals.opening = kindTotals.opening.add(row.amount());
      } else {
        kindTotals.add(row.entryKind(), row.amount());
      }
    }
    List<StoredValueMovement> movements = new ArrayList<>();
    totals.forEach((kind, kindTotals) -> {
      BigDecimal closing =
          kindTotals
              .opening
              .add(kindTotals.sold)
              .subtract(kindTotals.used)
              .subtract(kindTotals.expired);
      if (kindTotals.opening.signum() != 0
          || kindTotals.sold.signum() != 0
          || closing.signum() != 0) {
        movements.add(
            new StoredValueMovement(
                kind,
                kindTotals.opening,
                kindTotals.sold,
                kindTotals.used,
                kindTotals.expired,
                closing));
      }
    });
    return movements;
  }

  private StoredValueBalance storedValue(UUID id) {
    return withNames(queries.storedValueById(id)).stream()
        .findFirst()
        .orElseThrow(() -> new NotFoundException("No such balance."));
  }

  private List<StoredValueBalance> withNames(List<StoredValueBalance> balances) {
    Map<UUID, String> names =
        accounts.displayNames(
            balances.stream().map(StoredValueBalance::holder).filter(Objects::nonNull).toList());
    return balances.stream()
        .map(s -> {
          UUID holder = s.holder();
          return new StoredValueBalance(
              s.id(),
              s.kind(),
              holder,
              holder == null ? null : names.get(holder),
              s.code(),
              s.label(),
              s.expiresOn(),
              s.remaining());
        })
        .toList();
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

  /** One kind's running totals over a period; credits to the liability as positive. */
  private static final class Totals {
    private BigDecimal opening = BigDecimal.ZERO;
    private BigDecimal sold = BigDecimal.ZERO;
    private BigDecimal used = BigDecimal.ZERO;
    private BigDecimal expired = BigDecimal.ZERO;

    void add(EntryKind entryKind, BigDecimal amount) {
      // Any other kind that moves stored value is money taken off a card or credit
      switch (entryKind) {
        case STORED_VALUE_SALE -> sold = sold.add(amount);
        case STORED_VALUE_EXPIRY -> expired = expired.add(amount.negate());
        case STORED_VALUE_USE -> takenOff(amount);
        case PIECE_FEE -> takenOff(amount);
        case RETAIL_SALE -> takenOff(amount);
        case PAYMENT -> takenOff(amount);
        case PAYOUT -> takenOff(amount);
        case REFUND -> takenOff(amount);
        case ADJUSTMENT -> takenOff(amount);
        case CONSIGNMENT_SALE -> takenOff(amount);
        case CONSIGNMENT_REFUND -> takenOff(amount);
        case PAYEE_EARNING -> takenOff(amount);
        case PAYEE_PAYMENT -> takenOff(amount);
        case OPENING_BALANCE -> takenOff(amount);
      }
    }

    private void takenOff(BigDecimal amount) {
      used = used.add(amount.negate());
    }
  }
}
