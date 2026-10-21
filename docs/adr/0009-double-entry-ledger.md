# 9. A double-entry ledger, enforced by the database

- **Status:** accepted
- **Date:** 2026-10-20

## Context

The research behind the plan found three money problems:

- Matching processor payouts to sales is the top bookkeeping complaint (15 of 55 posters).
- 21 of 84 studios take money through two or more tools.
- Prepaid money (gift cards on 53 of 84 sites, plus firing cards, class packs and deposits) is a
  liability that someone has to roll forward each month.

The owner also requires a QuickBooks Online integration. Intuit's free API tier meters reads,
pooling 500,000 a month across every connected company, but not most writes. QuickBooks users'
most common complaint about existing connectors is per-transaction posting.

## Decision

- **One journal per studio, double-entry.** Each money event is one entry: a piece's fee, a
  payment, stored value sold, used or expired, a payout, or a retail sale. Lines are signed
  (debit positive) against a fixed chart of accounts, with sub-ledgers per member, per
  stored-value balance and per provider. Every balance is a sum of lines; none is stored.
- **The database enforces balance.** A `DEFERRABLE INITIALLY DEFERRED` constraint trigger
  rejects, at commit, any entry whose lines don't sum to zero. A bug in application code can't
  write unbalanced books.
- **Idempotency keys.** Each entry carries a key unique per studio, such as
  `piece-fee:<piece>` or `payout:STRIPE:po_123`. Re-delivered events and re-imported payout files
  therefore post once.
- **Providers are data, not architecture.** Money taken through any processor lands in
  `PROCESSOR_CLEARING`, tagged with the provider. The processor's payout clears it as sales less
  refunds and fees, plus adjustments, and the `payout` table checks that arithmetic. What remains
  in clearing is what each processor still owes the studio. Charging cards later goes through a
  `PaymentProvider` interface, and nothing in the books assumes there is only one provider.
- **Summary-level accounting.** QuickBooks or Xero receive one journal per day, split by account
  and tax category, and never every transaction. The first delivery path is a ledger-neutral
  journal CSV that both systems import. Live QBO posting (`QuickBooksJournal` builds the
  `JournalEntry`) is write-only and keyed by `DocNumber`, so a retry is refused, not duplicated.
- **Tax by category and date.** Each fee and sale carries a tax category, and rates are stored
  with an effective date. Washington, for example, began taxing pottery classes on 2025-10-01.

## Consequences

- Statements, reconciliation, the stored-value roll-forward and the export are all queries over
  one table pair.
- Corrections are new entries, not edits, which is what an accountant expects.
- The live QuickBooks connection still needs Intuit app credentials, an OAuth connection per
  studio and an account mapping. Those are Wave 2, or Wave 1 if the pilot studio runs QBO.

## In an interview

"Money is where a bug costs trust, so I made the database the last line of defence. A deferred
constraint trigger refuses any journal entry that doesn't balance, and idempotency keys make
every event and import safe to retry. Payment providers are just tags on a clearing account, so
a studio using Square at the desk and Stripe online reconciles both in one place. For
QuickBooks, I post one summary journal a day. Intuit meters reads across all my customers, and
per-transaction sync is what its users complain about."
