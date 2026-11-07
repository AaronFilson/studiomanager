package io.github.aaronfilson.studiomanager.ledger;

import static io.github.aaronfilson.studiomanager.ledger.PayablesChecks.blankToNull;
import static io.github.aaronfilson.studiomanager.ledger.PayablesChecks.notInFuture;
import static io.github.aaronfilson.studiomanager.ledger.PayablesChecks.requireMoney;
import static io.github.aaronfilson.studiomanager.ledger.PayablesChecks.requireScale;
import static io.github.aaronfilson.studiomanager.ledger.PayablesChecks.requireShare;
import static io.github.aaronfilson.studiomanager.ledger.PayablesChecks.truncate;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.EntryKind;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Line;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Provider;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.Earning;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.EarningKind;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.Payee;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.Payment;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.PaymentMethod;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.PaymentRunLine;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioPermissions;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Shares and fees payees earn, and the payments that settle them. */
@Service
class PayeePayments {

  private final PayeeRepository payeeRepository;
  private final PayeeEarningRepository earnings;
  private final PayeePaymentRepository payments;
  private final PayablesService payees;
  private final Journal journal;
  private final StudioBooks books;
  private final StudioMembers members;
  private final StudioPermissions permissions;

  PayeePayments(
      PayeeRepository payeeRepository,
      PayeeEarningRepository earnings,
      PayeePaymentRepository payments,
      PayablesService payees,
      Journal journal,
      StudioBooks books,
      StudioMembers members,
      StudioPermissions permissions) {
    this.permissions = permissions;
    this.payeeRepository = payeeRepository;
    this.earnings = earnings;
    this.payments = payments;
    this.payees = payees;
    this.journal = journal;
    this.books = books;
    this.members = members;
  }

  /**
   * Records a share or fee a payee has earned, such as an instructor's 70% of a class. Give either
   * the amount, or what the class took and the payee's share of it. Only an adjustment can be
   * negative, to claw back an amount recorded in error.
   */
  @Transactional
  public Earning recordEarning(
      UUID payeeId,
      EarningKind kind,
      String description,
      @Nullable LocalDate earnedOn,
      @Nullable BigDecimal amount,
      @Nullable BigDecimal gross,
      @Nullable BigDecimal share) {
    permissions.require(Permission.MONEY_PAYABLES_MANAGE, LedgerService.ONLY_STAFF);
    payees.requireActive(payeeId);
    String text = description == null ? "" : description.strip();
    if (text.isEmpty() || text.length() > 120) {
      throw new InvalidInputException("Describe what it's for in 1 to 120 characters.");
    }
    int digits = books.currency().getDefaultFractionDigits();
    BigDecimal earned;
    if (gross != null || share != null) {
      if (gross == null || share == null || amount != null) {
        throw new InvalidInputException(
            "Give the amount, or what the class took and the share, not both.");
      }
      requireMoney(gross, books.currency());
      requireShare(share);
      earned = gross.multiply(share).setScale(digits, RoundingMode.HALF_UP);
    } else if (amount != null) {
      requireScale(amount, books.currency());
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
    LocalDate today = books.today();
    LocalDate day = earnedOn == null ? today : notInFuture(earnedOn, today);
    UUID id = Uuids.v7();
    List<Line> lines =
        List.of(
            Line.debit(LedgerAccount.CONTRACT_SERVICES, earned),
            Line.credit(LedgerAccount.PAYEES_PAYABLE, earned).payee(payeeId));
    UUID entryId =
        journal.post(
            EntryKind.PAYEE_EARNING, day, truncate(text), id, "payee-earning:" + id, lines);
    earnings.saveAndFlush(
        new PayeeEarning(id, payeeId, kind, text, day, gross, share, earned, entryId));
    return new Earning(id, payeeId, kind, text, day, gross, share, earned);
  }

  /**
   * Records money sent to a payee, up to what they're owed. Studio credit pays a member-payee by
   * taking it off what they owe the studio, so an artist's sales can cover their firing fees.
   */
  @Transactional
  public Payment pay(
      UUID payeeId,
      BigDecimal amount,
      PaymentMethod method,
      @Nullable String reference,
      @Nullable LocalDate paidOn) {
    permissions.require(Permission.MONEY_PAYABLES_MANAGE, LedgerService.ONLY_STAFF);
    // Lock the payee so two payments at once can't both pay the same balance
    PayeeEntity locked =
        payeeRepository
            .findForUpdate(payeeId)
            .orElseThrow(() -> new NotFoundException("No such payee."));
    Payee payee = payees.view(locked);
    requireMoney(amount, books.currency());
    if (amount.compareTo(payee.owed()) > 0) {
      throw new InvalidInputException(
          "Only %s is owed to %s.".formatted(payee.owed().toPlainString(), payee.name()));
    }
    UUID memberAccount = payee.accountId();
    if (method == PaymentMethod.STUDIO_CREDIT
        && (memberAccount == null || members.roleOf(memberAccount).isEmpty())) {
      throw new InvalidInputException(
          "Only a payee who is a member here can be paid in studio credit.");
    }
    String ref = blankToNull(reference);
    if (ref != null && ref.length() > 80) {
      throw new InvalidInputException("Keep the reference to 80 characters.");
    }
    LocalDate today = books.today();
    LocalDate day = paidOn == null ? today : notInFuture(paidOn, today);
    UUID id = Uuids.v7();
    List<Line> lines =
        List.of(
            Line.debit(LedgerAccount.PAYEES_PAYABLE, amount).payee(payeeId),
            source(method, amount, memberAccount));
    UUID entryId =
        journal.post(
            EntryKind.PAYEE_PAYMENT,
            day,
            truncate(
                "Paid %s, %s%s"
                    .formatted(payee.name(), describe(method), ref == null ? "" : " " + ref)),
            id,
            "payee-payment:" + id,
            lines);
    payments.saveAndFlush(new PayeePayment(id, payeeId, day, amount, method, ref, entryId));
    return new Payment(id, payeeId, day, amount, method, ref);
  }

  /**
   * Pays several payees at once, such as the month's consignors by cheque. All or none: if one line
   * fails (more than is owed, an unknown payee), nothing is recorded and the error names it. Payees
   * are locked in id order, so two runs at once can't deadlock.
   */
  @Transactional
  public List<Payment> payRun(
      List<PaymentRunLine> lines, PaymentMethod method, @Nullable LocalDate paidOn) {
    permissions.require(Permission.MONEY_PAYABLES_MANAGE, LedgerService.ONLY_STAFF);
    if (lines.isEmpty() || lines.size() > 200) {
      throw new InvalidInputException("Pay from 1 to 200 payees at once.");
    }
    if (lines.stream().map(PaymentRunLine::payeeId).distinct().count() != lines.size()) {
      throw new InvalidInputException("Each payee can appear only once in a payment run.");
    }
    List<PaymentRunLine> ordered =
        lines.stream().sorted(Comparator.comparing(PaymentRunLine::payeeId)).toList();
    Map<UUID, Payment> paid = new HashMap<>();
    for (PaymentRunLine line : ordered) {
      paid.put(
          line.payeeId(), pay(line.payeeId(), line.amount(), method, line.reference(), paidOn));
    }
    // Back in the order asked for
    return lines.stream()
        .map(line -> Objects.requireNonNull(paid.get(line.payeeId()), "Every line was paid"))
        .toList();
  }

  /** Where the money for a payment comes from. */
  private static Line source(PaymentMethod method, BigDecimal amount, @Nullable UUID accountId) {
    return switch (method) {
      case CHECK, BANK_TRANSFER -> Line.credit(LedgerAccount.BANK, amount);
      case CASH -> Line.credit(LedgerAccount.PROCESSOR_CLEARING, amount).provider(Provider.CASH);
      case PAYPAL ->
          Line.credit(LedgerAccount.PROCESSOR_CLEARING, amount).provider(Provider.PAYPAL);
      case STRIPE ->
          Line.credit(LedgerAccount.PROCESSOR_CLEARING, amount).provider(Provider.STRIPE);
      case STUDIO_CREDIT ->
          Line.credit(LedgerAccount.MEMBER_RECEIVABLE, amount)
              .member(Objects.requireNonNull(accountId));
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
}
