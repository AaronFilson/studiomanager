package io.github.aaronfilson.studiomanager.ledger;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.EntryKind;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Line;
import io.github.aaronfilson.studiomanager.piece.PieceReceived;
import io.github.aaronfilson.studiomanager.supplies.SupplySold;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * What other modules charge members for, posted to their balance in the same transaction as the
 * charge itself: a piece's firing fee, something sold at the desk.
 */
@Component
class MemberCharges {

  private final Journal journal;
  private final TaxRates taxRates;
  private final StudioBooks books;

  MemberCharges(Journal journal, TaxRates taxRates, StudioBooks books) {
    this.journal = journal;
    this.taxRates = taxRates;
    this.books = books;
  }

  /** A piece's fee becomes what its maker owes, with sales tax at the rate in force that day. */
  @EventListener
  void onPieceReceived(PieceReceived piece) {
    if (piece.fee().amount().signum() == 0) {
      return;
    }
    LocalDate day = books.dateOf(piece.receivedAt());
    BigDecimal fee = piece.fee().amount();
    BigDecimal tax = taxRates.taxOn(fee, piece.taxCategory(), day, piece.fee().currency());
    List<Line> lines = new ArrayList<>();
    lines.add(
        Line.debit(LedgerAccount.MEMBER_RECEIVABLE, fee.add(tax)).member(piece.makerAccountId()));
    lines.add(Line.credit(LedgerAccount.REVENUE_FIRING, fee).taxCategory(piece.taxCategory()));
    if (tax.signum() != 0) {
      lines.add(Line.credit(LedgerAccount.SALES_TAX_PAYABLE, tax).taxCategory(piece.taxCategory()));
    }
    journal.post(
        EntryKind.PIECE_FEE,
        day,
        "Firing fee, piece " + piece.number(),
        piece.pieceId(),
        "piece-fee:" + piece.pieceId(),
        lines);
  }

  /** Something sold to a member (a bag of clay, a tool) goes on their balance, with tax. */
  @EventListener
  void onSupplySold(SupplySold sale) {
    LocalDate day = books.dateOf(sale.soldAt());
    String saleCategory = sale.taxCategory();
    String category = saleCategory == null ? "RETAIL" : saleCategory;
    BigDecimal tax = taxRates.taxOn(sale.amount(), category, day, books.currency());
    List<Line> lines = new ArrayList<>();
    lines.add(
        Line.debit(LedgerAccount.MEMBER_RECEIVABLE, sale.amount().add(tax))
            .member(sale.memberId()));
    lines.add(Line.credit(LedgerAccount.REVENUE_RETAIL, sale.amount()).taxCategory(category));
    if (tax.signum() != 0) {
      lines.add(Line.credit(LedgerAccount.SALES_TAX_PAYABLE, tax).taxCategory(category));
    }
    if (sale.amount().signum() != 0) {
      journal.post(
          EntryKind.RETAIL_SALE,
          day,
          LedgerService.truncate(
              "%s × %s"
                  .formatted(
                      sale.quantity().stripTrailingZeros().toPlainString(), sale.itemName())),
          sale.movementId(),
          "supply-sale:" + sale.movementId(),
          lines);
    }
  }
}
