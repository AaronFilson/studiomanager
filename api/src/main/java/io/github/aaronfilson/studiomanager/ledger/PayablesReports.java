package io.github.aaronfilson.studiomanager.ledger;

import static io.github.aaronfilson.studiomanager.ledger.PayablesChecks.requireStaff;

import io.github.aaronfilson.studiomanager.ledger.PayablesModel.NecLine;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.NecReport;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.PayeeStatement;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.PaymentMethod;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.StatementLine;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A payee's statement and the year's 1099-NEC list: sums over the journal, in SQL. */
@Service
class PayablesReports {

  private final PayablesService payees;
  private final PayablesQueries queries;
  private final LedgerService ledger;

  PayablesReports(PayablesService payees, PayablesQueries queries, LedgerService ledger) {
    this.payees = payees;
    this.queries = queries;
    this.ledger = ledger;
  }

  /**
   * What a payee was owed at the start, each sale, share and payment over the period, and the end.
   */
  @Transactional(readOnly = true)
  public PayeeStatement statement(UUID payeeId, LocalDate from, LocalDate to) {
    requireStaff();
    if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 366) {
      throw new InvalidInputException("Ask for a range of up to a year.");
    }
    PayeeEntity payee = payees.find(payeeId);
    BigDecimal opening = queries.owedBefore(payeeId, from);
    List<StatementLine> lines = queries.statementLines(payeeId, from, to);
    BigDecimal closing = lines.stream().map(StatementLine::amount).reduce(opening, BigDecimal::add);
    return new PayeeStatement(payeeId, payee.name(), from, to, opening, lines, closing);
  }

  /**
   * Payments in a calendar year to each payee the studio files a 1099-NEC for. Only payments by
   * cheque, bank transfer, cash or studio credit count: the IRS has card and payment-network
   * payments reported on the network's 1099-K instead. A payee is flagged once the reportable total
   * reaches the year's threshold.
   */
  @Transactional(readOnly = true)
  public NecReport necReport(int year) {
    requireStaff();
    if (year < 2000 || year > ledger.today().getYear()) {
      throw new InvalidInputException("Ask for a year that has started.");
    }
    BigDecimal threshold = PayablesService.necThreshold(year);
    List<String> reportable =
        Arrays.stream(PaymentMethod.values())
            .filter(PaymentMethod::countsTowardsNec)
            .map(Enum::name)
            .toList();
    List<NecLine> lines =
        queries.necLines(
            LocalDate.of(year, 1, 1), LocalDate.of(year + 1, 1, 1), reportable, threshold);
    return new NecReport(year, threshold, lines);
  }
}
