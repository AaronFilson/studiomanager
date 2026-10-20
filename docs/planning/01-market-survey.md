# Market Survey: Pottery Studio Management Software

_Researched 2026-10-19. Sources are listed at the bottom. Reddit, the Ceramic Arts Daily forum and Facebook groups could not be read directly, so read Reddit by hand before locking scope._

## TL;DR

- **The niche is real but crowded at the small end.** About ten pottery-specific tools exist (Kiln Fire, MudBuddy, WheelOwl, Kilnfox, ClayTrack, Classly, PotteryStudio.app, Zenamu, CollectIt, and Claybench in development). All are paid or hide their pricing ($19–$579/mo). **None has a permanently free plan**, and price creep as a studio grows is the most common complaint.
- **Many studios still run general booking tools** (Mindbody, Momence, Pike13, Square, Acuity) plus spreadsheets and paper tags. None of those knows what a kiln is.
- **The top pain is piece tracking.** Studios lose work, and members can't find out where their piece is. Firing-fee calculation and billing come second, shelf space and pickup deadlines third.
- **Invoice OCR did not show up as a pain point.** No one is complaining about supplier invoices. It is still worth building as a supporting feature rather than the headline (see [the plan](02-wave1-plan.md#why-ocr-is-in-wave-1-anyway)).

## Pain points, ranked by how many independent sources raised them

| # | Pain point | Signal | Representative evidence |
|---|---|---|---|
| 1 | **Piece tracking, lost or unclaimed work, "is my piece ready?"** | ~8 sources | Studio policies routinely disclaim lost work; a customer "had to keep calling in order to get information"; staff "group the pieces together and send the emails to the owners" by hand after each kiln unload |
| 2 | **Firing fees: how to charge and how to measure** | ~7 | Most charge $0.03–0.08 per cubic inch, measured at greenware and rounded up; others charge by the pound, per shelf or load, or with a seasonal volume cap. "Too many people think the firing cost is simply electricity" (depreciation and labor get left out) |
| 3 | **Shelf and locker space, pickup deadlines, forfeiture** | ~6 | "We clear every shelf on the 1st"; items left more than 14 days are removed; shelves assigned quarterly as they free up |
| 4 | **Kiln scheduling, loading quality, firing calendars** | ~5 | Glaze transferring onto another member's piece; "if it does not fit, it waits"; greenware and glaze deadline calendars published as PDFs; paper "pottery slips" |
| 5 | **Membership rules, onboarding, etiquette** | ~5 | Mandatory orientation plus a signed agreement; "3 strike" rules; pause and cancel cutoffs that "catch people out" |
| 6 | **Class booking and waitlists** | medium (mostly from vendors) | Mindbody is expensive and confusing; Square is "janky" for a class series. Automatic waitlists are expected |
| 7 | **Shared glaze book and test logs** | medium | Active recipes kept separate from tests; contaminated buckets become a "mystery" glaze; photographed test tiles |
| 8 | **Consignment sales of member work** | low–medium | 30–50% commission, monthly statements, W-9 and 1099-NEC, netting dues against commissions |
| 9 | **Material inventory and reordering** | low | Reorder points; cost per kg "from the most recent supplier invoice" |
| 10 | **Supplier invoice bookkeeping, OCR** | not found | Only "keep receipts 5–7 years" and a niche bookkeeping firm for paint-your-own studios |

### Owner needs vs member needs

| Owners / managers | Members |
|---|---|
| Firing-fee calculation and billing (rules, discounts, minimums, card on file) | Where is my piece, and when is it ready? |
| Piece intake through pickup: labels, stage tracking, automatic "ready" notices, forfeiture | Fair, clear firing charges |
| Kiln calendar, load log, cutoffs | Not losing work to bad labels, contamination or shelf clearing |
| Membership tiers, recurring billing, pause and cancel, signed rules agreement | Shelf space plus reminders before a deadline |
| Classes and waitlists | Booking open studio, wheels and classes |
| Shelf and locker assignment with expiry | A personal glaze and piece journal |
| Consignment ledger and payouts | |

## Competitive landscape

| Product | Target | Pottery-specific strengths | Pricing | Gaps |
|---|---|---|---|---|
| Kiln Fire | US membership studios | Members photograph and measure their piece and pay the firing fee themselves; memberships, classes, POS, waivers, Kisi door locks | Private quote, ~$29–579/mo, plus a cut of memberships | No kiln-load or shelf management, no glaze or clay inventory |
| MudBuddy | Community and teaching studios | Firing fees by weight; per-item inventory; return per kiln load | From $29/mo, usage-based | New, no reviews; shelves and glaze lab unclear |
| WheelOwl | EU studios | Strongest fee rules engine (per kg with temperature tiers, allowances, minimums, member vs guest), auditable billing | €19–79/mo | German/English only; no shelves or inventory |
| Kilnfox | Studios and paint-your-own | QR stage tracking with an email on each status change; pickup booking | $29–130/mo | Weak fee rules; no load planning |
| ClayTrack | Studios | Piece tracking only | From $19/mo | Narrow |
| Classly | Pottery studios | Memberships, wheel booking, classes, gift cards, QuickBooks and Shopify integrations | 4% transaction fee | No firing, kilns or work tracking |
| Zenamu | Small EU studios | Booking; kiln as a bookable resource | Free tier plus paid plans | No per-piece fees, glazes or shelves |
| Mindbody / Momence / Pike13 / Square / Acuity | General | Mature booking and payments | $0–286/mo plus processing markup | Nothing pottery-specific |
| Skutt KilnLink / Bartlett KilnAid | Kiln owners | Remote kiln monitoring | $20–100 per kiln per year | Only their own controllers; no studio features |
| Glazy | Potters | Open-source recipe database (more than 15k recipes) | Free | Recipes only |
| Personal apps (KilnNote, Pottery Tracker, ClayLab…) | Individual potters | Piece, glaze and firing journals | Free or freemium | Single user |

A CAD forum moderator responded to a 2026 "I'm building studio software" post by pointing at existing apps. **Expect skepticism.** The way to stand out is being free and better at the hard pottery-specific parts, not offering another booking calendar.

## Whitespace (where to win)

1. **A free core**, with Square payments as an optional premium feature. No competitor has a permanently free plan.
2. **Kiln-load planning:** shelves, volume fill, cone and atmosphere per load, a queue. No one does this visibly.
3. **Shelf and storage management:** assignment, waitlist, expiry reminders, forfeiture workflow.
4. **A configurable firing-fee engine** for every model in use (volume, weight, shelf, allowance, minimums, tiered). Only WheelOwl, in the EU, does this well.
5. **A full piece lifecycle:** piece → glaze → load → firing → result → pickup, with automatic "ready" notifications.
6. **Studio materials and cost intelligence:** clay and glaze stock, reorder points, *true cost per firing*. This is where invoice OCR fits.
7. **Kiln-controller integrations** (KilnLink, KilnAid) to pull firing logs into the load record. Later.
8. **Community-studio extras:** monitor shifts, work-exchange hours, "kiln-trained" certification gating.
9. **Importers** from Mindbody, Momence and Square, plus spreadsheet CSV.

## Sources

- https://theclayhole.com/pages/everything-that-goes-wrong-running-a-pottery-studio
- https://www.kleistonestudio.com/blogs/news/a-day-at-kleistone-studio-what-it-means-to-run-a-community-ceramic-studio
- https://community.ceramicartsdaily.org/topic/7910-firing-service-fees/
- https://community.ceramicartsdaily.org/topic/25391-dealing-with-a-studio-that-has-bad-practices/
- https://community.ceramicartsdaily.org/topic/13011-etiquette-for-community-studio-suggestions-plz/
- https://community.ceramicartsdaily.org/topic/16102-community-studio-classes-vs-membership-structuresbusiness-models/
- https://community.ceramicartsdaily.org/topic/44093-feedback-wanted-im-building-studio-management-software-built-around-ceramic-process/
- https://ceramicartsnetwork.org/daily/article/a-super-handy-way-to-calculate-firing-fees
- https://ceramicartsnetwork.org/pottery-making-illustrated/pottery-making-illustrated-article/In-the-Studio-Group-Studio-Glaze-Management
- https://www.tc.columbia.edu/media/microsites/art-studios/ceramics/TC-Ceramics-Studio-Policies-2025-26.pdf
- https://www.berkeleypottersstudio.com/s/2023FINAL_TPS_MemberHandbook.pdf
- https://thewheelhousenh.notion.site/Member-Consignment-Sales-1d11653845c3805eb89fd0d3bac37393
- https://www.kilnfire.com/ · https://wheelowl.com/en · https://alternativeto.net/software/kilnfox/about/ · https://claytrack.com/blog/best-pottery-studio-software/ · https://zenamu.com/for-studio/ceramics/ · https://www.capterra.com.au/software/1107549/MudBuddy
- https://www.mindbodyonline.com/business/education/blog/new-mindbody-pricing-united-states · https://findstack.com/products/momence/pricing · https://www.capterra.com/p/40229/MINDBODY/reviews/
- https://skutt.com/kilnlink-page/kilnlink-faq/ · https://help.kilncontrol.com/knowledge/kilnaid-kilnlink-comparison
- https://derekau.net/blog/glazy-open-source-ceramics-recipes-library/
