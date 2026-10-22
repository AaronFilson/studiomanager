# Wave 1 Plan: Features, Architecture, Resume Story

_Drafted 2026-10-19 from [the market survey](01-market-survey.md). **Revised 2026-10-20** from
[the studio manager research](reports/Pottery%20studio%20manager%20role.md), which covers 79 job
postings, quotes from 34 studios, an audit of 84 studio websites and 24 nonprofit filings. The
research changed the plan in four ways; see "What changed and why" at the end._

## Product positioning

**"A free studio manager that actually knows what a kiln is."**

The guiding principle is to **help a studio's manager, or stand in for one.** Most for-profit
studios are run by their owner (only 31 of 84 studio websites name anyone else running
operations). Co-ops have no manager at all: volunteer chairs split the work. Nonprofits put a
small paid team under a director. So the software must hold the studio's state well enough that
*whoever is covering* can run the day.

The research splits the job in two:
- **Software can run on its own:** ready, pickup and disposal notices; stale-piece alerts; closure
  and cover notices; low-stock alerts; maintenance due by firing count; roster gaps and hour
  credits; balance reminders; payout import, reconciliation and summary posting; certification
  expiry.
- **A person still does it, with software proposing or reminding:** loading, firing and unloading
  kilns; placing supply orders; repairs; inductions; approving outside clay and glaze; refunds and
  hard conversations.

The core is free for studios. It leads with what generic booking tools don't cover. Studios
already book classes elsewhere (55 of 84), so the product **reads class bookings rather than
replacing them.**

### Decisions
- **2026-10-19 · Infrastructure as code:** Terraform (azurerm, remote state in Azure Storage, plan
  on PR, apply on merge).
- **2026-10-19 · Units:** US customary and metric from day one (see below).
- **2026-10-20 · Payments: several providers.** Each processor payout is a first-class record
  from day one. Payouts are imported read-only from every tool a studio already uses (21 of 84
  studios take money through two or more). Charging cards goes through a `PaymentProvider`
  interface. The first provider is chosen with the pilot studio: Stripe is the processor at 6 of
  10 comparable vendors, while Square is common for gift cards at studios.
- **2026-10-20 · QuickBooks Online is in scope.** It is posted as daily or per-payout *summaries*:
  write-mostly, webhook-driven, never polling. Intuit's free tier pools 500,000 read credits a
  month across every connected studio, and per-transaction sync is what QuickBooks users complain
  about. A ledger-neutral summary export comes first. It also serves Xero studios (Australia and
  New Zealand) until a native Xero connection pays for itself.
- **2026-10-20 · Studios have a country and a currency.** Tax rules, the ledger and the choice of
  accounting system all depend on them.

## Wave 1

Each feature answers a researched job *and* carries a distinct technical story.

| # | Feature | The job it takes off the manager | Resume-worthy engineering |
|---|---|---|---|
| 1 | **Tenancy, roles and duties.** Signup, invites, a role (owner, staff, member). **Duties** layered on top: technician, monitor, work exchange, volunteer officer. Expiring **certifications** per resource ("may load kiln 2", "wheel room orientation, renews yearly") | Co-ops, techs and monitors run studios too; 17 of 84 sites gate use on an orientation | Multi-tenant Postgres with **row-level security**, Entra External ID, method-level authorization, permissions with validity periods |
| 2 | **Piece tracker.** Intake from the phone camera (photo, dimensions or weight), maker's mark, stages (greenware → bisque → glaze → fired → ready → picked up / unclaimed), **hold period with an unclaimed state**, a member status page | "Do not call us to find out if your work has been fired": only 6 of 84 studios publish status | React Native camera, direct-to-Blob uploads with **SAS tokens**, an explicit state machine |
| 3 | **Firing-fee rules engine.** Per in³ / kg / litre / shelf, flat, minimums, rounding, member-tier discounts, allowances, prepaid firing cards. Each fee carries a **tax category with effective dates** | How to charge; tax rules move (Washington began taxing pottery classes on 2025-10-01) | Rules versioned as JSONB, **auditable price snapshots**, money as `BigDecimal` with integer minor units, property-based tests (jqwik) |
| 4 | **Kilns, queue and firing log.** Kilns per site; a **queue policy per kiln** (oldest first, N pieces per member, class work first); building a load; the firing log; **one ready notice per batch**. A **firing counter** drives maintenance thresholds, and a kiln can be marked down | The kiln queue and firing calendar are the most-cited unserved scheduling job; maintenance is in 38 of 40 technician postings, but only 2 keep a log | Domain events, the **transactional outbox** (Spring Modulith), push and email notifications, counter-driven rules |
| 5 | **Studio calendar, rosters and "studio today".** One calendar per resource (kiln, wheel room, whole studio), with firings, rentals, maintenance, class blocks (read from the storefront) and closures. Fixed weekly roster slots per session, gap alerts, an hours-to-credit ledger. A handover view: what's in each kiln, the queue, low stock, notices sent, who is covering | 32 of 39 manager postings schedule something; 0 of 16 labour programmes use a tool; the manager is a single point of failure | Interval and recurrence modelling (`tstzrange`, exclusion constraints), iCalendar feeds, broadcast notices |
| 6 | **Money ledger.** Balances owed, plus **stored value**: gift cards, firing cards, class packs, deposits, with opening, sold, used, expired and closing figures. **Payout import and reconciliation** (each deposit = sales − refunds − fees ± adjustments). A daily summary export, posted to **QuickBooks Online** | Matching payouts to sales is the top bookkeeping complaint (15 of 55); gift cards are on 53 of 84 sites | Double-entry journal in Postgres, idempotent webhook ingestion, OAuth connections, a read budget per integration, reconciliation reports |
| 7 | **Clay and supplies.** Items with a pack size, supplier and lead time, **par levels**, low-stock alerts, **purchase requests with approval**, clay sales recorded against the member, "out of stock, use this instead" notices | Ordering is in 30 of 39 manager and 30 of 40 technician postings, with no tool named | Event-driven stock movements, approval workflow |

### Units: US customary and metric
- **Store canonical SI** (mm, g, cm³) as integers or `BigDecimal`. Each studio has a display
  preference, and each member can override it.
- **Fee rules are authored in a unit**, for example "$0.05 per in³, round each dimension up to the
  next whole inch". Rounding happens *in the rule's unit*, then the price is calculated. Rounding
  in SI would change the price.
- **Measurements keep their original value and unit** (`12.5 in`) alongside the SI value, so they
  can be audited and never drift.
- Kiln temperatures are stored in °C and shown in °F or °C. Cones have no unit.

### Wave 2 (next)
- Native QuickBooks posting, if the pilot doesn't already need it in Wave 1
- Charging through the first payment provider: card on file, automatic firing fees, membership
  billing with pause and stop
- Consignment and instructor payables, with a 1099-NEC export ($2,000 threshold from 2026).
  _First version done 2026-10-21 ([08](08-wave2-queue.md), item 1)._
- Waitlists that offer freed places
- Reading class bookings from storefronts (Shopify, WooCommerce, Acuity)
- Glaze book (users' own Glazy exports, test-tile photos)
- CSV importers
- **Supplies inbox with invoice OCR**, which feeds item costs. Moved from Wave 1: no owner asked
  for it, and 0 of 34 owner sources mention supplier invoices. It is still the planned
  AI-pipeline showcase.

### Wave 3 (later)
- Native Xero
- Kiln-controller log import (Bartlett Genesis CSV, TAP)
- Kiln-load fill visualization
- Semantic search over pieces and glazes (pgvector)
- AR dimension measurement

### Deferred on the evidence
- General SKU inventory: PYOP stock already lives in POS systems, and nonprofit retail is small.
- A fixed-asset register: no demand, and no tax rule requires one.
- Replacing class booking.
- Charging through more than one processor at launch.
- Writing to Shopify: App Store rules 1.1.2 and 1.1.8 forbid it.

## Architecture

```
            ┌──────────────┐     ┌───────────────────┐
 React web ─┤              │     │ Entra External ID │
            │ Container    │     └─────────┬─────────┘
 Expo app ──┤ Apps ingress │               │ JWT
            │              ├──► Spring Boot 4.1 modular monolith (Java 25, virtual threads)
            └──────────────┘     │  modules: studio · access · piece · firing · kiln ·
                                 │           schedule · ledger · supplies · notify
                                 │  Spring Modulith events + outbox
                ┌────────────────┼──────────────────┬───────────────────────┐
                ▼                ▼                  ▼                       ▼
        PostgreSQL 18      Blob Storage      Payment providers,       App Insights (OTel)
        (Flexible, RLS)    (SAS upload)      QBO (webhooks in,
                                             summaries out)
```

**Key decisions, with the interview answer for each:**
- **A modular monolith, not microservices.** Spring Modulith enforces module boundaries in tests.
  The OCR worker (Wave 2) is the one planned separate deployable. "I split where the scaling and
  failure characteristics differ, not by default."
- **Shared schema, a `studio_id` on every row, and Postgres RLS** as defence in depth beneath the
  application's own checks.
- **OpenAPI-first.** A TypeScript client is generated once and shared by web and mobile.
- **Integrations are summary-level and write-mostly.** "QuickBooks meters reads across all my
  customers, so I designed the sync around webhooks and daily journal entries, and kept a read
  budget per studio."
- **Java 25 LTS, Spring Boot 4.1.1, PostgreSQL 18, React 19 + Vite 8 + TanStack, Expo SDK 58.**
- **Azure Container Apps** with scale to zero, and a JVM AOT cache for cold starts (ADR 8).

## Repo layout (monorepo)

```
/api          Spring Boot (Gradle Kotlin DSL), Flyway migrations
/web          React + Vite
/mobile       Expo (from M1)
/packages/api-client   generated TS client (shared)
/infra        Terraform
/docs         planning, ADRs, runbooks
```

## Engineering practices (resume checklist)

- **Testing:** JUnit + Testcontainers, Modulith module tests, jqwik for the fee engine and the
  ledger (balances always sum to zero), Playwright for web, Maestro for mobile.
- **CI/CD:** GitHub Actions with **OIDC federated credentials** (no stored secrets), Flyway
  migrations in a separate job.
- **Security:** managed identity everywhere, RLS, provider OAuth tokens encrypted in Key Vault,
  webhook signature verification, and the outbound SSRF filter.
- **Observability:** OpenTelemetry → App Insights, structured JSON logs, and integration read and
  write counts per studio.
- **ADRs** in `/docs/adr`.

## Free-tier cost plan

| Service | Approach | Approx. cost |
|---|---|---|
| Container Apps | Scale to zero, within the monthly free grant | ~$0 at low traffic |
| PostgreSQL Flexible | B1ms is free for 12 months on a free account | ~$15–25/mo after that _(verify)_ |
| QuickBooks API | Intuit Builder tier: writes free, 500k read credits/month pooled | $0 if reads stay budgeted |
| Xero API | Free up to 5 connections, then A$35/month | Deferred to Wave 3 |
| Blob, Communication Services email | Usage-based | Low single digits |

## Milestones

1. **M0, skeleton:** done 2026-10-19 (monorepo, CI, IaC, auth, "hello studio"). The research
   review added studio country and currency.
2. **M1, piece tracker plus fee engine:** pieces, the hold period, a member status page, and fee
   rules with tax categories. **Find a pilot studio now.**
3. **M2, kilns, queue and notifications:** kiln queue policies, loads, the firing log and counter,
   one ready notice per batch. Closes the loop; demo-able on mobile.
4. **M3, studio calendar, duties and rosters:** the resource calendar, certifications, the roster
   and hours ledger, the "studio today" view.
5. **M4, money ledger:** stored value, payout import and reconciliation, the summary export, and
   QBO posting if the pilot runs QBO.
6. **M5, clay and supplies:** par levels, purchase requests, clay sales.
7. **M6:** public beta, then Wave 2.

If Wave 1 must shrink, cut from the bottom: M5, then M4's native QBO posting (keep the export).

### Status (2026-10-20)

M1–M5 have first versions on the API and the web, with unit tests, integration tests against
Postgres 18, and web tests. Still to do within Wave 1:
- **M1:** photo upload (direct to Blob storage with SAS tokens) and the Expo mobile app.
- **M2:** email and push delivery of notices through the outbox (the in-app inbox is done).
- **M3:** linking kiln firings onto the calendar automatically, and swapping shifts. _Both done
  2026-10-21 ([07](07-next-wave-queue.md), item 11)._
- **M4:** the live QuickBooks connection (OAuth, account mapping, daily posting). The CSV export
  and the `JournalEntry` mapping are done. Payout import from provider APIs.
- **M5:** "out of stock, use this instead" sent automatically. The substitute text shows on the
  supplies list. _Done 2026-10-21 ([07](07-next-wave-queue.md), item 11)._

## Open questions

1. **A pilot studio.** Ask the ten interview questions in the research report, and measure admin
   hours, "is it ready?" contacts, days to the ready notice, stock-outs, and minutes spent
   reconciling payouts.
2. **The first payment provider:** whatever the pilot uses.
3. **Product name.**
4. **Premium pricing.** Charge a flat fee, or pass through processing fees? This can wait for
   Wave 2.

## What changed and why (2026-10-20)

| Before | After | Evidence |
|---|---|---|
| Five features, ending with invoice OCR | Seven features, adding the calendar with rosters, the money ledger, and clay and supplies; OCR moved to Wave 2 | Scheduling is in 32 of 39 manager postings; payout matching is the top bookkeeping complaint; ordering is in 30 of 39 manager postings; nobody asked for OCR |
| Square as the only, premium, payment provider | Payouts imported from every provider; charging behind an interface; QBO summaries | 21 of 84 studios use two or more money tools; Intuit pools read credits |
| Monitor shifts, work exchange and certification gating in Wave 3 | In Wave 1 (M3) | Unserved: 0 of 16 labour programmes use a tool, and 17 of 84 sites gate on orientation |
| Consignment ledger in Wave 3 | Wave 2 | Commission splits of 60/40 and 50/50, plus instructor revenue shares |
| Roles: owner, staff, member | Roles plus duties and expiring certifications | Co-ops are run by volunteer chairs; the work is split between technicians and monitors |
