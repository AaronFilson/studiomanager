package io.github.aaronfilson.studiomanager.ledger;

import static io.github.aaronfilson.studiomanager.ledger.PayablesChecks.requireMoney;
import static io.github.aaronfilson.studiomanager.ledger.PayablesChecks.requireStaff;
import static io.github.aaronfilson.studiomanager.ledger.PayablesChecks.truncate;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.EntryKind;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Line;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Provider;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.ConsignmentItem;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.ConsignmentSale;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.MyConsignment;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.Payee;
import io.github.aaronfilson.studiomanager.platform.Capped;
import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consigned work on the shelf: listing it, selling it, refunding a sale, giving it back. */
@Service
class Consignments {

  /** The most items {@link #consignmentItems} returns. */
  static final int CONSIGNMENT_ITEMS_LIMIT = 2000;

  /** The most sales {@link #consignmentSales} returns. */
  static final int CONSIGNMENT_SALES_LIMIT = 1000;

  private final ConsignmentItemRepository items;
  private final ConsignmentSaleRepository sales;
  private final PayeeRepository payeeRepository;
  private final PayablesService payees;
  private final Journal journal;
  private final TaxRates taxRates;
  private final StudioBooks books;

  Consignments(
      ConsignmentItemRepository items,
      ConsignmentSaleRepository sales,
      PayeeRepository payeeRepository,
      PayablesService payees,
      Journal journal,
      TaxRates taxRates,
      StudioBooks books) {
    this.items = items;
    this.sales = sales;
    this.payeeRepository = payeeRepository;
    this.payees = payees;
    this.journal = journal;
    this.taxRates = taxRates;
    this.books = books;
  }

  /**
   * Consigned work, newest first; only what's still on the shelf unless asked for everything.
   * {@code dueBackOnly} lists work kept past its consignment period, oldest first: what should go
   * back to the artist.
   */
  @Transactional(readOnly = true)
  public Capped<ConsignmentItem> consignmentItems(
      @Nullable UUID payeeId, boolean inStockOnly, boolean dueBackOnly) {
    requireStaff();
    return items(payeeId, inStockOnly, dueBackOnly);
  }

  /**
   * A member's own consignment: the payee linked to their account, their work and its sales. Any
   * member may ask; they see only their own.
   */
  @Transactional(readOnly = true)
  public MyConsignment myConsignment() {
    TenantContext caller = TenantContext.require();
    Payee payee =
        payees.view(
            payeeRepository
                .findByAccount(caller.accountId())
                .orElseThrow(() ->
                    new NotFoundException("You don't have work on consignment here.")));
    return new MyConsignment(
        payee.id(),
        payee.name(),
        payee.owed(),
        items(payee.id(), false, false).items(),
        sales(payee.id()).items());
  }

  /**
   * Sells consigned work. {@code price} is what the buyer paid before tax, after any discount; left
   * out, it is the retail price. The artist's share is worked out by the item's terms: a share of
   * the price paid, or (when the studio bears discounts) a share of the retail price, never more
   * than was paid.
   */
  @Transactional
  public ConsignmentSale sell(
      UUID itemId, int quantity, @Nullable BigDecimal price, Provider provider) {
    requireStaff();
    ConsignmentItemEntity entity = lockItem(itemId);
    LocalDate day = books.today();
    ConsignmentItem item = entity.toView(day);
    if (quantity < 1) {
      throw new InvalidInputException("Sell at least one.");
    }
    if (quantity > item.inStock()) {
      throw new InvalidInputException("Only %d left in stock.".formatted(item.inStock()));
    }
    BigDecimal retail = item.retailPrice().multiply(BigDecimal.valueOf(quantity));
    BigDecimal paid = price == null ? retail : price;
    requireMoney(paid, books.currency());
    BigDecimal consignorAmount =
        PayablesService.consignorAmount(
            item, retail, paid, books.currency().getDefaultFractionDigits());
    BigDecimal tax = taxRates.taxOn(paid, PayablesService.RETAIL, day, books.currency());

    UUID saleId = Uuids.v7();
    UUID entryId =
        journal.post(
            EntryKind.CONSIGNMENT_SALE,
            day,
            truncate("Sold %d × %s (%s)".formatted(quantity, item.description(), item.payeeName())),
            itemId,
            "consignment-sale:" + saleId,
            saleLines(item.payeeId(), paid, consignorAmount, tax, provider));
    ConsignmentSaleEntity sale =
        new ConsignmentSaleEntity(
            saleId, entity, quantity, paid, consignorAmount, tax, provider, day, entryId);
    entity.sell(quantity);
    sales.saveAndFlush(sale);
    return sale.toView();
  }

  /**
   * Refunds a sale in full: the artist's share, the commission and the tax are all reversed, and
   * the work goes back on the shelf. If the artist has already been paid, they owe it back, and it
   * comes off their next sales.
   *
   * <p>Money given back in cash leaves the till now. Money given back through a processor comes out
   * of a later payout, whose import debits {@link LedgerAccount#REFUNDS}; this credits REFUNDS, so
   * the two meet there instead of counting the refund twice.
   */
  @Transactional
  public ConsignmentSale refund(UUID saleId) {
    requireStaff();
    ConsignmentSaleEntity sale =
        sales.findForUpdate(saleId).orElseThrow(() -> new NotFoundException("No such sale."));
    if (sale.refunded()) {
      throw new ConflictException("That sale was already refunded.");
    }
    ConsignmentItemEntity item = sale.item();
    PayeeEntity payee = item.payee();
    List<Line> lines = new ArrayList<>();
    for (Line line :
        saleLines(payee.id(), sale.price(), sale.consignorAmount(), sale.tax(), sale.provider())) {
      if (line.account() == LedgerAccount.PROCESSOR_CLEARING && sale.provider() != Provider.CASH) {
        lines.add(Line.credit(LedgerAccount.REFUNDS, line.amount()).provider(sale.provider()));
      } else {
        lines.add(line.reversed());
      }
    }
    LocalDate day = books.today();
    UUID entryId =
        journal.post(
            EntryKind.CONSIGNMENT_REFUND,
            day,
            truncate(
                "Refunded %d × %s (%s)"
                    .formatted(sale.quantity(), item.description(), payee.name())),
            item.id(),
            "consignment-refund:" + saleId,
            lines);
    sale.refund(day, entryId);
    item.unsell(sale.quantity());
    sales.flush();
    return sale.toView();
  }

  /** Gives unsold work back to the artist. */
  @Transactional
  public ConsignmentItem returnToArtist(UUID itemId, int quantity) {
    requireStaff();
    ConsignmentItemEntity item = lockItem(itemId);
    if (quantity < 1 || quantity > item.inStock()) {
      throw new InvalidInputException("Return from 1 to %d.".formatted(item.inStock()));
    }
    item.giveBack(quantity);
    items.flush();
    return item.toView(books.today());
  }

  /** Sales of a payee's work, or of everyone's, newest first. */
  @Transactional(readOnly = true)
  public Capped<ConsignmentSale> consignmentSales(@Nullable UUID payeeId) {
    requireStaff();
    return sales(payeeId);
  }

  private Capped<ConsignmentItem> items(
      @Nullable UUID payeeId, boolean inStockOnly, boolean dueBackOnly) {
    LocalDate today = books.today();
    Limit limit = Limit.of(Capped.fetchSize(CONSIGNMENT_ITEMS_LIMIT));
    List<ConsignmentItemEntity> found =
        dueBackOnly
            ? items.findDueBack(payeeId, today, limit)
            : items.findNewestFirst(payeeId, inStockOnly, limit);
    return Capped.fromFetched(found, CONSIGNMENT_ITEMS_LIMIT).map(item -> item.toView(today));
  }

  private Capped<ConsignmentSale> sales(@Nullable UUID payeeId) {
    return Capped.fromFetched(
            sales.findNewestFirst(payeeId, Limit.of(Capped.fetchSize(CONSIGNMENT_SALES_LIMIT))),
            CONSIGNMENT_SALES_LIMIT)
        .map(ConsignmentSaleEntity::toView);
  }

  /** Locks the item so two sales at once can't both take the last one. */
  private ConsignmentItemEntity lockItem(UUID itemId) {
    return items
        .findForUpdate(itemId)
        .orElseThrow(() -> new NotFoundException("No such consigned item."));
  }

  private static List<Line> saleLines(
      UUID payeeId,
      BigDecimal paid,
      BigDecimal consignorAmount,
      BigDecimal tax,
      Provider provider) {
    List<Line> lines = new ArrayList<>();
    lines.add(Line.debit(LedgerAccount.PROCESSOR_CLEARING, paid.add(tax)).provider(provider));
    if (consignorAmount.signum() != 0) {
      lines.add(Line.credit(LedgerAccount.PAYEES_PAYABLE, consignorAmount).payee(payeeId));
    }
    BigDecimal commission = paid.subtract(consignorAmount);
    if (commission.signum() != 0) {
      lines.add(
          Line.credit(LedgerAccount.REVENUE_COMMISSIONS, commission)
              .taxCategory(PayablesService.RETAIL));
    }
    if (tax.signum() != 0) {
      lines.add(
          Line.credit(LedgerAccount.SALES_TAX_PAYABLE, tax).taxCategory(PayablesService.RETAIL));
    }
    return lines;
  }
}
