# Wave 2: work queue

_Queued 2026-10-21, after [07](07-next-wave-queue.md) was merged into `main`. Wave 2's first
feature, maintenance ([06](06-wave2-maintenance-plan.md)), is done. This orders the rest of the
Wave 2 list in [02](02-wave1-plan.md#wave-2-next) and [04](04-pilot-readiness-plan.md#after-the-pilot-wave-2-candidates-to-be-ranked-by-what-the-pilot-shows)._

04 said that Wave 2 should wait for a pilot. There is still no pilot studio, and the owner has
asked to go ahead. So this queue does first the items that the research supports well and that
can be built and tested without anyone's credentials. Items that need a provider account or a
pilot's choice wait at the bottom.

## How it's ordered

| # | Work | Branch | Why this order |
|---|---|---|---|
| 1 | **Payables:** consignment, instructor shares, and a 1099-NEC report | `wave2-payables` | The research calls payables the biggest gap in the books, bigger than sales sync. Past Lives prints a PDF each month and re-keys it into QuickBooks. The Clay Studio pays consignors every month through bill.com. No vendor or credentials are needed. |
| 2 | **CSV importer with column mapping:** members first, then balances and consigned stock | `wave2-import` | [03](03-tools-in-use.md) puts a generic CSV importer first: it covers exports from Google Sheets, Forms and Airtable. A pilot has to move its existing list in on day one. |
| 3 | **Outbound webhooks:** piece ready, fee charged, member joined | `wave2-webhooks` | The integration owners ask for is "you cannot connect Zapier". The outbox makes it cheap, and the SSRF filter is already planned. |
| 4 | **Kiln firing-log import:** Genesis CSV first | later | Small, and attaches to the firing log. Wave 3 in 02, but 03 says it could move into Wave 2. It needs sample files. |
| 5 | **Glaze book:** a member imports their own Glazy YAML, and adds test-tile photos | later | Reuses photo uploads. Before seeding any data from Glazy, email them about licensing. |
| 6 | **Supplies inbox with invoice OCR** | later | The AI-pipeline showcase. No owner asked for it. It can be built against a stub, but it first runs for real at the deploy. |

**Waiting on a pilot or the owner's accounts.** None of these can be tested against the real
service without credentials, and the provider depends on the pilot:
- charging cards through the pilot's provider, and membership billing with pause and stop;
- live QuickBooks posting (needs an Intuit developer app);
- payout import from a provider's API instead of typed in;
- reading class bookings from the storefront, and the waitlists that go with them.

## 1. Payables

**The jobs.** From the [money research](research_notes/Pottery%20studio%20manager%20role/money_and_bookkeeping.md)
(B2, B2b, E1d, F7):
- **Consignment.** Each artist's work is taken in with a retail price. Sales are attributed to the
  consignor. The commission is worked out after discounts: The Clay Studio takes discounts out of
  its own half, and others split them. A statement each month, then a payout.
- **Instructor and guild shares.** Past Lives splits each class 70/10/20 between the instructor,
  the guild and the makerspace, about $2,600 a month. It needs who is owed what, whether it has
  been paid, and each payee's W-9 and 1099 status.
- **1099-NEC.** For payments made in 2026 the threshold is $2,000 (it was $600). Payments by card
  or through a payment network (PayPal, Stripe) go on the processor's 1099-K, not on a
  1099-NEC. Consignment proceeds count as payments for merchandise, which the IRS exempts, but
  some studios file for them anyway. So whether to file is a setting on each payee.

**The design.** It goes in the ledger module, on the same journal (ADR 9):
- A **payee** can be anyone the studio pays: an artist, an instructor or a guild. A payee may be
  linked to a member, and then can be paid in studio credit. We record when the W-9 arrived, but
  never the tax ID. The bookkeeper types that into whatever files the form, as Past Lives does.
- Three new accounts:
  - `PAYEES_PAYABLE`: a liability, with a sub-ledger per payee;
  - `REVENUE_COMMISSIONS`: the studio's share of a consignment sale, which is all the studio
    earns from it;
  - `CONTRACT_SERVICES`: an expense, for instructors' shares.
- **A consignment sale** is one entry:
  - debit clearing for the price plus tax;
  - credit the consignor's share to their payable;
  - credit the rest to commissions;
  - credit the tax to sales tax payable.

  A refund reverses it, and the item goes back into stock.
- **A share or fee earned** debits contract services and credits the payee. An adjustment can
  claw an amount back.
- **A payment** debits the payable and credits the money's source:
  - the bank, for a cheque or a bank transfer;
  - the till, for cash;
  - PayPal or Stripe's clearing balance, for those;
  - the payee's own member balance, for studio credit.

  The method decides whether the payment counts towards the 1099-NEC.
- **Reports:**
  - a payee statement for any period (opening, each line, closing);
  - who is owed what, with any W-9 still missing;
  - the 1099-NEC list for a year, viewable and as CSV.

**Not in this slice:**
- a member's own view of their consignment sales;
- bill.com or QuickBooks bill export (the daily journal already carries the payable);
- recording class revenue itself, which belongs to reading class bookings;
- consignment terms that expire after a minimum period.

## Status

Updated as each item lands.

- **1. Payables:** first version done 2026-10-21 (V21), on `wave2-payables`.
  - **Payees.** Each has optional consignment terms: the artist's share, and whether discounts
    come from both shares or only the studio's commission. Each also has a 1099 setting and the
    date the W-9 arrived.
  - **Consignment.** Work is taken in with a retail price, then sold, refunded or returned to the
    artist. A sale posts the artist's share to `PAYEES_PAYABLE`, the rest to
    `REVENUE_COMMISSIONS`, and retail sales tax on top. A refund reverses all three. If it went
    back through a processor, it meets that payout's refunds in `REFUNDS`, so it isn't counted
    twice.
  - **Earnings and payments.** A class share is worked out from what the class took. Payments
    can't exceed what's owed. Studio credit pays a member-payee against their own balance.
  - **Reports.** A statement for any period, and the 1099-NEC list. It counts cheque, bank, cash
    and studio-credit payments, but not PayPal or Stripe (those go on a 1099-K). The threshold is
    $2,000 for 2026 and $600 before. The list shows on the web and downloads as CSV, marking any
    W-9 still missing.
  - **Web.** An "Artists and instructors" page. The 1099 section shows for US studios only.
  - **Not yet:** see "Not in this slice" above. Also, the pilot should show whether staff want
    to pay several payees at once.
